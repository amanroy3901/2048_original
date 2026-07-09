package com.avfusionapps.game_2048.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avfusionapps.game_2048.data.GameSettingsRepository
import com.avfusionapps.game_2048.data.repository.RiseRepository
import com.avfusionapps.game_2048.game.RiseEngine
import com.avfusionapps.game_2048.model.DropStepKind
import com.avfusionapps.game_2048.model.RiseConfig
import com.avfusionapps.game_2048.model.RiseState
import com.avfusionapps.game_2048.model.RiseUndoSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Feedback events the Rise screen turns into sound/haptics/shakes. */
enum class RiseGameEvent { SHOOT, MERGE, COMBO, UNLOCK, PRESSURE, RERACK, GAME_OVER }

class RiseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RiseRepository(application.applicationContext)
    private val settingsRepository = GameSettingsRepository(application)
    private val engine = RiseEngine()

    private val _gameState = MutableStateFlow(RiseState())
    val gameState: StateFlow<RiseState> = _gameState.asStateFlow()

    val bestScore = repository.bestScore
    val bestTile = repository.bestTile

    val vibrationEnabled: StateFlow<Boolean> = settingsRepository.vibrationEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val soundEnabled: StateFlow<Boolean> = settingsRepository.soundEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _events = MutableSharedFlow<RiseGameEvent>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<RiseGameEvent> = _events.asSharedFlow()

    private var undoSnapshot: RiseUndoSnapshot? = null
    private var resolveJob: Job? = null

    init {
        viewModelScope.launch {
            val persistedBestTile = repository.bestTile.first()
            startNewGame(persistedBestTile)
        }
    }

    fun startNewGame(persistedBestTile: Int? = null) {
        resolveJob?.cancel()
        undoSnapshot = null
        val best = persistedBestTile ?: _gameState.value.bestTileEver
        val cadence = engine.pressureEvery(best)
        _gameState.value = RiseState(
            rack = List(RiseConfig.LANES) { engine.freshTile(engine.spawnValue(best)) },
            bestTileEver = best,
            unlockTarget = engine.unlockTarget(best),
            shotsUntilPressure = cadence,
            pressureEvery = cadence
        )
        viewModelScope.launch { repository.incrementGamesPlayed() }
    }

    fun togglePause() {
        _gameState.value = _gameState.value.copy(isPaused = !_gameState.value.isPaused)
    }

    fun setPaused(paused: Boolean) {
        _gameState.value = _gameState.value.copy(isPaused = paused)
    }

    /** Fire the rack tile in [lane] straight up its own lane. */
    fun shoot(lane: Int) {
        val state = _gameState.value
        if (state.isGameOver || state.isPaused || state.isResolving) return
        if (lane !in 0 until RiseConfig.LANES) return
        val tile = state.rack[lane] ?: return

        val result = engine.shoot(state.columns, lane, tile, state.bestTileEver)
        if (result.overflow) {
            // Lane physically full and no match — the stack is far past the
            // danger line anyway; this is a fail-safe.
            _gameState.value = state.copy(isGameOver = true)
            onGameOver()
            return
        }

        undoSnapshot = RiseUndoSnapshot(
            columns = state.columns,
            rack = state.rack,
            score = state.score,
            bestTileEver = state.bestTileEver,
            shotsUntilPressure = state.shotsUntilPressure,
            canReRack = state.canReRack,
            graceActive = state.graceActive
        )

        resolveJob = viewModelScope.launch {
            _events.tryEmit(RiseGameEvent.SHOOT)
            _gameState.value = state.copy(
                isResolving = true,
                canUndo = false,
                comboCount = 0,
                rack = state.rack.toMutableList().also { it[lane] = null }
            )
            scheduleRefill(lane)

            var runningBest = state.bestTileEver
            for (step in result.steps) {
                step.unlockedValue?.let { runningBest = maxOf(runningBest, it) }
                val maxOnBoard = step.columns.maxOf { c -> c.maxOfOrNull { it.value } ?: 0 }
                runningBest = maxOf(runningBest, maxOnBoard)

                _gameState.value = _gameState.value.copy(
                    columns = step.columns,
                    score = _gameState.value.score + step.gained,
                    lastStep = step,
                    comboCount = step.chainIndex,
                    bestTileEver = runningBest,
                    unlockTarget = engine.unlockTarget(runningBest),
                    justUnlockedValue = step.unlockedValue ?: _gameState.value.justUnlockedValue
                )

                when (step.kind) {
                    DropStepKind.PLACE -> delay(DropAnim.SHOT_TRAVEL)
                    DropStepKind.MERGE -> {
                        _events.tryEmit(if (step.chainIndex > 1) RiseGameEvent.COMBO else RiseGameEvent.MERGE)
                        if (step.unlockedValue != null) _events.tryEmit(RiseGameEvent.UNLOCK)
                        delay(DropAnim.MERGE_STEP)
                    }
                    DropStepKind.PURGE -> Unit // never emitted in Rise (purge disabled)
                }
            }

            var settled = _gameState.value.copy(
                isResolving = false,
                canUndo = true,
                moveCount = _gameState.value.moveCount + 1,
                lastStep = null
            )

            // Any stack past the line after the player's settle = game over
            // (this is also how a failed grace-rescue ends).
            if (engine.isOverLine(settled.columns)) {
                _gameState.value = settled.copy(isGameOver = true, graceActive = false)
                repository.updateBests(settled.score, runningBest)
                onGameOver()
                return@launch
            }

            // Pressure countdown; a drop resets the cycle and the free re-rack.
            val shotsLeft = settled.shotsUntilPressure - 1
            settled = if (shotsLeft <= 0) {
                val pressured = engine.pressureRow(settled.columns, runningBest)
                val cadence = engine.pressureEvery(runningBest)
                _events.tryEmit(RiseGameEvent.PRESSURE)
                settled.copy(
                    columns = pressured,
                    shotsUntilPressure = cadence,
                    pressureEvery = cadence,
                    canReRack = true,
                    // Pressure pushed a stack over → one grace shot to fix it.
                    graceActive = engine.isOverLine(pressured)
                )
            } else {
                settled.copy(shotsUntilPressure = shotsLeft, graceActive = false)
            }
            _gameState.value = settled

            repository.updateBests(settled.score, runningBest)
        }
    }

    private fun scheduleRefill(lane: Int) {
        viewModelScope.launch {
            delay(RiseConfig.REFILL_DELAY)
            val st = _gameState.value
            if (!st.isGameOver && st.rack[lane] == null) {
                _gameState.value = st.copy(
                    rack = st.rack.toMutableList().also {
                        it[lane] = engine.freshTile(engine.spawnValue(st.bestTileEver))
                    }
                )
            }
        }
    }

    /** Reroll one rack tile — free, once per pressure cycle. */
    fun reRack(lane: Int) {
        val state = _gameState.value
        if (state.isGameOver || state.isPaused || state.isResolving || !state.canReRack) return
        val current = state.rack.getOrNull(lane) ?: return

        var newValue = engine.spawnValue(state.bestTileEver)
        var attempts = 0
        while (newValue == current.value && attempts < 8) {
            newValue = engine.spawnValue(state.bestTileEver)
            attempts++
        }

        _events.tryEmit(RiseGameEvent.RERACK)
        _gameState.value = state.copy(
            rack = state.rack.toMutableList().also { it[lane] = engine.freshTile(newValue) },
            canReRack = false
        )
    }

    /** Single free undo of the last shot (board, rack, counters — everything). */
    fun undo() {
        val snap = undoSnapshot ?: return
        val state = _gameState.value
        if (state.isResolving || state.isGameOver) return
        undoSnapshot = null
        _gameState.value = state.copy(
            columns = snap.columns,
            rack = snap.rack,
            score = snap.score,
            bestTileEver = snap.bestTileEver,
            unlockTarget = engine.unlockTarget(snap.bestTileEver),
            shotsUntilPressure = snap.shotsUntilPressure,
            canReRack = snap.canReRack,
            graceActive = snap.graceActive,
            canUndo = false,
            lastStep = null,
            comboCount = 0
        )
    }

    /** Called by the UI after the unlock banner finishes. */
    fun clearUnlockBanner() {
        if (_gameState.value.justUnlockedValue != null) {
            _gameState.value = _gameState.value.copy(justUnlockedValue = null)
        }
    }

    private fun onGameOver() {
        _events.tryEmit(RiseGameEvent.GAME_OVER)
        val state = _gameState.value
        viewModelScope.launch {
            repository.updateBests(state.score, state.bestTileEver)
        }
    }

    override fun onCleared() {
        super.onCleared()
        resolveJob?.cancel()
    }
}
