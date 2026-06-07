package com.avfusionapps.game_2048.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.avfusionapps.game_2048.model.TimeAttackScore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.timeAttackDataStore: DataStore<Preferences> by preferencesDataStore(name = "time_attack")

class TimeAttackRepository(private val context: Context) {

    companion object {
        val HIGH_SCORE_KEY = intPreferencesKey("high_score")
        val BEST_TIME_KEY = longPreferencesKey("best_time")
        val TOP_SCORES_KEY = stringPreferencesKey("top_scores")
        const val DEFAULT_HIGH_SCORE = 0
        const val DEFAULT_BEST_TIME = 0L
        const val MAX_SAVED_SCORES = 10
    }

    private val gson = Gson()

    val highScore: Flow<Int> = context.timeAttackDataStore.data
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

    val bestTimeSurvived: Flow<Long> = context.timeAttackDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[BEST_TIME_KEY] ?: DEFAULT_BEST_TIME
        }

    val topScores: Flow<List<TimeAttackScore>> = context.timeAttackDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val scoresJson = preferences[TOP_SCORES_KEY] ?: "[]"
            try {
                val type = object : TypeToken<List<TimeAttackScore>>() {}.type
                gson.fromJson(scoresJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun saveTimeAttackHighScore(score: TimeAttackScore) {
        context.timeAttackDataStore.edit { preferences ->
            // Update high score if better
            val currentHighScore = preferences[HIGH_SCORE_KEY] ?: DEFAULT_HIGH_SCORE
            if (score.score > currentHighScore) {
                preferences[HIGH_SCORE_KEY] = score.score
            }

            // Update best time survived if better
            val currentBestTime = preferences[BEST_TIME_KEY] ?: DEFAULT_BEST_TIME
            if (score.timeSurvived > currentBestTime) {
                preferences[BEST_TIME_KEY] = score.timeSurvived
            }

            // Add to top scores list
            val currentScoresJson = preferences[TOP_SCORES_KEY] ?: "[]"
            val currentScores = try {
                val type = object : TypeToken<List<TimeAttackScore>>() {}.type
                gson.fromJson<List<TimeAttackScore>>(currentScoresJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            val updatedScores = (currentScores + score)
                .sortedByDescending { it.score }
                .take(MAX_SAVED_SCORES)

            preferences[TOP_SCORES_KEY] = gson.toJson(updatedScores)
        }
    }

    suspend fun clearAllScores() {
        context.timeAttackDataStore.edit { preferences ->
            preferences[HIGH_SCORE_KEY] = DEFAULT_HIGH_SCORE
            preferences[BEST_TIME_KEY] = DEFAULT_BEST_TIME
            preferences[TOP_SCORES_KEY] = "[]"
        }
    }
}
