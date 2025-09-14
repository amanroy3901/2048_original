package com.avfusionapps.game_2048.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room database for the 2048 game.
 * Stores game moves for undo functionality and game resumption.
 */
@Database(entities = [GameMove::class], version = 1, exportSchema = false)
@TypeConverters(GameMoveConverters::class)
abstract class GameDatabase : RoomDatabase() {
    
    /**
     * Get the DAO for game moves.
     */
    abstract fun gameMoveDao(): GameMoveDao
    
    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null
        
        /**
         * Get the singleton instance of the database.
         * @param context The application context
         * @return The database instance
         */
        fun getInstance(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "game_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}