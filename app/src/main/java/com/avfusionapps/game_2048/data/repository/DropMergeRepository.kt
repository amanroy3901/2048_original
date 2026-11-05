package com.avfusionapps.game_2048.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dropMergeDataStore: DataStore<Preferences> by preferencesDataStore(name = "drop_merge")

/**
 * Persistence for the Neon Drop mode: best score, best tile ever built, games
 * played, and the player's shooter level and XP. Power-up charges are per game
 * and aren't stored; coins live in the shared game settings.
 */
class DropMergeRepository(private val context: Context) {

    companion object {
        val BEST_SCORE_KEY = intPreferencesKey("best_score")
        val BEST_TILE_KEY = intPreferencesKey("best_tile")
        val GAMES_PLAYED_KEY = intPreferencesKey("games_played")
        val LEVEL_KEY = intPreferencesKey("level")
        val XP_KEY = intPreferencesKey("xp")
    }

    /** Shooter level (starts at 1) and XP towards the next level. */
    val levelAndXp: Flow<Pair<Int, Int>> = context.dropMergeDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> (preferences[LEVEL_KEY] ?: 1) to (preferences[XP_KEY] ?: 0) }

    suspend fun saveLevel(level: Int, xp: Int) {
        context.dropMergeDataStore.edit { preferences ->
            preferences[LEVEL_KEY] = level
            preferences[XP_KEY] = xp
        }
    }

    val bestScore: Flow<Int> = context.dropMergeDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[BEST_SCORE_KEY] ?: 0 }

    val bestTile: Flow<Int> = context.dropMergeDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[BEST_TILE_KEY] ?: 0 }

    val gamesPlayed: Flow<Int> = context.dropMergeDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[GAMES_PLAYED_KEY] ?: 0 }

    suspend fun updateBests(score: Int, tile: Int) {
        context.dropMergeDataStore.edit { preferences ->
            val currentBestScore = preferences[BEST_SCORE_KEY] ?: 0
            if (score > currentBestScore) preferences[BEST_SCORE_KEY] = score
            val currentBestTile = preferences[BEST_TILE_KEY] ?: 0
            if (tile > currentBestTile) preferences[BEST_TILE_KEY] = tile
        }
    }

    suspend fun incrementGamesPlayed() {
        context.dropMergeDataStore.edit { preferences ->
            preferences[GAMES_PLAYED_KEY] = (preferences[GAMES_PLAYED_KEY] ?: 0) + 1
        }
    }
}
