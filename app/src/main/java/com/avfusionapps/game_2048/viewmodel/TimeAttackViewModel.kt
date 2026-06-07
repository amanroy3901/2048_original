package com.avfusionapps.game_2048.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avfusionapps.game_2048.data.repository.TimeAttackRepository
import com.avfusionapps.game_2048.model.BonusType
import com.avfusionapps.game_2048.model.TimeAttackScore
import com.avfusionapps.game_2048.model.TimeAttackState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

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
        
        val (newGrid, scoreGained, mergedTiles) = moveGrid(currentGrid, direction)

        if (newGrid != currentGrid) {
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
            val gridWithNewTile = addRandomTile(newGrid)

            _gameState.value = currentState.copy(
                grid = gridWithNewTile,
                score = currentState.score + finalScoreGained,
                timeRemainingMillis = newTime,
                multiplier = min(5.0f, multiplier), // Cap at 5x
                lastBonus = lastBonus
            )
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
            val (newLine, lineScore, lineMerged) = processLine(currentLine)
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

    private fun processLine(line: List<Int>): Triple<List<Int>, Int, List<Int>> {
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
}
