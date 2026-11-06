package com.avfusionapps.game_2048.shared.game

import com.avfusionapps.game_2048.shared.platform.nowMillis
import com.avfusionapps.game_2048.shared.platform.Telemetry
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

private var noticeSeq = 0L

/**
 * Represents the overall state of the game, including UI elements and logic state.
 */
data class GameState(
    val grid: List<List<Int>> = emptyList(), // Initialize empty, set properly in init
    val score: Int = 0,
    val highScore: Int = 0,
    val playerName: String = GameSettingsRepository.DEFAULT_PLAYER_NAME,
    val gridSize: Int = 4, // Default size
    val isGameOver: Boolean = false,
    val tileAnimationInfo: Map<Pair<Int, Int>, TileAnimationInfo> = emptyMap(),
    val moveCount: Int = 0,
    val currentLevel: Int = 1,
    val unlockedLevels: Set<Int> = setOf(1),
    val hasSavedGame: Boolean = false,
    /** Back-to-back merging moves; the score multiplier is min(combo, 5). */
    val combo: Int = 0,
    val powerUps: PowerUps = PowerUps(),
    /** Moves left with no new tile spawning (Freeze power-up). */
    val freezeMoves: Int = 0,
    /** Hammer is armed: the next tap on a tile breaks it. */
    val hammerArmed: Boolean = false,
    /** Cell currently being smashed by the hammer (plays its animation before it empties). */
    val breakingCell: Pair<Int, Int>? = null,
    /** Points and coins from the last move, for the floating popup. */
    val lastGain: MoveGain? = null,
    /** Moves left before the Shadow Rock crumbles; 0 when there is no rock. */
    val rockTtl: Int = 0,
    /** Moves in a row without a merge; 10 drops a Shadow Rock. */
    val dryMoves: Int = 0,
    /** Blocked swipes in a row; 3 drop a Shadow Rock. */
    val bonks: Int = 0,
    /** Bumped on every blocked swipe so the board can shake and the sprites get dizzy. */
    val bonkKey: Int = 0,
    val bonkDirection: Direction? = null,
    /** A short message floated over the middle of the board ("Bonk!", "New friend: Frosty!"). */
    val notice: BoardNotice? = null,
    /** Bumped on every undo so the sprites can fade back in. */
    val rewindKey: Int = 0
)

/** Grid value of the Shadow Rock penalty tile: it slides but never merges. */
const val SHADOW_ROCK = -1
const val ROCK_LIFETIME = 12
private const val DRY_MOVES_FOR_ROCK = 10
private const val BONKS_FOR_ROCK = 3

data class BoardNotice(val text: String, val key: Long = nowMillis() * 1000 + (noticeSeq++))

/** Power-up charges for the current game. When one hits 0, another costs [PowerUps.COST] coins. */
data class PowerUps(
    val undo: Int = 3,
    val hint: Int = 5,
    val hammer: Int = 3,
    val shuffle: Int = 3,
    val freeze: Int = 3
) {
    companion object {
        const val COST = 50
    }
}

enum class PowerUp { UNDO, HINT, HAMMER, SHUFFLE, FREEZE }

/** What the last move earned. [key] is the move number, so the board knows when to replay the popup. */
data class MoveGain(val points: Int, val multiplier: Int, val coins: Int, val key: Int)

/**
 * Enum representing the possible move directions.
 */
enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

/**
 * Helper data class used internally during line processing to track tile origins.
 * @param value The final value of the tile after potential merge.
 * @param originalIndex The index within the *input* line where this tile (or the first part of a merge) originated.
 * @param mergedFromIndex The index within the *input* line where the second tile involved in a merge originated. Null if no merge.
 */
private data class ProcessedTile(
    val value: Int,
    val originalIndex: Int,
    val mergedFromIndex: Int? = null
)


class GameViewModel {

    val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Call when the owning screen/app goes away. */
    fun onCleared() {
        saveCurrentGameState()
        viewModelScope.cancel()
    }

    /** One-shot user messages (replaces Android Toasts); the UI shows them as snackbars/toasts. */
    private val _toast = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val toastMessages: SharedFlow<String> = _toast.asSharedFlow()
    private fun toast(msg: String) { _toast.tryEmit(msg) }

    fun saveCurrentGameState() {
        if (!gameState.isGameOver && gameState.moveCount > 0 && !gameState.hasSavedGame) {
            gameState = gameState.copy(hasSavedGame = true)
            _gameStateFlow.value = gameState
        }
    }

    private val settingsRepository = GameSettingsRepository
    private val gameMoveRepository = GameMoveRepository()

    private var _state by mutableStateOf(GameState())

    /** Current game state (Compose state, mirrored to [gameStateFlow]). */
    var gameState: GameState
        get() = _state
        private set(value) { _state = value; _gameStateFlow.value = value }

    private val _gameStateFlow = MutableStateFlow(GameState())
    val gameStateFlow: StateFlow<GameState> = _gameStateFlow.asStateFlow()

    private val _resumePrompt = MutableStateFlow(false)
    val resumePrompt: StateFlow<Boolean> = _resumePrompt.asStateFlow()

    private val _newlyUnlockedLevel = MutableStateFlow<Int?>(null)
    val newlyUnlockedLevel: StateFlow<Int?> = _newlyUnlockedLevel.asStateFlow()

    private val _newlyUnlockedTileValue = MutableStateFlow<Int?>(null)
    val newlyUnlockedTileValue: StateFlow<Int?> = _newlyUnlockedTileValue.asStateFlow()

