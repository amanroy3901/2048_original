package com.avfusionapps.game_2048.data // Or your preferred data layer package

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first // Import first()
import kotlinx.coroutines.flow.map
import java.io.IOException

// Define the DataStore instance at the top level linked to the Context
private val Context.gameSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "game_settings")

class GameSettingsRepository(private val context: Context) {

    // Define Preference Keys
    companion object {
        val PLAYER_NAME_KEY = stringPreferencesKey("player_name")
        val HIGH_SCORE_KEY = intPreferencesKey("high_score")
        const val DEFAULT_PLAYER_NAME = "Player"
        const val DEFAULT_HIGH_SCORE = 0
    }

    // Flow to get the player name
    val playerNameFlow: Flow<String> = context.gameSettingsDataStore.data
        .catch { exception ->
            // Handle potential IOExceptions
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PLAYER_NAME_KEY] ?: DEFAULT_PLAYER_NAME
        }

    // Flow to get the high score
    val highScoreFlow: Flow<Int> = context.gameSettingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[HIGH_SCORE_KEY] ?: DEFAULT_HIGH_SCORE
        }

    // Suspending function to save only the player name
    suspend fun updatePlayerName(name: String) {
        context.gameSettingsDataStore.edit { preferences ->
            preferences[PLAYER_NAME_KEY] = name
        }
    }

    // Suspending function to save the high score ONLY if it's higher
    suspend fun updateHighScoreIfHigher(newScore: Int) {
        // Use first() to get the current value before editing
        // Note: This reads *just before* editing. In rare concurrent scenarios,
        // you might edit based on slightly stale data, but it's usually acceptable.
        // For absolute safety, you might read inside the edit block, but that's less common.
        val currentHighScore = highScoreFlow.first() // Get current stored high score

        if (newScore > currentHighScore) {
            context.gameSettingsDataStore.edit { preferences ->
                preferences[HIGH_SCORE_KEY] = newScore
                println("DataStore: New high score saved: $newScore") // Logging
            }
        } else {
             println("DataStore: Score $newScore not higher than $currentHighScore. Not saved.") // Logging
        }
    }

    // Optional: Function to save both (e.g., initial setup) - generally prefer specific updates
    suspend fun saveSettings(name: String, score: Int) {
         context.gameSettingsDataStore.edit { preferences ->
             preferences[PLAYER_NAME_KEY] = name
             preferences[HIGH_SCORE_KEY] = score
         }
    }
}