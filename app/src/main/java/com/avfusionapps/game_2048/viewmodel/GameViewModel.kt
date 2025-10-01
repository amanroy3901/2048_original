package com.avfusionapps.game_2048.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avfusionapps.game_2048.data.GameSettingsRepository
import com.avfusionapps.game_2048.data.room.GameMoveRepository
import com.avfusionapps.game_2048.notification.ReminderManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Represents the animation state for a tile after a move.
 * @param startPosition The grid coordinates (row, col) where the tile originated *before* this move. Null if it didn't move.
 * @param isNew True if this tile was newly added in this move.
 * @param isMerged True if this tile is the result of a merge in this move.
 */
data class TileAnimationInfo(
    val startPosition: Pair<Int, Int>? = null,
    val isNew: Boolean = false,
    val isMerged: Boolean = false
)

/**
 * Represents the overall state of the game, including UI elements and logic state.
 * @param grid The current 2D list representing the game board tiles.
 * @param score The score for the current game session.
 * @param highScore The highest score achieved (either current session or loaded persistent).
 * @param playerName The current player's name.
 * @param gridSize The dimensions of the game grid (e.g., 4 for 4x4).
 * @param isGameOver True if no more valid moves can be made.
 * @param tileAnimationInfo A map where the key is the *final* grid position (row, col) of a tile,
 *                          and the value contains information about how it arrived there (moved from, new, merged).
 * @param moveCount Counter for the number of moves made.
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
    val hasSavedGame: Boolean = false
)

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

class GameViewModel(application: Application) : AndroidViewModel(application) {

    override fun onCleared() {
        super.onCleared()
        saveCurrentGameState()
    }

    /**
     * Saves the current game state to the database.
     * Called when the app is paused or the ViewModel is cleared.
     */
    fun saveCurrentGameState() {
        // Only save if there's an active game (not game over and has moves)
        if (!gameState.isGameOver && gameState.moveCount > 0) {
            viewModelScope.launch {
                val moveId = gameMoveRepository.saveMove(
                    gameState.grid,
                    gameState.score,
                    gameState.moveCount
                )
                println("Saved game state on pause: Move #${gameState.moveCount}, Grid size: ${gameState.gridSize}")

                updateGameState(gameState.copy(hasSavedGame = true))
            }
        }
    }

    private val settingsRepository = GameSettingsRepository(application)
    private val reminderManager = ReminderManager(application)
    private val gameMoveRepository = GameMoveRepository(application)


    var gameState by mutableStateOf(GameState())
        private set

    private val _gameStateFlow = MutableStateFlow(GameState())
    val gameStateFlow: StateFlow<GameState> = _gameStateFlow.asStateFlow()

    private val _resumePrompt = MutableStateFlow(false)
    val resumePrompt: StateFlow<Boolean> = _resumePrompt.asStateFlow()

    private fun updateGameState(newState: GameState) {
        gameState = newState
        _gameStateFlow.value = newState
    }

    private var _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    val persistentPlayerName: StateFlow<String> = settingsRepository.playerNameFlow
        .stateIn(
            scope = viewModelScope, // Scope tied to ViewModel lifecycle
            started = SharingStarted.WhileSubscribed(5000), // Keep flow active while UI observes
            initialValue = GameSettingsRepository.DEFAULT_PLAYER_NAME // Provide initial default
        )

    val persistentHighScore: StateFlow<Int> = settingsRepository.highScoreFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GameSettingsRepository.DEFAULT_HIGH_SCORE
        )

    private val _mergeEvent = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val mergeEvent: SharedFlow<Unit> = _mergeEvent.asSharedFlow()

    val hasSavedGameFlow: StateFlow<Boolean> = gameStateFlow
        .map { it.hasSavedGame }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            val initialName = persistentPlayerName.first()
            val initialHighScore = persistentHighScore.first()

            val lastMove = gameMoveRepository.getLastMove()

            if (lastMove != null) {
                updateGameState(
                    gameState.copy(
                        playerName = initialName,
                        highScore = initialHighScore,
                        grid = List(gameState.gridSize) { List(gameState.gridSize) { 0 } },
                        hasSavedGame = true
                    )
                )
                _resumePrompt.value = true
            } else {
                updateGameState(
                    gameState.copy(
                    playerName = initialName,
                    highScore = initialHighScore,
                    grid = List(gameState.gridSize) { List(gameState.gridSize) { 0 } }
                ))
                if (gameState.grid.all { row -> row.all { it == 0 } }) {
                    initializeGame()
                }
            }
            updateCanUndoState()
        }
    }

    fun markResumableWithoutMove() {
        val hasTiles = gameState.grid.any { row -> row.any { it != 0 } }
        if (hasTiles && !gameState.isGameOver) {
            updateGameState(gameState.copy(hasSavedGame = true))
            viewModelScope.launch {
                gameMoveRepository.saveMove(
                    gameState.grid,
                    gameState.score,
                    gameState.moveCount
                )
                updateCanUndoState()
            }
        }
    }

    /** Updates the player name in local state and triggers persistent save. */
    fun updatePlayerName(name: String) {
        val validName = name.ifBlank { GameSettingsRepository.DEFAULT_PLAYER_NAME }
        updateGameState(gameState.copy(playerName = validName)) // Update UI state immediately
        viewModelScope.launch { settingsRepository.updatePlayerName(validName) } // Save in background
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
                hasSavedGame = false
            )
        )

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
                        hasSavedGame = false
                    )
                )

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
        if (gameState.isGameOver) return

        val currentGrid = gameState.grid
        val size = gameState.gridSize
        val newGrid = MutableList(size) { MutableList(size) { 0 } }
        var boardMoved = false
        var scoreIncreaseThisTurn = 0
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
                        animationInfoMap[finalPos] = TileAnimationInfo(
                            startPosition = originalPos,
                            isMerged = didMerge,
                            isNew = false
                        )
                    }

                    if (didMerge) {
                        _mergeEvent.tryEmit(Unit)
                    }
                }
            }
        }


        if (boardMoved) {
            val (newTileRow, newTileCol) = addRandomTile(newGrid)
            if (newTileRow != -1) {
                animationInfoMap[Pair(newTileRow, newTileCol)] = TileAnimationInfo(isNew = true)
            }

            val newScore = gameState.score + scoreIncreaseThisTurn
            val currentPersistentHighScore =
                persistentHighScore.value
            if (newScore > currentPersistentHighScore) {
                viewModelScope.launch {
                    settingsRepository.updateHighScoreIfHigher(newScore)
                }
            }
            val newLocalHighScore = maxOf(newScore, gameState.highScore)

            val gameOver = isGameOver(newGrid)

            val newMoveCount = gameState.moveCount + 1
            updateGameState(
                gameState.copy(
                    grid = newGrid,
                    score = newScore,
                    highScore = newLocalHighScore,
                    isGameOver = gameOver,
                    tileAnimationInfo = animationInfoMap,
                    moveCount = newMoveCount
                )
            )

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
            }
        }
    }

    fun enableNotification() {
        reminderManager.scheduleReminders()
    }

    /**
     * Undoes the last move by restoring the previous game state from the database.
     * @return True if the undo was successful, false if there are no moves to undo.
     */
    fun undoMove(context: Context) {
        viewModelScope.launch {
            val moves = gameMoveRepository.getLastMoves(2)

            if (moves.size >= 2) {
                val latest = moves[0]
                val previousMove = moves[1]

                Log.d("VIVEK", "undoMove: $latest")
                Log.d("VIVEK", "undoMove: $previousMove")

                val restoredGrid: List<List<Int>> = previousMove.grid.map { it.toList() }

                updateGameState(
                    gameState.copy(
                        grid = restoredGrid,
                        score = previousMove.score,
                        moveCount = previousMove.moveNumber,
                        tileAnimationInfo = emptyMap(),
                        isGameOver = false
                    )
                )

                gameMoveRepository.deleteMoveByNumber(latest.moveNumber)
                updateCanUndoState()
            } else {
                Toast.makeText(context, "At least 2 move is required to use Undo", Toast.LENGTH_SHORT).show()
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

                updateGameState(
                    gameState.copy(
                        grid = lastMove.grid,
                        gridSize = gridSize,
                        score = lastMove.score,
                        moveCount = lastMove.moveNumber,
                        tileAnimationInfo = emptyMap(),
                        isGameOver = false,
                        hasSavedGame = false
                    )
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

    /**
     * Updates the canUndo state based on available moves in the database.
     */
    private suspend fun updateCanUndoState() {
        val moveCount = gameMoveRepository.getMoveCount()
        _canUndo.value = moveCount > 1 // Need at least 2 moves to undo (current + previous)
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
                if (value1 == value2) {
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
            grid[row][col] = if (Math.random() < 0.9) 2 else 4 // 90% chance of 2
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
                if (c + 1 < size && current == grid[r][c + 1]) return false
                // Check bottom neighbor
                if (r + 1 < size && current == grid[r + 1][c]) return false
            }
        }
        return true // No empty cells and no possible merges
    }
}