    private val _shouldShowNameEditDialog = MutableStateFlow(false)
    val shouldShowNameEditDialog: StateFlow<Boolean> = _shouldShowNameEditDialog.asStateFlow()

    fun resetNameEditDialogState() {
        _shouldShowNameEditDialog.value = false
    }

    private fun updateGameState(newState: GameState) {
        val old = gameState
        gameState = newState
        _gameStateFlow.value = newState
        if (old.currentLevel != newState.currentLevel || old.unlockedLevels != newState.unlockedLevels) {
            settingsRepository.saveLevels(newState.currentLevel, newState.unlockedLevels)
        }
    }

    private var _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    val persistentPlayerName: StateFlow<String> = settingsRepository.playerNameFlow
    val persistentHighScore: StateFlow<Int> = settingsRepository.highScoreFlow

    private val _mergeEvent = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val mergeEvent: SharedFlow<Unit> = _mergeEvent.asSharedFlow()

    /** One-shot sound cue per move (a SoundManager.SOUND_* id). */
    private val _soundEvent = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val soundEvent: SharedFlow<Int> = _soundEvent.asSharedFlow()

    val hasSavedGameFlow: StateFlow<Boolean> = gameStateFlow
        .map { it.hasSavedGame }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val currentLevel: StateFlow<Int> = gameStateFlow
        .map { it.currentLevel }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    val unlockedLevels: StateFlow<Set<Int>> = gameStateFlow
        .map { it.unlockedLevels }
        .stateIn(viewModelScope, SharingStarted.Eagerly, setOf(1))

    val seenSprites: StateFlow<Set<Int>> = settingsRepository.seenSpritesFlow
    val coins: StateFlow<Int> = settingsRepository.coinsFlow
    val soundEnabled: StateFlow<Boolean> = settingsRepository.soundEnabledFlow
    val vibrationEnabled: StateFlow<Boolean> = settingsRepository.vibrationEnabledFlow
    val remindersEnabled: StateFlow<Boolean> = settingsRepository.remindersEnabledFlow
    val hasSeenClassicTutorial: StateFlow<Boolean?> = settingsRepository.hasSeenClassicTutorialFlow

    fun setHasSeenClassicTutorial(hasSeen: Boolean) = settingsRepository.updateHasSeenClassicTutorial(hasSeen)
    fun updateSoundEnabled(enabled: Boolean) = settingsRepository.updateSoundEnabled(enabled)
    fun updateVibrationEnabled(enabled: Boolean) = settingsRepository.updateVibrationEnabled(enabled)
    /** Reminders are not scheduled on iOS yet; only the setting is stored. */
    fun updateRemindersEnabled(enabled: Boolean) = settingsRepository.updateRemindersEnabled(enabled)

    private fun calculateCurrentLevel(grid: List<List<Int>>): Int {
        val highestTile = grid.flatten().maxOrNull() ?: 0
        return LevelProgression.getLevelForTileValue(highestTile)
    }

    private fun checkLevelUnlock(
        grid: List<List<Int>>,
        currentUnlockedLevels: Set<Int>,
        previousCurrentLevel: Int
    ): Int? {
        val highestTile = grid.flatten().maxOrNull() ?: 0
        val newLevel = LevelProgression.getLevelForTileValue(highestTile)
        return if (newLevel > previousCurrentLevel && newLevel !in currentUnlockedLevels) newLevel else null
    }

    init {
        val initialName = persistentPlayerName.value
        val initialHighScore = persistentHighScore.value
        val currentLevel = settingsRepository.loadCurrentLevel()
        val unlockedLevels = settingsRepository.loadUnlockedLevels()
        val lastMove = gameMoveRepository.getLastMove()
        if (lastMove != null) {
            updateGameState(
                gameState.copy(
                    playerName = initialName,
                    highScore = initialHighScore,
                    currentLevel = currentLevel,
                    unlockedLevels = unlockedLevels,
                    grid = lastMove.grid,
                    score = lastMove.score,
                    gridSize = lastMove.grid.size,
                    moveCount = lastMove.moveNumber,
                    hasSavedGame = true
                )
            )
            _resumePrompt.value = true
        } else {
            updateGameState(
                gameState.copy(
                    playerName = initialName,
                    highScore = initialHighScore,
                    currentLevel = currentLevel,
                    unlockedLevels = unlockedLevels,
                    grid = List(gameState.gridSize) { List(gameState.gridSize) { 0 } }
                )
            )
            initializeGame()
        }
        updateCanUndoState()
    }

    fun markResumableWithoutMove() {
        val hasTiles = gameState.grid.any { row -> row.any { it != 0 } }
        if (hasTiles && !gameState.isGameOver) {
            updateGameState(gameState.copy(hasSavedGame = true))
            if (gameMoveRepository.getLastMove() == null) {
                gameMoveRepository.saveMove(gameState.grid, gameState.score, gameState.moveCount)
                updateCanUndoState()
            }
        }
    }

    /** Updates the player name in local state and persists it. */
    fun updatePlayerName(name: String) {
        val validName = name.ifBlank { GameSettingsRepository.DEFAULT_PLAYER_NAME }
        updateGameState(gameState.copy(playerName = validName))
        settingsRepository.updatePlayerName(validName)
        toast("Name updated!")
    }

    /** Sets a new grid size, resets the game board, and initializes it. */
    fun updateGridSize(size: Int) {
        if (size >= 3 && size != gameState.gridSize) {
            updateGameState(
                gameState.copy(
                    gridSize = size,
                    score = 0, // Reset score for new size
                    isGameOver = false,
                    moveCount = 0,
                    tileAnimationInfo = emptyMap(),
                    grid = List(size) { List(size) { 0 } }
                ))
            initializeGame()
        } else if (size == gameState.gridSize) {
            initializeGame()
        }
    }

