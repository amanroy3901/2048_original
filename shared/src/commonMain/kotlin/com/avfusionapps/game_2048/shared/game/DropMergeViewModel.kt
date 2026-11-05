package com.avfusionapps.game_2048.shared.game

import com.avfusionapps.game_2048.shared.platform.KeyValueStore
import com.avfusionapps.game_2048.shared.platform.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Feedback events the screen turns into sound/haptics. */
enum class DropGameEvent { SHOOT, SKIP, MERGE, COMBO, PURGE, UNLOCK, GAME_OVER, POWER, REMOVE_HIT, LEVEL_UP }

/** Central timing knobs for the mode's playback + UI animations (millis). */
object DropAnim {
    const val SHOT_TRAVEL = 160L
    const val MERGE_STEP = 210L
    const val PURGE_STEP = 320L
    const val SETTLE_STEP = 480L
    const val REMOVE_HIT = 420L

    // The Elemental Sprites board plays the prototype's slower, showier timing.
    /** Flight (up to 220 ms) plus the 90 ms impact before the cascade starts. */
    const val SPRITE_SHOT = 310L
    /** The eaten sprites fly in (155 ms), then the survivor evolves and holds for 240 ms. */
    const val SPRITE_MERGE = 395L
    /** Purged sprites crumble (560 ms), then a 240 ms beat. */
    const val SPRITE_PURGE = 800L
    /** The hammer lands at 340 ms; the sprite crumbles for 420 ms before the lane closes. */
    const val SPRITE_REMOVE = 760L
    const val TILE_MOVE = 180
    const val MERGE_POP = 190
    const val CONSUME_FLY = 160
}

class DropMergeViewModel {

    val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Set by the Elemental Sprites board: play steps at the sprite prototype's pace. */
    var spritePacing = false

    private val repository = DropMergeRepository()
    private val engine = DropMergeEngine()

    private val _gameState = MutableStateFlow(DropMergeState())
    val gameState: StateFlow<DropMergeState> = _gameState.asStateFlow()

    val bestScore = repository.bestScore
    val bestTile = repository.bestTile

    
    val vibrationEnabled: StateFlow<Boolean> = GameSettingsRepository.vibrationEnabledFlow

    
    val soundEnabled: StateFlow<Boolean> = GameSettingsRepository.soundEnabledFlow

    // null = not yet loaded (avoids flashing the tutorial before we know).
    private val _hasSeenTutorial = MutableStateFlow<Boolean?>(KeyValueStore.getBool(TUTORIAL_KEY, false))
    val hasSeenTutorial: StateFlow<Boolean?> = _hasSeenTutorial.asStateFlow()

    fun setTutorialSeen() {
        KeyValueStore.putBool(TUTORIAL_KEY, true)
        _hasSeenTutorial.value = true
    }

    private val _events = MutableSharedFlow<DropGameEvent>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<DropGameEvent> = _events.asSharedFlow()

    /** Snapshots before each shot or power-up, newest last; Undo pops one. */
    private val history = ArrayDeque<DropUndoSnapshot>()
    private var resolveJob: Job? = null
    /** Set while a power-up is waiting on its charge, so taps can't stack up. */
    private var busy = false
    private var noticeKey = 0
    private var levelUpKey = 0

    init {
        viewModelScope.launch {
            val persistedBestTile = repository.bestTile.value
            val (level, xp) = repository.levelAndXp()
            _gameState.update { it.copy(level = level, xp = xp) }
            startNewGame(persistedBestTile)
        }
        viewModelScope.launch {
            GameSettingsRepository.coinsFlow.collect { coins -> _gameState.update { it.copy(coins = coins) } }
        }
    }

    fun startNewGame(persistedBestTile: Int? = null) {
        resolveJob?.cancel()
        busy = false
        history.clear()
        val old = _gameState.value
        val best = persistedBestTile ?: old.bestTileEver
        // Fresh board is empty (boardMax 0) → both bootstrap to the small floor.
        val current = engine.spawnValue(boardMax = 0, bestTileEver = best)
        val next = engine.spawnValue(boardMax = 0, bestTileEver = best)
        _gameState.value = DropMergeState(
            currentValue = current,
            nextValue = next,
            bestTileEver = best,
            unlockTarget = engine.unlockTarget(best),
            level = old.level,
            xp = old.xp,
            coins = old.coins
        )
        viewModelScope.launch { repository.incrementGamesPlayed() }
    }

    fun togglePause() {
        _gameState.value = _gameState.value.copy(isPaused = !_gameState.value.isPaused)
    }

    fun setPaused(paused: Boolean) {
        _gameState.value = _gameState.value.copy(isPaused = paused)
    }

