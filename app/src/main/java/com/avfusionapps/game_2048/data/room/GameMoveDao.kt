package com.avfusionapps.game_2048.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the GameMove entity.
 * Provides methods to interact with the game_moves table in the database.
 */
@Dao
interface GameMoveDao {
    /**
     * Insert a new game move into the database.
     * @param gameMove The move to insert
     * @return The ID of the inserted move
     */
    @Insert
    suspend fun insertMove(gameMove: GameMove): Long
    
    /**
     * Get the most recent move for the current game.
     * @return The most recent GameMove or null if no moves exist
     */
    @Query("SELECT * FROM game_moves ORDER BY moveNumber DESC LIMIT 1")
    suspend fun getLastMove(): GameMove?
    
    /**
     * Get the last N moves in descending order (most recent first).
     * @param limit The maximum number of moves to retrieve
     * @return A list of the most recent GameMove objects
     */
    @Query("SELECT * FROM game_moves ORDER BY moveNumber DESC LIMIT :limit")
    suspend fun getLastMoves(limit: Int): List<GameMove>
    
    /**
     * Delete all moves from the database.
     */
    @Query("DELETE FROM game_moves")
    suspend fun deleteAllMoves()
    
    /**
     * Delete all moves except the most recent N moves.
     * @param keepCount The number of most recent moves to keep
     */
    @Query("DELETE FROM game_moves WHERE id NOT IN (SELECT id FROM game_moves ORDER BY moveNumber DESC LIMIT :keepCount)")
    suspend fun keepOnlyLastMoves(keepCount: Int)
    
    /**
     * Get the total number of moves stored in the database.
     * @return The count of moves
     */
    @Query("SELECT COUNT(*) FROM game_moves")
    suspend fun getMoveCount(): Int
    
    /**
     * Observe the most recent move as a Flow.
     * @return A Flow emitting the most recent GameMove whenever it changes
     */
    @Query("SELECT * FROM game_moves ORDER BY moveNumber DESC LIMIT 1")
    fun observeLastMove(): Flow<GameMove?>

    @Query("DELETE FROM game_moves WHERE moveNumber = :moveNumber")
    suspend fun deleteMoveByNumber(moveNumber: Int)
}