    /** Resets the game board (adds initial tiles), score, and move count. Keeps name/high score. */
    fun initializeGame() {
        val size = gameState.gridSize
        val newGrid = MutableList(size) { MutableList(size) { 0 } }
        addRandomTile(newGrid)
        addRandomTile(newGrid)
        updateGameState(
            gameState.copy(
                grid = newGrid,
                score = 0,
                isGameOver = false,
                moveCount = 0,
                tileAnimationInfo = emptyMap(),
                hasSavedGame = false,
                combo = 0,
                powerUps = PowerUps(),
                freezeMoves = 0,
                hammerArmed = false,
                breakingCell = null,
                lastGain = null,
                rockTtl = 0,
                dryMoves = 0,
                bonks = 0,
                notice = null
            )
        )
        markSeen(newGrid)

        viewModelScope.launch {
            gameMoveRepository.clearAllMoves()
            updateCanUndoState()
        }
    }

    /** Resumes the previous game from the last saved move */
    fun resumeGame() {
        viewModelScope.launch {
            val lastMove = gameMoveRepository.getLastMove()
            if (lastMove != null) {
                updateGameState(
                    gameState.copy(
                        grid = lastMove.grid,
                        score = lastMove.score,
                        moveCount = lastMove.moveNumber,
                        isGameOver = false,
                        tileAnimationInfo = emptyMap(),
                        hasSavedGame = false,
                        combo = 0,
                        freezeMoves = 0,
                        hammerArmed = false,
                        breakingCell = null,
                        lastGain = null,
                        rockTtl = if (lastMove.grid.flatten().contains(SHADOW_ROCK)) ROCK_LIFETIME else 0,
                        dryMoves = 0,
                        bonks = 0,
                        notice = null
                    )
                )
                markSeen(lastMove.grid)

                updateCanUndoState()
            } else {
                initializeGame()
            }
        }
    }