    private fun idle(state: DropMergeState = _gameState.value): Boolean =
        !busy && !state.isResolving && !state.isPaused && state.removingTileId == null

    /** Fire the current tile up [col]. Ignored while paused/over/animating, or while Remove is armed. */
    fun shoot(col: Int) {
        val state = _gameState.value
        if (state.isGameOver || !idle(state) || state.removeArmed) return
        if (col !in 0 until DropMergeConfig.COLUMNS) return

        val tile = engine.freshTile(state.currentValue)
        val result = engine.resolveShot(state.columns, col, tile, state.bestTileEver)
        pushHistory(state)

        // A full, non-matching column is a fatal shot. The board is unchanged, so
        // Undo (from the game-over screen) simply takes the shot back.
        if (result.overflow) {
            _gameState.value = state.copy(isGameOver = true, isResolving = false, canUndo = true)
            onGameOver()
            return
        }

        resolveJob = viewModelScope.launch {
            _events.tryEmit(DropGameEvent.SHOOT)
            _gameState.value = state.copy(
                isResolving = true, canUndo = false, comboCount = 0, justUndid = false,
                actionCount = state.actionCount + 1
            )
            if (state.moveCount == 0) Progress.gameStarted(GameMode.SHOOTER)
            Progress.shot()
            val runningBest = playSteps(result.steps, state.bestTileEver)
            Progress.merged(GameMode.SHOOTER, result.steps.filter { it.kind == DropStepKind.MERGE }.map { it.gained })
            if (result.maxChain >= 2) Progress.chain(result.maxChain)
            Progress.score(_gameState.value.score)

            // Reload the launcher and settle the turn. A frozen launcher keeps its
            // tile; otherwise cap the next tile at the board's max so it can merge.
            val now = _gameState.value
            val frozen = now.freezeShots > 0
            val settled = now.copy(
                isResolving = false,
                canUndo = true,
                canSkip = true, // fresh swap each shot
                currentValue = if (frozen) now.currentValue else now.nextValue,
                nextValue = if (frozen) now.nextValue else engine.spawnValue(
                    boardMax = engine.boardMax(now.columns),
                    bestTileEver = runningBest
                ),
                freezeShots = (now.freezeShots - 1).coerceAtLeast(0),
                moveCount = now.moveCount + 1,
                lastStep = null
            )
            _gameState.value = settled
            if (frozen && settled.freezeShots == 0) notice("Freeze wore off")
            gainXp(result.totalGained)

            repository.updateBests(settled.score, runningBest)
            checkLocked()
        }
    }

    /**
     * NEXT: swap the tile in the launcher with the next one. Free, but once per
     * shot (it resets after each shot), and not while the launcher is frozen.
     */
    fun skipTile() {
        val state = _gameState.value
        if (state.isGameOver || !idle(state)) return
        if (state.freezeShots > 0) {
            notice("The launcher is frozen")
            return
        }
        if (!state.canSkip) {
            notice("One swap per shot")
            return
        }
        _events.tryEmit(DropGameEvent.SKIP)
        _gameState.value = state.copy(
            currentValue = state.nextValue,
            nextValue = state.currentValue,
            canSkip = false
        )
    }

    /** Undo: rewinds the last shot or power-up (also from the game-over screen). */
    fun undo() {
        val state = _gameState.value
        if (!idle(state)) return
        if (history.isEmpty()) {
            notice("Nothing to undo yet")
            return
        }
        busy = true
        viewModelScope.launch {
            try {
                if (!takeCharge(DropPower.UNDO)) return@launch
                val snap = history.removeLast()
                val now = _gameState.value
                _gameState.value = now.copy(
                    columns = snap.columns,
                    score = snap.score,
                    currentValue = snap.currentValue,
                    nextValue = snap.nextValue,
                    bestTileEver = snap.bestTileEver,
                    unlockTarget = engine.unlockTarget(snap.bestTileEver),
                    freezeShots = snap.freezeShots,
                    level = snap.level,
                    xp = snap.xp,
                    isGameOver = false,
                    canUndo = history.isNotEmpty(),
                    canSkip = true,
                    removeArmed = false,
                    lastStep = null,
                    comboCount = 0,
                    justUndid = true // tiles split back in place instead of shooting up
                )
                repository.saveLevel(snap.level, snap.xp)
                _events.tryEmit(DropGameEvent.POWER)
                notice("Rewound the last move")
            } finally {
                busy = false
            }
        }
    }

