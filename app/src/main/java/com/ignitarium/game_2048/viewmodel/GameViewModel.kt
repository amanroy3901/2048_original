package com.ignitarium.game_2048.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class TileAnimationInfo(
    val startPosition: Pair<Int, Int>? = null,
    val isNew: Boolean = false,
    val isMerged: Boolean = false
)

data class GameState(
    val grid: List<List<Int>> = List(4) { List(4) { 0 } },
    val score: Int = 0,
    val highScore: Int = 0,
    val playerName: String = "Player",
    val gridSize: Int = 4,
    val isGameOver: Boolean = false,
    val tileAnimationInfo: Map<Pair<Int, Int>, TileAnimationInfo> = mapOf(),
    val moveCount: Int = 0
)

class GameViewModel : ViewModel() {
    var gameState by mutableStateOf(GameState())
        private set

    fun updatePlayerName(name: String) {
        gameState = gameState.copy(playerName = name)
    }

    fun updateGridSize(size: Int) {
        gameState = gameState.copy(
            gridSize = size,
            grid = List(size) { List(size) { 0 } }
        )
        initializeGame()
    }

    fun initializeGame() {
        val newGrid = MutableList(gameState.gridSize) { MutableList(gameState.gridSize) { 0 } }
        // Add two initial tiles
        addRandomTile(newGrid)
        addRandomTile(newGrid)
        gameState = gameState.copy(
            grid = newGrid,
            score = 0,
            isGameOver = false
        )
    }

    private fun addRandomTile(grid: MutableList<MutableList<Int>>): Pair<Int, Int> {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (i in grid.indices) {
            for (j in grid[i].indices) {
                if (grid[i][j] == 0) {
                    emptyCells.add(Pair(i, j))
                }
            }
        }
        if (emptyCells.isNotEmpty()) {
            val (row, col) = emptyCells.random()
            grid[row][col] = if (Math.random() < 0.9) 2 else 4
            return Pair(row, col)
        }
        return Pair(-1, -1)
    }

    fun clearAnimationInfo() {
        gameState = gameState.copy(tileAnimationInfo = mapOf())
    }

    fun move(direction: Direction) {
        val newGrid = gameState.grid.map { it.toMutableList() }.toMutableList()
        var moved = false
        var score = gameState.score
        val animationInfo = mutableMapOf<Pair<Int, Int>, TileAnimationInfo>()

        when (direction) {
            Direction.UP -> {
                for (col in newGrid[0].indices) {
                    val result = mergeTiles((0 until gameState.gridSize).map { newGrid[it][col] })
                    if (result.first) {
                        moved = true
                        score += result.third
                        for (row in newGrid.indices) {
                            newGrid[row][col] = result.second[row]
                        }
                    }
                }
            }
            Direction.DOWN -> {
                for (col in newGrid[0].indices) {
                    val result = mergeTiles((gameState.gridSize - 1 downTo 0).map { newGrid[it][col] })
                    if (result.first) {
                        moved = true
                        score += result.third
                        for (row in newGrid.indices) {
                            newGrid[gameState.gridSize - 1 - row][col] = result.second[row]
                        }
                    }
                }
            }
            Direction.LEFT -> {
                for (row in newGrid.indices) {
                    val result = mergeTiles(newGrid[row])
                    if (result.first) {
                        moved = true
                        score += result.third
                        newGrid[row] = result.second.toMutableList()
                    }
                }
            }
            Direction.RIGHT -> {
                for (row in newGrid.indices) {
                    val result = mergeTiles(newGrid[row].reversed())
                    if (result.first) {
                        moved = true
                        score += result.third
                        newGrid[row] = result.second.reversed().toMutableList()
                    }
                }
            }
        }

        if (moved) {
            val (row, col) = addRandomTile(newGrid)
            if (row != -1 && col != -1) {
                animationInfo[Pair(row, col)] = TileAnimationInfo(isNew = true)
            }
            gameState = gameState.copy(
                grid = newGrid,
                score = score,
                highScore = maxOf(score, gameState.highScore),
                isGameOver = isGameOver(newGrid),
                tileAnimationInfo = animationInfo,
                moveCount = gameState.moveCount + 1
            )
        }
    }

    private fun mergeTiles(line: List<Int>): Triple<Boolean, List<Int>, Int> {
        val nonZeroTiles = line.filter { it != 0 }
        val merged = mutableListOf<Int>()
        var moved = false
        var score = 0
        var i = 0

        while (i < nonZeroTiles.size) {
            if (i + 1 < nonZeroTiles.size && nonZeroTiles[i] == nonZeroTiles[i + 1]) {
                merged.add(nonZeroTiles[i] * 2)
                score += nonZeroTiles[i] * 2
                moved = true
                i += 2
            } else {
                merged.add(nonZeroTiles[i])
                i++
            }
        }

        while (merged.size < line.size) {
            merged.add(0)
        }

        return Triple(moved || merged != line, merged, score)
    }

    private fun isGameOver(grid: List<List<Int>>): Boolean {
        // Check for empty cells
        if (grid.any { row -> row.any { it == 0 } }) return false

        // Check for possible merges
        for (i in grid.indices) {
            for (j in grid[i].indices) {
                val current = grid[i][j]
                // Check right
                if (j + 1 < grid[i].size && current == grid[i][j + 1]) return false
                // Check down
                if (i + 1 < grid.size && current == grid[i + 1][j]) return false
            }
        }
        return true
    }
}

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}