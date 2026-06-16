package com.avfusionapps.game_2048.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avfusionapps.game_2048.data.repository.TimeAttackRepository
import com.avfusionapps.game_2048.model.BonusType
import com.avfusionapps.game_2048.model.TimeAttackScore
import com.avfusionapps.game_2048.model.TimeAttackState
import com.avfusionapps.game_2048.model.TileAnimationInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

private data class TimeAttackProcessedTile(
    val value: Int,
    val originalIndex: Int,
    val mergedFromIndex: Int? = null
)

class TimeAttackViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TimeAttackRepository(application.applicationContext)

    private val _gameState = MutableStateFlow(TimeAttackState())
    val gameState: StateFlow<TimeAttackState> = _gameState.asStateFlow()

    private var timerJob: Job? = null
    private val initialTimeMillis = 60_000L // 60 seconds
    private val maxTimeMillis = 120_000L // Cap at 2 minutes

    val highScore = repository.highScore
    val bestTimeSurvived = repository.bestTimeSurvived
    val topScores = repository.topScores

    init {
        startNewGame()
    }

    fun startNewGame() {
        timerJob?.cancel()
        val newGrid = createEmptyGrid()
        _gameState.value = TimeAttackState(
            grid = addRandomTile(addRandomTile(newGrid)),
            score = 0,
            timeRemainingMillis = initialTimeMillis,
            isGameOver = false,
            isPaused = false,
            multiplier = 1.0f,
            lastBonus = null
        )
        startTimer()
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (_gameState.value.timeRemainingMillis > 0 && !_gameState.value.isGameOver) {
                delay(100) // Update every 100ms for smooth countdown

                if (!_gameState.value.isPaused) {
                    val newTime = _gameState.value.timeRemainingMillis - 100
                    _gameState.value = _gameState.value.copy(timeRemainingMillis = max(0, newTime))

                    if (newTime <= 0) {
                        endGame()
                    }
                }
            }
        }
    }

    fun onSwipe(direction: Direction) {
        if (_gameState.value.isGameOver || _gameState.value.isPaused) return

        val currentState = _gameState.value
        val currentGrid = currentState.grid
        val size = currentGrid.size
        val newGrid = MutableList(size) { MutableList(size) { 0 } }
        var boardMoved = false
        var scoreGained = 0
        val mergedTiles = mutableListOf<Int>()
        val animationInfoMap = mutableMapOf<Pair<Int, Int>, TileAnimationInfo>()

        for (i in 0 until size) {
            val currentLine = getLine(currentGrid, i, direction)
            val (processedLine, lineScore, lineMoved) = processLine(currentLine)

            if (lineMoved) boardMoved = true
            scoreGained += lineScore

            processedLine.forEachIndexed { resultIndex, processedTile ->
                if (processedTile.value > 0) {
                    val finalPos = getFinalPosition(i, resultIndex, direction, size)
                    newGrid[finalPos.first][finalPos.second] = processedTile.value
                    
                    val originalPos = getFinalPosition(i, processedTile.originalIndex, direction, size)
                    val positionChanged = finalPos != originalPos
                    val didMerge = processedTile.mergedFromIndex != null

                    if (didMerge) {
                        mergedTiles.add(processedTile.value)
                    }

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
                }
            }
        }

        if (boardMoved) {
            var timeBonus = 0L
            var multiplier = currentState.multiplier
            var lastBonus: BonusType? = null

            // Calculate bonuses for merged tiles
            mergedTiles.forEach { tileValue ->
                // Base merge bonus
                timeBonus += BonusType.MergeBonus.timeBonusMillis

                // Bonus based on tile value
                val tileBonus = when (tileValue) {
                    128 -> BonusType.Tile128Bonus
                    256 -> BonusType.Tile256Bonus
                    512 -> BonusType.Tile512Bonus
                    1024 -> BonusType.Tile1024Bonus
                    2048 -> BonusType.Tile2048Bonus
                    else -> null
                }

                tileBonus?.let {
                    timeBonus += it.timeBonusMillis
                    lastBonus = it
                }

                // Increase multiplier
                multiplier += (tileValue / 1000f) * 0.1f
            }

            // Apply time bonus with cap
            val newTime = min(maxTimeMillis, currentState.timeRemainingMillis + timeBonus)

            // Calculate final score with multiplier
            val finalScoreGained = (scoreGained * multiplier).toInt()

            // Add new random tile to grid
            val gridWithNewTile = addRandomTile(newGrid.map { it.toList() })
            
            // Find where the new tile was placed
            var newTileRow = -1
            var newTileCol = -1
            for (r in 0 until size) {
                for (c in 0 until size) {
                    if (newGrid[r][c] == 0 && gridWithNewTile[r][c] != 0) {
                        newTileRow = r
                        newTileCol = c
                        break
                    }
                }
                if (newTileRow != -1) break
            }
            
            if (newTileRow != -1) {
                animationInfoMap[Pair(newTileRow, newTileCol)] = TileAnimationInfo(isNew = true)
            }

            val gameOver = isGameOver(gridWithNewTile)

            _gameState.value = currentState.copy(
                grid = gridWithNewTile,
                previousGrid = currentGrid,
                previousScore = currentState.score,
                score = currentState.score + finalScoreGained,
                timeRemainingMillis = newTime,
                multiplier = min(5.0f, multiplier), // Cap at 5x
                lastBonus = lastBonus,
                isGameOver = gameOver,
                tileAnimationInfo = animationInfoMap,
                moveCount = currentState.moveCount + 1
            )

            if (gameOver) {
                endGame()
            }
        } else {
            if (isGameOver(currentGrid)) {
                endGame()
            }
        }
    }

    fun clearAnimationInfo() {
        if (_gameState.value.tileAnimationInfo.isNotEmpty()) {
            _gameState.value = _gameState.value.copy(tileAnimationInfo = emptyMap())
        }
    }

    fun togglePause() {
        _gameState.value = _gameState.value.copy(isPaused = !_gameState.value.isPaused)
    }

    private fun endGame() {
        timerJob?.cancel()
        _gameState.value = _gameState.value.copy(isGameOver = true)

        // Save score to repository
        viewModelScope.launch {
            repository.saveTimeAttackHighScore(
                TimeAttackScore(
                    score = _gameState.value.score,
                    timeSurvived = initialTimeMillis - _gameState.value.timeRemainingMillis
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    // Game logic helper functions - matching GameViewModel's implementation
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

    private fun createEmptyGrid(size: Int = 4): List<List<Int>> {
        return List(size) { List(size) { 0 } }
    }

    private fun addRandomTile(grid: List<List<Int>>): List<List<Int>> {
        val mutableGrid = grid.map { it.toMutableList() }.toMutableList()
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        mutableGrid.forEachIndexed { r, row ->
            row.forEachIndexed { c, value -> if (value == 0) emptyCells.add(r to c) }
        }
        return if (emptyCells.isNotEmpty()) {
            val (row, col) = emptyCells.random(Random)
            mutableGrid[row][col] = if (Math.random() < 0.9) 2 else 4
            mutableGrid
        } else {
            grid // Grid full
        }
    }

    private fun moveGrid(
        grid: List<List<Int>>,
        direction: Direction
    ): Triple<List<List<Int>>, Int, List<Int>> {
        val size = grid.size
        val mutableGrid = MutableList(size) { MutableList(size) { 0 } }
        var score = 0
        val mergedTiles = mutableListOf<Int>()

        for (i in 0 until size) {
            val currentLine = getLine(grid, i, direction)
            val (newLine, lineScore, lineMerged) = processLineSimple(currentLine)
            score += lineScore
            mergedTiles.addAll(lineMerged)
            applyLine(mutableGrid, newLine, i, direction)
        }

        val newGrid = mutableGrid.map { it.toList() }
        
        // Check if grid actually changed
        val gridChanged = grid != newGrid
        
        return Triple(newGrid, score, mergedTiles)
    }

    private fun getLine(grid: List<List<Int>>, index: Int, direction: Direction): List<Int> {
        val size = grid.size
        return when (direction) {
            Direction.UP -> (0 until size).map { grid[it][index] }
            Direction.DOWN -> (size - 1 downTo 0).map { grid[it][index] }
            Direction.LEFT -> grid[index]
            Direction.RIGHT -> grid[index].reversed()
        }
    }

    private fun applyLine(grid: MutableList<MutableList<Int>>, line: List<Int>, index: Int, direction: Direction) {
        val size = grid.size
        when (direction) {
            Direction.UP -> line.forEachIndexed { i, v -> grid[i][index] = v }
            Direction.DOWN -> line.forEachIndexed { i, v -> grid[size - 1 - i][index] = v }
            Direction.LEFT -> grid[index] = line.toMutableList()
            Direction.RIGHT -> grid[index] = line.reversed().toMutableList()
        }
    }

    private fun processLineSimple(line: List<Int>): Triple<List<Int>, Int, List<Int>> {
        val nonZero = line.filter { it != 0 }
        val result = mutableListOf<Int>()
        var score = 0
        val merged = mutableListOf<Int>()
        var i = 0

        while (i < nonZero.size) {
            if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
                val mergedValue = nonZero[i] * 2
                result.add(mergedValue)
                score += mergedValue
                merged.add(mergedValue)
                i += 2
            } else {
                result.add(nonZero[i])
                i++
            }
        }

        while (result.size < line.size) {
            result.add(0)
        }

        return Triple(result, score, merged)
    }

    private fun processLine(line: List<Int>): Triple<List<TimeAttackProcessedTile>, Int, Boolean> {
        val indexedNonZero = line.mapIndexedNotNull { index, value -> if (value != 0) index to value else null }

        if (indexedNonZero.isEmpty()) {
            val resultList = List(line.size) { TimeAttackProcessedTile(0, -1) }
            return Triple(resultList, 0, false)
        }

        val result = mutableListOf<TimeAttackProcessedTile>()
        var score = 0
        var i = 0
        var lineChanged = false

        while (i < indexedNonZero.size) {
            val (originalIndex1, value1) = indexedNonZero[i]

            if (i + 1 < indexedNonZero.size) {
                val (originalIndex2, value2) = indexedNonZero[i + 1]
                if (value1 == value2) {
                    val mergedValue = value1 * 2
                    result.add(
                        TimeAttackProcessedTile(
                            value = mergedValue,
                            originalIndex = originalIndex1,
                            mergedFromIndex = originalIndex2
                        )
                    )
                    score += mergedValue
                    lineChanged = true
                    i += 2
                    continue
                }
            }

            result.add(TimeAttackProcessedTile(value = value1, originalIndex = originalIndex1))
            if (result.size - 1 != i) {
                lineChanged = true
            }
            i++
        }

        result.forEachIndexed { finalLineIndex, processedTile ->
            if (processedTile.originalIndex != finalLineIndex) {
                lineChanged = true
            }
        }

        val finalResultList = mutableListOf<TimeAttackProcessedTile>()
        finalResultList.addAll(result)
        while (finalResultList.size < line.size) {
            finalResultList.add(TimeAttackProcessedTile(0, -1))
        }

        return Triple(finalResultList, score, lineChanged)
    }

    private fun getFinalPosition(
        lineIndex: Int,
        resultIndex: Int,
        direction: Direction,
        size: Int
    ): Pair<Int, Int> {
        return when (direction) {
            Direction.UP -> Pair(resultIndex, lineIndex)
            Direction.DOWN -> Pair(size - 1 - resultIndex, lineIndex)
            Direction.LEFT -> Pair(lineIndex, resultIndex)
            Direction.RIGHT -> Pair(lineIndex, size - 1 - resultIndex)
        }
    }

    fun undoMove() {
        // Time Attack is fast-paced, undo might not be heavily used, but we should implement basic undo if needed.
        // For simplicity and performance in Time Attack, we can restrict undo to just 1 previous state or disable it.
        // Let's implement a single-step undo.
        _gameState.value.previousGrid?.let { prevGrid ->
            _gameState.value = _gameState.value.copy(
                grid = prevGrid,
                score = _gameState.value.previousScore,
                previousGrid = null // Can only undo once
            )
        }
    }

    fun showHint(context: Context) {
        val bestMove = findBestMove(_gameState.value.grid)
        if (bestMove != null) {
            Toast.makeText(context, "Try swiping ${bestMove.name}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No obvious good moves!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findBestMove(grid: List<List<Int>>): Direction? {
        var bestScore = -1
        var bestDirection: Direction? = null

        for (direction in Direction.values()) {
            val size = grid.size
            val tempGrid = MutableList(size) { MutableList(size) { 0 } }
            var scoreGained = 0
            var moved = false

            for (i in 0 until size) {
                val currentLine = getLine(grid, i, direction)
                val (newLine, lineScore, _) = processLineSimple(currentLine)
                if (currentLine != newLine) moved = true
                scoreGained += lineScore
                applyLine(tempGrid, newLine, i, direction)
            }

            if (moved && scoreGained > bestScore) {
                bestScore = scoreGained
                bestDirection = direction
            }
        }

        if (bestDirection == null) {
            for (direction in Direction.values()) {
                val size = grid.size
                for (i in 0 until size) {
                    val currentLine = getLine(grid, i, direction)
                    val (newLine, _, _) = processLineSimple(currentLine)
                    if (currentLine != newLine) return direction
                }
            }
        }
        
        return bestDirection
    }
}