    /** Shuffle: deals the sprites back out across the lanes in a new order, then lets them merge. */
    fun shuffle() {
        val state = _gameState.value
        if (state.isGameOver || !idle(state)) return
        if (state.columns.sumOf { it.size } < 2) {
            notice("Not enough sprites to shuffle")
            return
        }
        busy = true
        resolveJob = viewModelScope.launch {
            try {
                if (!takeCharge(DropPower.SHUFFLE)) return@launch
                val now = _gameState.value
                pushHistory(now)
                val result = engine.shuffle(now.columns, now.bestTileEver)
                _events.tryEmit(DropGameEvent.POWER)
                _gameState.value = now.copy(
                    isResolving = true, powerResolving = true, removeArmed = false, comboCount = 0, justUndid = false,
                    shuffleKey = now.shuffleKey + 1, actionCount = now.actionCount + 1
                )
                finishPowerUp(result)
            } finally {
                busy = false
            }
        }
    }

    /**
     * Remove: arms the hammer (the next tile tap takes that tile off the board),
     * or puts it away. The charge is taken on the hit. From the game-over screen
     * it reopens the board so the player can clear a lane.
     */
    fun toggleRemove() {
        val state = _gameState.value
        if (!idle(state)) return
        if (state.removeArmed) {
            _gameState.value = state.copy(removeArmed = false)
            return
        }
        if (state.columns.all { it.isEmpty() }) {
            notice("No sprites to remove yet")
            return
        }
        if (state.powerUps.remove <= 0 && state.coins < DropPowerUps.COST) {
            notice("You need ${DropPowerUps.COST} coins for another Remove")
            return
        }
        _gameState.value = state.copy(removeArmed = true, isGameOver = false)
        notice("Tap a sprite to remove it")
    }

    /** Remove the tile [tileId] while the hammer is armed. */
    fun removeTile(tileId: Long) {
        val state = _gameState.value
        if (!state.removeArmed || !idle(state)) return
        if (state.columns.none { col -> col.any { it.id == tileId } }) return
        busy = true
        resolveJob = viewModelScope.launch {
            try {
                if (!takeCharge(DropPower.REMOVE)) {
                    _gameState.update { it.copy(removeArmed = false) }
                    return@launch
                }
                val now = _gameState.value
                pushHistory(now)
                // The hammer swings over the tile first; then it's knocked out and the lane closes up.
                _gameState.value = now.copy(
                    removeArmed = false, removingTileId = tileId, isResolving = true, powerResolving = true,
                    comboCount = 0, justUndid = false, actionCount = now.actionCount + 1
                )
                _events.tryEmit(DropGameEvent.REMOVE_HIT)
                delay(if (spritePacing) DropAnim.SPRITE_REMOVE else DropAnim.REMOVE_HIT)
                val result = engine.removeTile(now.columns, tileId, now.bestTileEver)
                _gameState.update { it.copy(removingTileId = null) }
                if (result == null) {
                    _gameState.update { it.copy(isResolving = false, powerResolving = false) }
                    return@launch
                }
                finishPowerUp(result)
            } finally {
                busy = false
            }
        }
    }

    /** Freeze: the launcher keeps its tile for the next few shots. */
    fun freeze() {
        val state = _gameState.value
        if (state.isGameOver || !idle(state)) return
        if (state.freezeShots > 0) {
            notice("Already frozen: ${state.freezeShots} shots left")
            return
        }
        busy = true
        viewModelScope.launch {
            try {
                if (!takeCharge(DropPower.FREEZE)) return@launch
                _gameState.update { it.copy(freezeShots = DropMergeConfig.FREEZE_SHOTS, removeArmed = false) }
                _events.tryEmit(DropGameEvent.POWER)
                notice("Frozen: you keep this sprite for ${DropMergeConfig.FREEZE_SHOTS} shots")
            } finally {
                busy = false
            }
        }
    }

    /** Called when the player taps the level chest. */
    fun chestTapped() {
        notice("Level up to open the chest: it gives a free power-up")
    }

    /** Called by the UI after the unlock banner finishes. */
    fun clearUnlockBanner() {
        if (_gameState.value.justUnlockedValue != null) {
            _gameState.value = _gameState.value.copy(justUnlockedValue = null)
        }
    }

    // ─────────────────────────────── Internals ──────────────────────────────