    /**
     * Processes a player move in the specified direction.
     * Calculates new tile positions, merges, score, and animation information.
     * Updates the gameState.
     */
    fun move(direction: Direction) {
        if (gameState.isGameOver || gameState.breakingCell != null) return
        if (gameState.hammerArmed) updateGameState(gameState.copy(hammerArmed = false))

        val currentGrid = gameState.grid
        val size = gameState.gridSize
        val newGrid = MutableList(size) { MutableList(size) { 0 } }
        var boardMoved = false
        var mergedThisTurn = false
        var scoreIncreaseThisTurn = 0
        var coinsThisTurn = 0
        val animationInfoMap = mutableMapOf<Pair<Int, Int>, TileAnimationInfo>()

        for (i in 0 until size) {
            val currentLine = getLine(currentGrid, i, direction)
            val (processedLine, lineScore, lineMoved) = processLine(currentLine)

            if (lineMoved) boardMoved = true
            scoreIncreaseThisTurn += lineScore

            processedLine.forEachIndexed { resultIndex, processedTile ->
                if (processedTile.value > 0) {
                    val finalPos = getFinalPosition(i, resultIndex, direction, size)
                    newGrid[finalPos.first][finalPos.second] =
                        processedTile.value
                    val originalPos =
                        getFinalPosition(i, processedTile.originalIndex, direction, size)

                    val positionChanged = finalPos != originalPos
                    val didMerge = processedTile.mergedFromIndex != null

                    if (positionChanged || didMerge) {
                        val mergedFromPos = if (didMerge) {
                            getFinalPosition(i, processedTile.mergedFromIndex!!, direction, size)
                        } else null

                        animationInfoMap[finalPos] = TileAnimationInfo(
                            startPosition = originalPos,
                            mergedFromPosition = mergedFromPos,
                            isMerged = didMerge,
                            isNew = false
                        )
                    }

                    if (didMerge) {
                        mergedThisTurn = true
                        _mergeEvent.tryEmit(Unit)
                        // Merges of 32 and up pay coins: 32 → 1, 64 → 2 … 2048 → 64.
                        if (processedTile.value >= 32) coinsThisTurn += processedTile.value / 32
                    }
                }
            }
        }


        if (boardMoved) {
            // Freeze skips the spawn for a few moves.
            val frozen = gameState.freezeMoves > 0
            if (!frozen) {
                val (newTileRow, newTileCol) = addRandomTile(newGrid)
                if (newTileRow != -1) {
                    animationInfoMap[Pair(newTileRow, newTileCol)] = TileAnimationInfo(isNew = true)
                }
            }

            // Shadow Rock: counts down each move and crumbles at 0. A dry spell drops a new one.
            var notice: BoardNotice? = null
            var rockTtl = gameState.rockTtl
            if (rockTtl > 0) {
                rockTtl--
                if (rockTtl == 0) {
                    removeRock(newGrid)
                    notice = BoardNotice("The Shadow Rock crumbled!")
                }
            }
            val dryMoves = if (mergedThisTurn) 0 else gameState.dryMoves + 1
            var dryAfter = dryMoves
            if (dryMoves >= DRY_MOVES_FOR_ROCK) {
                dryAfter = 0
                val rockAt = dropRock(newGrid)
                if (rockAt != null) {
                    animationInfoMap[rockAt] = TileAnimationInfo(isNew = true)
                    rockTtl = ROCK_LIFETIME
                    notice = BoardNotice("10 moves without a merge. A Shadow Rock fell in!")
                }
            }

            // New sprites found this move (merges only; spawns are always 2 or 4).
            val mergedValues = animationInfoMap.filterValues { it.isMerged }.keys.map { newGrid[it.first][it.second] }
            val firstTimers = mergedValues.filter { it >= 8 && it !in seenSprites.value }
            if (firstTimers.isNotEmpty()) {
                val best = firstTimers.max()
                notice = BoardNotice("New friend: ${com.avfusionapps.game_2048.shared.sprites.ElementalSprites.specFor(best).name}!")
            }
            markSeen(newGrid)

            // Combo: merging on back-to-back moves multiplies the points, up to x5.
            val newCombo = if (mergedThisTurn) gameState.combo + 1 else 0
            val multiplier = newCombo.coerceIn(1, 5)
            scoreIncreaseThisTurn *= multiplier
            if (coinsThisTurn > 0) {
                settingsRepository.addCoins(coinsThisTurn)
            }

            val newScore = gameState.score + scoreIncreaseThisTurn
            val currentPersistentHighScore =
                persistentHighScore.value
            if (newScore > currentPersistentHighScore) {
                settingsRepository.updateHighScoreIfHigher(newScore)
            }
            val newLocalHighScore = maxOf(newScore, gameState.highScore)

            val gameOver = isGameOver(newGrid)

            // Check for level unlocks
            val currentLevel = calculateCurrentLevel(newGrid)
            val newlyUnlockedLevel = checkLevelUnlock(
                newGrid,
                gameState.unlockedLevels,
                gameState.currentLevel
            )
            
            val newUnlockedLevels = if (newlyUnlockedLevel != null) {
                gameState.unlockedLevels + newlyUnlockedLevel
            } else {
                gameState.unlockedLevels
            }

            val previousHighestTile = gameState.grid.flatten().maxOrNull() ?: 0
            val currentHighestTile = newGrid.flatten().maxOrNull() ?: 0
            val reachedGoal = currentHighestTile >= 128 && currentHighestTile > previousHighestTile
            if (reachedGoal) {
                _newlyUnlockedTileValue.value = currentHighestTile
            }

            // One sound per move: celebration on a new milestone tile, else merge / slide / game over.
            when {
                gameOver -> _soundEvent.tryEmit(SoundManager.SOUND_GAME_OVER)
                reachedGoal -> _soundEvent.tryEmit(SoundManager.SOUND_LEVEL_UP)
                mergedThisTurn -> _soundEvent.tryEmit(SoundManager.SOUND_MERGE)
                else -> _soundEvent.tryEmit(SoundManager.SOUND_MOVE)
            }

            // Long-term progress: stats, achievements and the daily challenge.
            if (gameState.moveCount == 0) Progress.gameStarted(GameMode.CLASSIC)
            Progress.merged(GameMode.CLASSIC, mergedValues)
            if (newCombo >= 2) Progress.combo(newCombo)
            if (scoreIncreaseThisTurn > 0) Progress.score(newScore)

            val newMoveCount = gameState.moveCount + 1
            updateGameState(
                gameState.copy(
                    grid = newGrid,
                    score = newScore,
                    highScore = newLocalHighScore,
                    currentLevel = currentLevel,
                    unlockedLevels = newUnlockedLevels,
                    isGameOver = gameOver,
                    tileAnimationInfo = animationInfoMap,
                    moveCount = newMoveCount,
                    combo = newCombo,
                    freezeMoves = if (frozen) gameState.freezeMoves - 1 else 0,
                    rockTtl = rockTtl,
                    dryMoves = dryAfter,
                    bonks = 0,
                    notice = notice ?: gameState.notice,
                    lastGain = if (scoreIncreaseThisTurn > 0) {
                        MoveGain(scoreIncreaseThisTurn, multiplier, coinsThisTurn, newMoveCount)
                    } else null
                )
            )

            // Handle level unlock
            if (newlyUnlockedLevel != null) {
                _newlyUnlockedLevel.value = newlyUnlockedLevel
                Telemetry.event("level_unlocked", "level" to newlyUnlockedLevel)
                settingsRepository.saveLevels(currentLevel, newUnlockedLevels)
            }

            viewModelScope.launch {
                val moveId = gameMoveRepository.saveMove(newGrid, newScore, newMoveCount)
                println("Saved move #$newMoveCount with ID $moveId, grid size: ${newGrid.size}")

                gameMoveRepository.keepOnlyLastMoves(3)
                updateCanUndoState()

                if (!gameOver) {
                    updateGameState(gameState.copy(hasSavedGame = true))
                }
            }

            if (gameOver) {
                println("Game Over! Final Score: $newScore")
            }

        } else {
            if (isGameOver(gameState.grid)) { // Check the current grid
                updateGameState(gameState.copy(isGameOver = true))
                println("Game Over! (No valid moves left)")
            } else {
                // Bonk: swiping where nothing can move makes everyone dizzy and breaks the combo.
                // Three in a row drop a Shadow Rock.
                val bonks = gameState.bonks + 1
                var notice = BoardNotice(if (gameState.combo >= 2) "Bonk! Combo lost." else "Bonk!")
                var grid = gameState.grid
                var rockTtl = gameState.rockTtl
                var anim = gameState.tileAnimationInfo
                var moveCount = gameState.moveCount
                var bonksAfter = bonks
                if (bonks >= BONKS_FOR_ROCK) {
                    bonksAfter = 0
                    val g = grid.map { it.toMutableList() }.toMutableList()
                    val rockAt = dropRock(g)
                    if (rockAt != null) {
                        grid = g
                        rockTtl = ROCK_LIFETIME
                        moveCount += 1
                        anim = mapOf(rockAt to TileAnimationInfo(isNew = true))
                        notice = BoardNotice("Three bonks in a row. A Shadow Rock fell in!")
                        viewModelScope.launch {
                            gameMoveRepository.saveMove(g, gameState.score, moveCount)
                            gameMoveRepository.keepOnlyLastMoves(3)
                            updateCanUndoState()
                        }
                    }
                }
                _soundEvent.tryEmit(SoundManager.SOUND_MOVE)
                updateGameState(
                    gameState.copy(
                        grid = grid,
                        combo = 0,
                        bonks = bonksAfter,
                        bonkKey = gameState.bonkKey + 1,
                        bonkDirection = direction,
                        rockTtl = rockTtl,
                        tileAnimationInfo = anim,
                        moveCount = moveCount,
                        notice = notice,
                        isGameOver = isGameOver(grid)
                    )
                )
            }
        }
    }

