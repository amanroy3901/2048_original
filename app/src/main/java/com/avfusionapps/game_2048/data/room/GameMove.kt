package com.avfusionapps.game_2048.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a game move in the 2048 game.
 * Stores the grid state, score, and other relevant information for each move.
 */
@Entity(tableName = "game_moves")
data class GameMove(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val grid: List<List<Int>>,  // The grid state after this move
    val score: Int,             // The score after this move
    val moveNumber: Int,        // The sequential move number in the game
    val timestamp: Long = System.currentTimeMillis() // When this move was made
)