package com.avfusionapps.game_2048.data // Or your preferred data layer package

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
        val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED_KEY = booleanPreferencesKey("vibration_enabled")
        val HAS_SEEN_CLASSIC_TUTORIAL_KEY = booleanPreferencesKey("has_seen_classic_tutorial")
        val HAS_SEEN_TIME_ATTACK_TUTORIAL_KEY = booleanPreferencesKey("has_seen_time_attack_tutorial")
        val HAS_SEEN_NEON_DROP_TUTORIAL_KEY = booleanPreferencesKey("has_seen_neon_drop_tutorial")
        val REMINDERS_ENABLED_KEY = booleanPreferencesKey("reminders_enabled")
        val LAST_PLAYED_AT_KEY = longPreferencesKey("last_played_at")
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

    // Suspending function to save the high score ONLY if it's higher.
    // The compare-and-set happens *inside* the edit block, which DataStore serializes,
    // so concurrent writers can never regress a higher stored value.
    suspend fun updateHighScoreIfHigher(newScore: Int) {
        context.gameSettingsDataStore.edit { preferences ->
            val currentHighScore = preferences[HIGH_SCORE_KEY] ?: DEFAULT_HIGH_SCORE
            if (newScore > currentHighScore) {
                preferences[HIGH_SCORE_KEY] = newScore
                println("DataStore: New high score saved: $newScore") // Logging
            } else {
                println("DataStore: Score $newScore not higher than $currentHighScore. Not saved.") // Logging
            }
        }
    }

    // Optional: Function to save both (e.g., initial setup) - generally prefer specific updates
    suspend fun saveSettings(name: String, score: Int) {
         context.gameSettingsDataStore.edit { preferences ->
             preferences[PLAYER_NAME_KEY] = name
             preferences[HIGH_SCORE_KEY] = score
         }
    }

    // Flow to get the sound enabled setting
    val soundEnabledFlow: Flow<Boolean> = context.gameSettingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SOUND_ENABLED_KEY] ?: true
        }

    // Flow to get the vibration enabled setting
    val vibrationEnabledFlow: Flow<Boolean> = context.gameSettingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[VIBRATION_ENABLED_KEY] ?: true
        }

    // Suspending function to update the sound setting
    suspend fun updateSoundEnabled(enabled: Boolean) {
        context.gameSettingsDataStore.edit { preferences ->
            preferences[SOUND_ENABLED_KEY] = enabled
        }
    }

    // Suspending function to update the vibration setting
    suspend fun updateVibrationEnabled(enabled: Boolean) {
        context.gameSettingsDataStore.edit { preferences ->
            preferences[VIBRATION_ENABLED_KEY] = enabled
        }
    }

    // Flow to check if classic tutorial has been seen
    val hasSeenClassicTutorialFlow: Flow<Boolean> = context.gameSettingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[HAS_SEEN_CLASSIC_TUTORIAL_KEY] ?: false
        }

    // Flow to check if time attack tutorial has been seen
    val hasSeenTimeAttackTutorialFlow: Flow<Boolean> = context.gameSettingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[HAS_SEEN_TIME_ATTACK_TUTORIAL_KEY] ?: false
        }

    // Suspending function to update the classic tutorial seen status
    suspend fun updateHasSeenClassicTutorial(hasSeen: Boolean) {
        context.gameSettingsDataStore.edit { preferences ->
            preferences[HAS_SEEN_CLASSIC_TUTORIAL_KEY] = hasSeen
        }
    }

    // Suspending function to update the time attack tutorial seen status
    suspend fun updateHasSeenTimeAttackTutorial(hasSeen: Boolean) {
        context.gameSettingsDataStore.edit { preferences ->
            preferences[HAS_SEEN_TIME_ATTACK_TUTORIAL_KEY] = hasSeen
        }
    }

    // Flow to check if the Neon Drop guided tutorial has been seen
    val hasSeenNeonDropTutorialFlow: Flow<Boolean> = context.gameSettingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[HAS_SEEN_NEON_DROP_TUTORIAL_KEY] ?: false
        }

    // Suspending function to update the Neon Drop tutorial seen status
    suspend fun updateHasSeenNeonDropTutorial(hasSeen: Boolean) {
        context.gameSettingsDataStore.edit { preferences ->
            preferences[HAS_SEEN_NEON_DROP_TUTORIAL_KEY] = hasSeen
        }
    }

    // ── Re-engagement reminders ──────────────────────────────────────────────

    /** Whether play-reminder notifications are enabled (default on). */
    val remindersEnabledFlow: Flow<Boolean> = context.gameSettingsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw exception
        }
        .map { preferences -> preferences[REMINDERS_ENABLED_KEY] ?: true }

    suspend fun updateRemindersEnabled(enabled: Boolean) {
        context.gameSettingsDataStore.edit { preferences ->
            preferences[REMINDERS_ENABLED_KEY] = enabled
        }
    }

    /** Epoch millis of the player's last active session; drives reminder inactivity checks. */
    val lastPlayedAtFlow: Flow<Long> = context.gameSettingsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw exception
        }
        .map { preferences -> preferences[LAST_PLAYED_AT_KEY] ?: 0L }

    suspend fun updateLastPlayedAt(timestamp: Long) {
        context.gameSettingsDataStore.edit { preferences ->
            preferences[LAST_PLAYED_AT_KEY] = timestamp
        }
    }
}