    /** Plays back a cascade step by step; returns the best tile reached. */
    private suspend fun playSteps(steps: List<DropStep>, startBest: Int): Int {
        var runningBest = startBest
        for (step in steps) {
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
                DropStepKind.PLACE -> delay(if (spritePacing) DropAnim.SPRITE_SHOT else DropAnim.SHOT_TRAVEL)
                DropStepKind.SETTLE -> delay(DropAnim.SETTLE_STEP)
                DropStepKind.MERGE -> {
                    _events.tryEmit(if (step.chainIndex > 1) DropGameEvent.COMBO else DropGameEvent.MERGE)
                    if (step.unlockedValue != null) _events.tryEmit(DropGameEvent.UNLOCK)
                    delay(if (spritePacing) DropAnim.SPRITE_MERGE else DropAnim.MERGE_STEP)
                }
                DropStepKind.PURGE -> {
                    _events.tryEmit(DropGameEvent.PURGE)
                    delay(if (spritePacing) DropAnim.SPRITE_PURGE else DropAnim.PURGE_STEP)
                }
            }
        }
        return runningBest
    }

    /** Plays a shuffle/remove result, then settles the board without reloading the launcher. */
    private suspend fun finishPowerUp(result: DropShotResult) {
        val runningBest = playSteps(result.steps, _gameState.value.bestTileEver)
        _gameState.update { it.copy(isResolving = false, powerResolving = false, canUndo = true, lastStep = null) }
        gainXp(result.totalGained)
        repository.updateBests(_gameState.value.score, runningBest)
        checkLocked()
    }

    private fun checkLocked() {
        val state = _gameState.value
        if (engine.isBoardLocked(state.columns, state.currentValue)) {
            _gameState.value = state.copy(isGameOver = true)
            onGameOver()
        }
    }

    private fun pushHistory(state: DropMergeState) {
        history.addLast(
            DropUndoSnapshot(
                columns = state.columns,
                score = state.score,
                currentValue = state.currentValue,
                nextValue = state.nextValue,
                bestTileEver = state.bestTileEver,
                freezeShots = state.freezeShots,
                level = state.level,
                xp = state.xp
            )
        )
        while (history.size > DropMergeConfig.UNDO_DEPTH) history.removeFirst()
    }

    /** Uses one charge of [power], or buys one with coins once they've run out. */
    private suspend fun takeCharge(power: DropPower): Boolean {
        val name = power.label()
        if (_gameState.value.powerUps[power] <= 0) {
            if (!Progress.spendCoins(DropPowerUps.COST)) {
                notice("You need ${DropPowerUps.COST} coins for another $name")
                return false
            }
            notice("Bought 1 $name for ${DropPowerUps.COST} coins")
            logPowerUp(power, bought = true)
            Progress.powerUpUsed()
            return true
        }
        _gameState.update { it.copy(powerUps = it.powerUps.plus(power, -1)) }
        logPowerUp(power, bought = false)
        Progress.powerUpUsed()
        return true
    }

    /** XP is earned 1:1 with score. Each level-up opens the chest: +1 of a random power-up. */
    private fun gainXp(amount: Int) {
        if (amount <= 0) return
        val state = _gameState.value
        var level = state.level
        var xp = state.xp + amount
        var powerUps = state.powerUps
        var reward: DropPower? = null
        while (xp >= DropMergeConfig.xpForLevel(level)) {
            xp -= DropMergeConfig.xpForLevel(level)
            level += 1
            reward = DropPower.entries.random()
            powerUps = powerUps.plus(reward, 1)
        }
        _gameState.value = state.copy(
            level = level,
            xp = xp,
            powerUps = powerUps,
            levelUp = reward?.let { DropLevelUp(level, it, ++levelUpKey) } ?: state.levelUp
        )
        if (reward != null) {
            _events.tryEmit(DropGameEvent.LEVEL_UP)
            Telemetry.event("shooter_level_up", "level" to level)
            notice("Level $level! The chest gave you +1 ${reward.label()}")
        }
        viewModelScope.launch { repository.saveLevel(level, xp) }
    }

    private fun notice(text: String) {
        _gameState.update { it.copy(notice = DropNotice(text, ++noticeKey)) }
    }

    private fun logPowerUp(power: DropPower, bought: Boolean) =
        Telemetry.event("power_up_used", "power_up" to power.name.lowercase(), "mode" to "shooter", "bought" to bought)

    private fun DropPower.label() = name.lowercase().replaceFirstChar { it.uppercase() }

    private fun onGameOver() {
        _events.tryEmit(DropGameEvent.GAME_OVER)
        val state = _gameState.value
        viewModelScope.launch {
            repository.updateBests(state.score, state.bestTileEver)
        }
    }

    /** Call when the screen is gone for good. */
    fun onCleared() {
        resolveJob?.cancel()
        viewModelScope.cancel()
    }

    companion object {
        const val SOUND_KEY = "sound_enabled"
        const val VIBRATION_KEY = "vibration_enabled"
        const val TUTORIAL_KEY = "seen_neon_drop_tutorial"
    }
}
