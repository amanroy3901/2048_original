package com.avfusionapps.game_2048.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avfusionapps.game_2048.data.GameSettingsRepository
import com.avfusionapps.game_2048.data.repository.DropMergeRepository
import com.avfusionapps.game_2048.game.DropMergeEngine
import com.avfusionapps.game_2048.model.DropMergeConfig
import com.avfusionapps.game_2048.model.DropMergeState
import com.avfusionapps.game_2048.model.DropStepKind
import com.avfusionapps.game_2048.model.DropUndoSnapshot
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

/** Feedback events the screen turns into sound/haptics. */
enum class DropGameEvent { SHOOT, MERGE, COMBO, PURGE, UNLOCK, GAME_OVER }

/** Central timing knobs for the mode's playback + UI animations (millis). */
object DropAnim {
    const val SHOT_TRAVEL = 160L
    const val MERGE_STEP = 210L
    const val PURGE_STEP = 320L
    const val TILE_MOVE = 180
    const val MERGE_POP = 190
    const val CONSUME_FLY = 160
}

class DropMergeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DropMergeRepository(application.applicationContext)
    private val settingsRepository = GameSettingsRepository(application)
    private val engine = DropMergeEngine()

    private val _gameState = MutableStateFlow(DropMergeState())
    val gameState: StateFlow<DropMergeState> = _gameState.asStateFlow()

    val bestScore = repository.bestScore
    val bestTile = repository.bestTile

    val vibrationEnabled: StateFlow<Boolean> = settingsRepository.vibrationEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val soundEnabled: StateFlow<Boolean> = settingsRepository.soundEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _events = MutableSharedFlow<DropGameEvent>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<DropGameEvent> = _events.asSharedFlow()

    private var undoSnapshot: DropUndoSnapshot? = null
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
        val current = engine.spawnValue(best)
        val next = engine.spawnValue(best)
        _gameState.value = DropMergeState(
            currentValue = current,
            nextValue = next,
            bestTileEver = best,
            unlockTarget = engine.unlockTarget(best)
        )
        viewModelScope.launch { repository.incrementGamesPlayed() }
    }

    fun togglePause() {
        _gameState.value = _gameState.value.copy(isPaused = !_gameState.value.isPaused)
    }

    fun setPaused(paused: Boolean) {
        _gameState.value = _gameState.value.copy(isPaused = paused)
    }

    /** Fire the current tile up [col]. Ignored while paused/over/animating. */
    fun shoot(col: Int) {
        val state = _gameState.value
        if (state.isGameOver || state.isPaused || state.isResolving) return
        if (col !in 0 until DropMergeConfig.COLUMNS) return

        // A full, non-matching column is a fatal shot — resolve it as game over.
        val tile = engine.freshTile(state.currentValue)
        val result = engine.resolveShot(state.columns, col, tile, state.bestTileEver)

        if (result.overflow) {
            _gameState.value = state.copy(isGameOver = true, isResolving = false)
            onGameOver()
            return
        }

        undoSnapshot = DropUndoSnapshot(
            columns = state.columns,
            score = state.score,
            currentValue = state.currentValue,
            nextValue = state.nextValue,
            bestTileEver = state.bestTileEver
        )

        resolveJob = viewModelScope.launch {
            _events.tryEmit(DropGameEvent.SHOOT)
            _gameState.value = state.copy(isResolving = true, canUndo = false, comboCount = 0)

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
                        _events.tryEmit(if (step.chainIndex > 1) DropGameEvent.COMBO else DropGameEvent.MERGE)
                        if (step.unlockedValue != null) _events.tryEmit(DropGameEvent.UNLOCK)
                        delay(DropAnim.MERGE_STEP)
                    }
                    DropStepKind.PURGE -> {
                        _events.tryEmit(DropGameEvent.PURGE)
                        delay(DropAnim.PURGE_STEP)
                    }
                }
            }

            // Reload the launcher and settle the turn.
            val newCurrent = _gameState.value.nextValue
            val newNext = engine.spawnValue(runningBest)
            val settled = _gameState.value.copy(
                isResolving = false,
                canUndo = true,
                currentValue = newCurrent,
                nextValue = newNext,
                moveCount = _gameState.value.moveCount + 1,
                lastStep = null
            )
            _gameState.value = settled

            repository.updateBests(settled.score, runningBest)

            if (engine.isBoardLocked(settled.columns, settled.currentValue)) {
                _gameState.value = settled.copy(isGameOver = true)
                onGameOver()
            }
        }
    }

    /** Single free undo of the last shot. */
    fun undo() {
        val snap = undoSnapshot ?: return
        val state = _gameState.value
        if (state.isResolving || state.isGameOver) return
        undoSnapshot = null
        _gameState.value = state.copy(
            columns = snap.columns,
            score = snap.score,
            currentValue = snap.currentValue,
            nextValue = snap.nextValue,
            bestTileEver = snap.bestTileEver,
            unlockTarget = engine.unlockTarget(snap.bestTileEver),
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
        _events.tryEmit(DropGameEvent.GAME_OVER)
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