    /** Puts the Shadow Rock on a random empty cell unless one is already out. Returns where, or null. */
    private fun dropRock(grid: MutableList<MutableList<Int>>): Pair<Int, Int>? {
        if (grid.flatten().contains(SHADOW_ROCK)) return null
        val empty = mutableListOf<Pair<Int, Int>>()
        grid.forEachIndexed { r, row -> row.forEachIndexed { c, v -> if (v == 0) empty.add(r to c) } }
        val at = empty.randomOrNull() ?: return null
        grid[at.first][at.second] = SHADOW_ROCK
        return at
    }

    private fun removeRock(grid: MutableList<MutableList<Int>>) {
        grid.forEachIndexed { r, row -> row.forEachIndexed { c, v -> if (v == SHADOW_ROCK) grid[r][c] = 0 } }
    }

    private fun markSeen(grid: List<List<Int>>) {
        val values = grid.flatten().filter { it > 0 && it !in seenSprites.value }.toSet()
        if (values.isNotEmpty()) viewModelScope.launch {
            settingsRepository.markSpritesSeen(values)
            Progress.spritesChanged()
        }
    }

    /**
     * Takes one charge of [kind], buying one for [PowerUps.COST] coins when it has run out.
     * Returns false (and tells the player) when neither is possible.
     */
    private fun takeCharge(kind: PowerUp): Boolean {
        val p = gameState.powerUps
        val left = when (kind) {
            PowerUp.UNDO -> p.undo
            PowerUp.HINT -> p.hint
            PowerUp.HAMMER -> p.hammer
            PowerUp.SHUFFLE -> p.shuffle
            PowerUp.FREEZE -> p.freeze
        }
        if (left <= 0) {
            if (!settingsRepository.spendCoins(PowerUps.COST)) {
                toast("You need ${PowerUps.COST} coins for another ${kind.label()}")
                return false
            }
            toast("Bought 1 ${kind.label()} for ${PowerUps.COST} coins")
            logPowerUp(kind, bought = true)
            Progress.powerUpUsed()
            return true
        }
        val next = when (kind) {
            PowerUp.UNDO -> p.copy(undo = left - 1)
            PowerUp.HINT -> p.copy(hint = left - 1)
            PowerUp.HAMMER -> p.copy(hammer = left - 1)
            PowerUp.SHUFFLE -> p.copy(shuffle = left - 1)
            PowerUp.FREEZE -> p.copy(freeze = left - 1)
        }
        updateGameState(gameState.copy(powerUps = next))
        logPowerUp(kind, bought = false)
        Progress.powerUpUsed()
        return true
    }

    private fun PowerUp.label() = name.lowercase()

    private fun logPowerUp(kind: PowerUp, bought: Boolean) =
        Telemetry.event("power_up_used", "power_up" to kind.name.lowercase(), "mode" to "classic", "bought" to bought)

    /** Arms the hammer, or puts it away if it is already armed. The charge is taken on the hit. */
    fun toggleHammer() {
        if (gameState.breakingCell != null) return
        if (gameState.hammerArmed) {
            updateGameState(gameState.copy(hammerArmed = false))
            return
        }
        if (gameState.grid.flatten().count { it != 0 } < 2) {
            toast("Nothing worth breaking yet")
            return
        }
        if (gameState.powerUps.hammer <= 0 && coins.value < PowerUps.COST) {
            toast("You need ${PowerUps.COST} coins for another hammer")
            return
        }
        updateGameState(gameState.copy(hammerArmed = true))
    }

    /** Smashes the tile at (row, col): plays the break animation, then empties the cell as a new move. */
    fun hammerAt(row: Int, col: Int) {
        if (!gameState.hammerArmed || gameState.grid[row][col] == 0) return
        viewModelScope.launch {
            if (!takeCharge(PowerUp.HAMMER)) {
                updateGameState(gameState.copy(hammerArmed = false))
                return@launch
            }
            updateGameState(gameState.copy(hammerArmed = false, breakingCell = row to col))
            // The hammer lands at 340 ms and the sprite crumbles for 560 ms before the cell empties.
            kotlinx.coroutines.delay(900)
            val grid = gameState.grid.map { it.toMutableList() }.toMutableList()
            if (grid[row][col] == SHADOW_ROCK) Progress.rockSmashed()
            grid[row][col] = 0
            commitPowerUpBoard(grid, emptyMap())
        }
    }

