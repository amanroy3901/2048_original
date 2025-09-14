package com.avfusionapps.game_2048.data.room

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing game move data.
 * Provides a clean API for the ViewModel to interact with the Room database.
 */
class GameMoveRepository(private val context: Context) {
    
    private val gameMoveDao = GameDatabase.getInstance(context).gameMoveDao()
    
    /**
     * Save a game move to the database.
     * @param grid The current grid state
     * @param score The current score
     * @param moveNumber The current move number
     * @return The ID of the inserted move
     */
    suspend fun saveMove(grid: List<List<Int>>, score: Int, moveNumber: Int): Long {
        val gameMove = GameMove(
            grid = grid,
            score = score,
            moveNumber = moveNumber
        )
        return gameMoveDao.insertMove(gameMove)
    }
    
    /**
     * Get the most recent move.
     * @return The most recent GameMove or null if no moves exist
     */
    suspend fun getLastMove(): GameMove? {
        return gameMoveDao.getLastMove()
    }
    
    /**
     * Get the last N moves in descending order (most recent first).
     * @param limit The maximum number of moves to retrieve
     * @return A list of the most recent GameMove objects
     */
    suspend fun getLastMoves(limit: Int): List<GameMove> {
        return gameMoveDao.getLastMoves(limit)
    }
    
    /**
     * Delete all moves from the database.
     */
    suspend fun clearAllMoves() {
        gameMoveDao.deleteAllMoves()
    }
    
    /**
     * Keep only the most recent N moves and delete the rest.
     * @param keepCount The number of most recent moves to keep
     */
    suspend fun keepOnlyLastMoves(keepCount: Int) {
        gameMoveDao.keepOnlyLastMoves(keepCount)
    }
    
    /**
     * Get the total number of moves stored in the database.
     * @return The count of moves
     */
    suspend fun getMoveCount(): Int {
        return gameMoveDao.getMoveCount()
    }
    
    /**
     * Observe the most recent move as a Flow.
     * @return A Flow emitting the most recent GameMove whenever it changes
     */
    fun observeLastMove(): Flow<GameMove?> {
        return gameMoveDao.observeLastMove()
    }
}