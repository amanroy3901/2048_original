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

private val Context.fallMergeDataStore: DataStore<Preferences> by preferencesDataStore(name = "fall_merge")

/**
 * Persistence for the Neon Drop (falling merge) mode: best score, best tile
 * and games played. Intentionally no currencies or consumables.
 */
class FallMergeRepository(private val context: Context) {

    companion object {
        val BEST_SCORE_KEY = intPreferencesKey("best_score")
        val BEST_TILE_KEY = intPreferencesKey("best_tile")
        val GAMES_PLAYED_KEY = intPreferencesKey("games_played")
    }

    val bestScore: Flow<Int> = context.fallMergeDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[BEST_SCORE_KEY] ?: 0 }

    val bestTile: Flow<Int> = context.fallMergeDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[BEST_TILE_KEY] ?: 0 }

    val gamesPlayed: Flow<Int> = context.fallMergeDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[GAMES_PLAYED_KEY] ?: 0 }

    suspend fun updateBests(score: Int, tile: Int) {
        context.fallMergeDataStore.edit { preferences ->
            val currentBestScore = preferences[BEST_SCORE_KEY] ?: 0
            if (score > currentBestScore) preferences[BEST_SCORE_KEY] = score
            val currentBestTile = preferences[BEST_TILE_KEY] ?: 0
            if (tile > currentBestTile) preferences[BEST_TILE_KEY] = tile
        }
    }

    suspend fun incrementGamesPlayed() {
        context.fallMergeDataStore.edit { preferences ->
            preferences[GAMES_PLAYED_KEY] = (preferences[GAMES_PLAYED_KEY] ?: 0) + 1
        }
    }
}