    /** Moves every tile to a random cell. */
    fun shuffle() {
        if (gameState.breakingCell != null) return
        val tiles = mutableListOf<Pair<Pair<Int, Int>, Int>>()
        gameState.grid.forEachIndexed { r, row -> row.forEachIndexed { c, v -> if (v != 0) tiles.add((r to c) to v) } }
        if (tiles.size < 2) {
            toast("Not enough tiles to shuffle")
            return
        }
        viewModelScope.launch {
            if (!takeCharge(PowerUp.SHUFFLE)) return@launch
            val size = gameState.gridSize
            val cells = (0 until size * size).map { it / size to it % size }.shuffled()
            val grid = MutableList(size) { MutableList(size) { 0 } }
            val anim = mutableMapOf<Pair<Int, Int>, TileAnimationInfo>()
            tiles.forEachIndexed { k, (from, v) ->
                val to = cells[k]
                grid[to.first][to.second] = v
                anim[to] = TileAnimationInfo(startPosition = from, shuffled = true)
            }
            commitPowerUpBoard(grid, anim)
        }
    }

    /** No new tiles for the next 3 moves. */
    fun freeze() {
        if (gameState.isGameOver || gameState.freezeMoves > 0) return
        viewModelScope.launch {
            if (!takeCharge(PowerUp.FREEZE)) return@launch
            updateGameState(gameState.copy(freezeMoves = 3))
        }
    }

    /** Saves a board changed by a power-up as its own move, so Undo can take it back. */
    private fun commitPowerUpBoard(grid: List<List<Int>>, anim: Map<Pair<Int, Int>, TileAnimationInfo>) {
        val newMoveCount = gameState.moveCount + 1
        updateGameState(
            gameState.copy(
                grid = grid,
                moveCount = newMoveCount,
                tileAnimationInfo = anim,
                breakingCell = null,
                isGameOver = isGameOver(grid),
                lastGain = null,
                rockTtl = if (grid.flatten().contains(SHADOW_ROCK)) gameState.rockTtl else 0,
                notice = if (gameState.rockTtl > 0 && !grid.flatten().contains(SHADOW_ROCK)) BoardNotice("Smashed the Shadow Rock!") else gameState.notice
            )
        )
        gameMoveRepository.saveMove(grid, gameState.score, newMoveCount)
        gameMoveRepository.keepOnlyLastMoves(3)
        updateCanUndoState()
    }

    /**
     * Undoes the last move by restoring the previous game state from the database.
     * @return True if the undo was successful, false if there are no moves to undo.
     */
    fun undoMove() {
        viewModelScope.launch {
            val moves = gameMoveRepository.getLastMoves(2)

            if (moves.size >= 2) {
                if (!takeCharge(PowerUp.UNDO)) return@launch
                val latest = moves[0]
                val previousMove = moves[1]

                val restoredGrid: List<List<Int>> = previousMove.grid.map { it.toList() }
                
                // Recalculate current level from the restored grid
                val restoredCurrentLevel = calculateCurrentLevel(previousMove.grid)
                val restoredUnlockedLevels = gameState.unlockedLevels + restoredCurrentLevel

                updateGameState(
                    gameState.copy(
                        grid = restoredGrid,
                        score = previousMove.score,
                        moveCount = previousMove.moveNumber,
                        currentLevel = restoredCurrentLevel,
                        unlockedLevels = restoredUnlockedLevels,
                        tileAnimationInfo = emptyMap(),
                        isGameOver = false,
                        combo = 0,
                        hammerArmed = false,
                        lastGain = null,
                        rewindKey = gameState.rewindKey + 1,
                        rockTtl = if (restoredGrid.flatten().contains(SHADOW_ROCK)) {
                            if (gameState.rockTtl > 0) (gameState.rockTtl + 1).coerceAtMost(ROCK_LIFETIME) else 1
                        } else 0,
                        bonks = 0
                    )
                )

                gameMoveRepository.deleteMoveById(latest.id)
                updateCanUndoState()
            } else {
                toast("At least 2 move is required to use Undo")
            }
        }
    }

    /**
     * Resumes a saved game from the database.
     * This function ensures the grid is properly loaded from the saved state.
     */
    fun resumeSavedGame() {
        viewModelScope.launch {
            val lastMove = gameMoveRepository.getLastMove()
            if (lastMove != null) {
                println("Resuming game with grid: ${lastMove.grid}")
                println("Grid size: ${lastMove.grid.size}x${lastMove.grid.firstOrNull()?.size ?: 0}")
                println("Score: ${lastMove.score}, Move: ${lastMove.moveNumber}")

                val gridSize = lastMove.grid.size
                
                // Recalculate current level from the resumed grid
                val resumedCurrentLevel = calculateCurrentLevel(lastMove.grid)
                val resumedUnlockedLevels = gameState.unlockedLevels + resumedCurrentLevel

                gameState = gameState.copy(
                    grid = lastMove.grid,
                    gridSize = gridSize,
                    score = lastMove.score,
                    moveCount = lastMove.moveNumber,
                    currentLevel = resumedCurrentLevel,
                    unlockedLevels = resumedUnlockedLevels,
                    tileAnimationInfo = emptyMap(),
                    isGameOver = false,
                    hasSavedGame = false
                )
                updateCanUndoState()
            } else {
                // If no saved move found, initialize a new game
                println("No saved game found, starting new game")
                initializeGame()
            }
        }
    }

    /**
     * Declines to resume a saved game and starts a new game instead.
     */
    fun declineSavedGame() {
        gameState = gameState.copy(hasSavedGame = false)
        initializeGame() // This will clear saved moves and start a new game
    }

    fun consumeResumePrompt() { _resumePrompt.value = false }

    fun consumeNewlyUnlockedLevel() {
        _newlyUnlockedLevel.value = null
        _newlyUnlockedTileValue.value = null
    }

