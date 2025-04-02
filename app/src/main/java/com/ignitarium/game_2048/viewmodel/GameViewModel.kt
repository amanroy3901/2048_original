package com.ignitarium.game_2048.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ignitarium.game_2048.data.GameSettingsRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
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
    val moveCount: Int = 0
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

// ViewModel extending AndroidViewModel to get Application context for Repository
class GameViewModel(application: Application) : AndroidViewModel(application) {

    // Repository for handling persistent data storage (name, high score)
    private val settingsRepository = GameSettingsRepository(application)

    // --- State Management ---

    // The primary mutable state holder for the UI, observed by Composables.
    var gameState by mutableStateOf(GameState())
        private set // Restrict direct mutation from outside the ViewModel

    // StateFlow representing the PERSISTENT player name from DataStore, shared with UI.
    val persistentPlayerName: StateFlow<String> = settingsRepository.playerNameFlow
        .stateIn(
            scope = viewModelScope, // Scope tied to ViewModel lifecycle
            started = SharingStarted.WhileSubscribed(5000), // Keep flow active while UI observes
            initialValue = GameSettingsRepository.DEFAULT_PLAYER_NAME // Provide initial default
        )

    // StateFlow representing the PERSISTENT high score from DataStore, shared with UI.
    val persistentHighScore: StateFlow<Int> = settingsRepository.highScoreFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GameSettingsRepository.DEFAULT_HIGH_SCORE
        )

    // --- Haptic Feedback Signal ---
    // A SharedFlow to emit events to the UI when a merge occurs, triggering haptics.
    private val _mergeEvent = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val mergeEvent: SharedFlow<Unit> = _mergeEvent.asSharedFlow()

    // --- Initialization ---
    init {
        // Load persistent values and set up the initial game state when ViewModel is created.
        viewModelScope.launch {
            val initialName = persistentPlayerName.first() // Get stored name
            val initialHighScore = persistentHighScore.first() // Get stored high score
            gameState = gameState.copy(
                playerName = initialName,
                highScore = initialHighScore,
                // Initialize grid based on default size (or potentially saved size later)
                grid = List(gameState.gridSize) { List(gameState.gridSize) { 0 } }
            )
            // If the grid is empty (e.g., first launch), add initial tiles.
            if (gameState.grid.all { row -> row.all { it == 0 } }) {
                initializeGame()
            }
        }
    }

    // --- Public Functions Called by UI ---

    /** Updates the player name in local state and triggers persistent save. */
    fun updatePlayerName(name: String) {
        val validName = name.ifBlank { GameSettingsRepository.DEFAULT_PLAYER_NAME }
        gameState = gameState.copy(playerName = validName) // Update UI state immediately
        viewModelScope.launch { settingsRepository.updatePlayerName(validName) } // Save in background
    }

    /** Sets a new grid size, resets the game board, and initializes it. */
    fun updateGridSize(size: Int) {
        if (size >= 3 && size != gameState.gridSize) { // Basic validation and check if size changed
            gameState = gameState.copy(
                gridSize = size,
                score = 0, // Reset score for new size
                isGameOver = false,
                moveCount = 0,
                tileAnimationInfo = emptyMap(),
                grid = List(size) { List(size) { 0 } } // Create new empty grid
            )
            initializeGame() // Add starting tiles to the new grid
        } else if (size == gameState.gridSize) {
            // If size is the same, just start a new game on the current grid size
            initializeGame()
        }
    }

    /** Resets the game board (adds initial tiles), score, and move count. Keeps name/high score. */
    fun initializeGame() {
        val size = gameState.gridSize
        val newGrid = MutableList(size) { MutableList(size) { 0 } }
        addRandomTile(newGrid) // Add first tile
        addRandomTile(newGrid) // Add second tile
        gameState = gameState.copy(
            grid = newGrid,
            score = 0,
            isGameOver = false,
            moveCount = 0,
            tileAnimationInfo = emptyMap() // Clear previous animations
            // Note: Keeps existing playerName and highScore from gameState
        )
    }

    /**
     * Processes a player move in the specified direction.
     * Calculates new tile positions, merges, score, and animation information.
     * Updates the gameState.
     */
    fun move(direction: Direction) {
        if (gameState.isGameOver) return // Ignore moves if game is over

        val currentGrid = gameState.grid
        val size = gameState.gridSize
        // Create a new grid initialized to zeros. We'll populate it based on moves.
        val newGrid = MutableList(size) { MutableList(size) { 0 } }
        var boardMoved = false // Flag if any tile moved/merged on the entire board
        var scoreIncreaseThisTurn = 0
        // Map to store animation info: Key = final position (r,c), Value = Animation details
        val animationInfoMap = mutableMapOf<Pair<Int, Int>, TileAnimationInfo>()

        // Process the grid line by line (row or column based on direction)
        for (i in 0 until size) { // i = row index for LEFT/RIGHT, column index for UP/DOWN
            val currentLine = getLine(currentGrid, i, direction) // Extract the line to process
            val (processedLine, lineScore, lineMoved) = processLine(currentLine) // Process merges/slides

            if (lineMoved) boardMoved = true // Mark if any line had changes
            scoreIncreaseThisTurn += lineScore // Accumulate score

            // Place the results back onto the newGrid and record animation origins
            processedLine.forEachIndexed { resultIndex, processedTile ->
                if (processedTile.value > 0) { // Only process tiles with values
                    // Calculate the final grid position for this tile
                    val finalPos = getFinalPosition(i, resultIndex, direction, size)
                    newGrid[finalPos.first][finalPos.second] = processedTile.value // Place tile in new grid

                    // Calculate where this tile originated in grid coordinates
                    val originalPos = getFinalPosition(i, processedTile.originalIndex, direction, size)

                    // Determine if the tile physically moved to a different grid cell
                    val positionChanged = finalPos != originalPos
                    // Determine if this tile resulted from a merge
                    val didMerge = processedTile.mergedFromIndex != null

                    // If the tile moved or merged, record its starting position for animation
                    if (positionChanged || didMerge) {
                        animationInfoMap[finalPos] = TileAnimationInfo(
                            startPosition = originalPos,
                            isMerged = didMerge,
                            isNew = false // It came from an existing tile, so not new
                        )
                    }

                    // If a merge happened, emit event for haptic feedback
                    if (didMerge) {
                        _mergeEvent.tryEmit(Unit)
                    }
                }
            }
        } // End of line processing loop

        // --- Post-Move Actions ---

        // If any tile moved or merged on the board
        if (boardMoved) {
            // Add a new random tile to an empty spot in the just-calculated newGrid
            val (newTileRow, newTileCol) = addRandomTile(newGrid)
            if (newTileRow != -1) { // Check if a tile was successfully added
                // Record animation info for the newly added tile
                // This overwrites any previous move/merge info if the new tile landed there (shouldn't happen)
                animationInfoMap[Pair(newTileRow, newTileCol)] = TileAnimationInfo(isNew = true)
            }

            // --- Update Score & High Score ---
            val newScore = gameState.score + scoreIncreaseThisTurn
            val currentPersistentHighScore = persistentHighScore.value // Get latest saved high score
            // Check if the persistent high score needs updating
            if (newScore > currentPersistentHighScore) {
                viewModelScope.launch {
                    settingsRepository.updateHighScoreIfHigher(newScore)
                }
            }
            // Update the highScore displayed in the current game session immediately
            val newLocalHighScore = maxOf(newScore, gameState.highScore)

            // --- Check Game Over ---
            // Check *after* adding the new tile, using the final newGrid state
            val gameOver = isGameOver(newGrid)

            // --- Update Game State ---
            // Commit all changes to the central gameState object
            gameState = gameState.copy(
                grid = newGrid, // The final grid state after moves and new tile
                score = newScore,
                highScore = newLocalHighScore, // Reflect the best score seen so far
                isGameOver = gameOver,
                tileAnimationInfo = animationInfoMap, // Pass the collected animation data
                moveCount = gameState.moveCount + 1 // Increment move counter
            )

            if (gameOver) { println("Game Over! Final Score: $newScore") }

        } else {
            // If no tiles moved at all, check if the board is full and stuck (Game Over)
            if (isGameOver(gameState.grid)) { // Check the current grid
                gameState = gameState.copy(isGameOver = true)
                println("Game Over! (No valid moves left)")
            }
        }
    }

    /** Clears the animation information map, typically called by UI after animations complete. */
    fun clearAnimationInfo() {
        // Avoid unnecessary state updates if map is already empty
        if (gameState.tileAnimationInfo.isNotEmpty()) {
            gameState = gameState.copy(tileAnimationInfo = emptyMap())
        }
    }

    // --- Private Helper Functions ---

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
    private fun getFinalPosition(lineIndex: Int, resultIndex: Int, direction: Direction, size: Int): Pair<Int, Int> {
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
        val indexedNonZero = line.mapIndexedNotNull { index, value -> if (value != 0) index to value else null }

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
                    result.add(ProcessedTile(value = mergedValue, originalIndex = originalIndex1, mergedFromIndex = originalIndex2))
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
            if(processedTile.originalIndex != finalLineIndex) {
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