    /** Cloud sign-in is not part of the shared app: no-op. */
    fun loadUserDataFromFirebase() {}

    /** Reminders are not scheduled in the shared app: no-op. */
    fun enableNotification() {}

    private fun updateCanUndoState() {
        _canUndo.value = gameMoveRepository.getMoveCount() > 1
    }


    /** Clears the animation information map, typically called by UI after animations complete. */
    fun clearAnimationInfo() {
        // Avoid unnecessary state updates if map is already empty
        if (gameState.tileAnimationInfo.isNotEmpty()) {
            gameState = gameState.copy(tileAnimationInfo = emptyMap())
        }
    }


    /** Extracts a specific line (row or column) from the grid based on the move direction. */
    private fun getLine(grid: List<List<Int>>, index: Int, direction: Direction): List<Int> {
        val size = grid.size
        return when (direction) {
            // For UP/DOWN, index is the column index, we extract the column elements
            Direction.UP -> (0 until size).map { grid[it][index] }
            Direction.DOWN -> (size - 1 downTo 0).map { grid[it][index] } // Reverse order for processing
            // For LEFT/RIGHT, index is the row index, we extract the row elements
            Direction.LEFT -> grid[index]
            Direction.RIGHT -> grid[index].reversed() // Reverse order for processing
        }
    }

    /** Calculates the final (row, col) grid coordinates given the original line index, the tile's index within the processed line result, the direction, and grid size. */
    private fun getFinalPosition(
        lineIndex: Int,
        resultIndex: Int,
        direction: Direction,
        size: Int
    ): Pair<Int, Int> {
        return when (direction) {
            // For UP: resultIndex becomes row, lineIndex is column
            Direction.UP -> Pair(resultIndex, lineIndex)
            // For DOWN: row is calculated from bottom, lineIndex is column
            Direction.DOWN -> Pair(size - 1 - resultIndex, lineIndex)
            // For LEFT: lineIndex is row, resultIndex is column
            Direction.LEFT -> Pair(lineIndex, resultIndex)
            // For RIGHT: lineIndex is row, column is calculated from right
            Direction.RIGHT -> Pair(lineIndex, size - 1 - resultIndex)
        }
    }

    /**
     * Processes a single line (row or column) of tiles.
     * Handles sliding tiles to the beginning and merging adjacent identical tiles.
     * Returns detailed information about each resulting tile, the score increase, and whether the line changed.
     */
    private fun processLine(line: List<Int>): Triple<List<ProcessedTile>, Int, Boolean> {
        // Map original indices to their non-zero values
        val indexedNonZero =
            line.mapIndexedNotNull { index, value -> if (value != 0) index to value else null }

        if (indexedNonZero.isEmpty()) {
            // If the line was empty, return an empty processed list and no change
            val resultList = List(line.size) { ProcessedTile(0, -1) } // Pad with empty placeholders
            return Triple(resultList, 0, false)
        }

        val result = mutableListOf<ProcessedTile>()
        var score = 0
        var i = 0
        var lineChanged = false // Flag if this specific line changed

        // Iterate through the non-zero tiles found
        while (i < indexedNonZero.size) {
            val (originalIndex1, value1) = indexedNonZero[i]

            // Check for a potential merge with the next tile
            if (i + 1 < indexedNonZero.size) {
                val (originalIndex2, value2) = indexedNonZero[i + 1]
                if (value1 == value2 && value1 > 0) {
                    // --- Merge Occurred ---
                    val mergedValue = value1 * 2
                    result.add(
                        ProcessedTile(
                            value = mergedValue,
                            originalIndex = originalIndex1,
                            mergedFromIndex = originalIndex2
                        )
                    )
                    score += mergedValue
                    lineChanged = true // Merge always means the line changed
                    i += 2 // Skip the next tile since it was merged
                    continue // Move to the next potential tile/pair
                }
            }

            // --- No Merge ---
            // Add the current tile as is
            result.add(ProcessedTile(value = value1, originalIndex = originalIndex1))
            // Check if this tile slid: its final position in the `result` list is different from its position `i` in the `indexedNonZero` list
            if (result.size - 1 != i) {
                lineChanged = true
            }
            i++ // Move to the next non-zero tile
        }

        // Now, explicitly check if the original grid index differs from the final grid index for *every* tile placed.
        // This catches cases where tiles only slid, without merging.
        result.forEachIndexed { finalLineIndex, processedTile ->
            if (processedTile.originalIndex != finalLineIndex) {
                lineChanged = true
            }
        }


        // Pad the result list with empty tile placeholders to match the original line size
        val finalResultList = mutableListOf<ProcessedTile>()
        finalResultList.addAll(result)
        while (finalResultList.size < line.size) {
            finalResultList.add(ProcessedTile(0, -1))
        }

        return Triple(finalResultList, score, lineChanged)
    }


    /** Adds a random tile (2 or 4) to an empty cell in the provided mutable grid. Returns the position (row, col) or (-1,-1) if full. */
    private fun addRandomTile(grid: MutableList<MutableList<Int>>): Pair<Int, Int> {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        grid.forEachIndexed { r, row ->
            row.forEachIndexed { c, value -> if (value == 0) emptyCells.add(r to c) }
        }
        return if (emptyCells.isNotEmpty()) {
            val (row, col) = emptyCells.random()
            grid[row][col] = if (Random.nextDouble() < 0.9) 2 else 4 // 90% chance of 2
            row to col // Return position
        } else {
            -1 to -1 // Indicate board is full
        }
    }

    /** Checks if the game is over (no empty cells and no possible adjacent merges). */
    private fun isGameOver(grid: List<List<Int>>): Boolean {
        val size = grid.size
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (grid[r][c] == 0) return false // Found an empty cell
                val current = grid[r][c]
                // Check right neighbor
                if (c + 1 < size && current > 0 && current == grid[r][c + 1]) return false
                // Check bottom neighbor
                if (r + 1 < size && current > 0 && current == grid[r + 1][c]) return false
            }
        }
        return true // No empty cells and no possible merges
    }

    fun showHint() {
        // Find the best move using a simple heuristic (highest score yield)
        val bestMove = findBestMove(gameState.grid)
        if (bestMove == null) {
            toast("No obvious good moves!")
            return
        }
        viewModelScope.launch {
            if (takeCharge(PowerUp.HINT)) {
                toast("Try swiping ${bestMove.name}")
            }
        }
    }

    private fun findBestMove(grid: List<List<Int>>): Direction? {
        var bestScore = -1
        var bestDirection: Direction? = null

        for (direction in Direction.values()) {
            // Simulate move
            val tempGrid = grid.map { it.toMutableList() }
            var scoreGained = 0
            var moved = false

            // Simplified simulation logic just to gauge immediate score gain
            when (direction) {
                Direction.UP -> {
                    for (c in 0 until gameState.gridSize) {
                        val col = mutableListOf<Int>()
                        for (r in 0 until gameState.gridSize) if (tempGrid[r][c] != 0) col.add(tempGrid[r][c])
                        
                        var i = 0
                        while (i < col.size - 1) {
                            if (col[i] > 0 && col[i] == col[i + 1]) {
                                scoreGained += col[i] * 2
                                col[i] *= 2
                                col.removeAt(i + 1)
                            }
                            i++
                        }
                        if (col.size != tempGrid.count { it[c] != 0 }) moved = true
                    }
                }
                Direction.DOWN -> {
                    for (c in 0 until gameState.gridSize) {
                        val col = mutableListOf<Int>()
                        for (r in gameState.gridSize - 1 downTo 0) if (tempGrid[r][c] != 0) col.add(tempGrid[r][c])
                        
                        var i = 0
                        while (i < col.size - 1) {
                            if (col[i] > 0 && col[i] == col[i + 1]) {
                                scoreGained += col[i] * 2
                                col[i] *= 2
                                col.removeAt(i + 1)
                            }
                            i++
                        }
                        if (col.size != tempGrid.count { it[c] != 0 }) moved = true
                    }
                }
                Direction.LEFT -> {
                    for (r in 0 until gameState.gridSize) {
                        val row = mutableListOf<Int>()
                        for (c in 0 until gameState.gridSize) if (tempGrid[r][c] != 0) row.add(tempGrid[r][c])
                        
                        var i = 0
                        while (i < row.size - 1) {
                            if (row[i] > 0 && row[i] == row[i + 1]) {
                                scoreGained += row[i] * 2
                                row[i] *= 2
                                row.removeAt(i + 1)
                            }
                            i++
                        }
                        if (row.size != tempGrid[r].count { it != 0 }) moved = true
                    }
                }
                Direction.RIGHT -> {
                    for (r in 0 until gameState.gridSize) {
                        val row = mutableListOf<Int>()
                        for (c in gameState.gridSize - 1 downTo 0) if (tempGrid[r][c] != 0) row.add(tempGrid[r][c])
                        
                        var i = 0
                        while (i < row.size - 1) {
                            if (row[i] > 0 && row[i] == row[i + 1]) {
                                scoreGained += row[i] * 2
                                row[i] *= 2
                                row.removeAt(i + 1)
                            }
                            i++
                        }
                        if (row.size != tempGrid[r].count { it != 0 }) moved = true
                    }
                }
            }

            if (moved && scoreGained > bestScore) {
                bestScore = scoreGained
                bestDirection = direction
            }
        }

        // If no immediate merge, just return the first valid move
        if (bestDirection == null) {
            for (direction in Direction.values()) {
                if (canMoveInDirection(grid, direction)) return direction
            }
        }
        
        return bestDirection
    }

    private fun canMoveInDirection(grid: List<List<Int>>, direction: Direction): Boolean {
        // Basic check if a move is possible in the given direction
        when (direction) {
            Direction.UP -> {
                for (c in 0 until gameState.gridSize) {
                    for (r in 1 until gameState.gridSize) {
                        if (grid[r][c] != 0 && (grid[r - 1][c] == 0 || grid[r - 1][c] == grid[r][c] && grid[r][c] > 0)) return true
                    }
                }
            }
            Direction.DOWN -> {
                for (c in 0 until gameState.gridSize) {
                    for (r in 0 until gameState.gridSize - 1) {
                        if (grid[r][c] != 0 && (grid[r + 1][c] == 0 || grid[r + 1][c] == grid[r][c] && grid[r][c] > 0)) return true
                    }
                }
            }
            Direction.LEFT -> {
                for (r in 0 until gameState.gridSize) {
                    for (c in 1 until gameState.gridSize) {
                        if (grid[r][c] != 0 && (grid[r][c - 1] == 0 || grid[r][c - 1] == grid[r][c] && grid[r][c] > 0)) return true
                    }
                }
            }
            Direction.RIGHT -> {
                for (r in 0 until gameState.gridSize) {
                    for (c in 0 until gameState.gridSize - 1) {
                        if (grid[r][c] != 0 && (grid[r][c + 1] == 0 || grid[r][c + 1] == grid[r][c] && grid[r][c] > 0)) return true
                    }
                }
            }
        }
        return false
    }
}
