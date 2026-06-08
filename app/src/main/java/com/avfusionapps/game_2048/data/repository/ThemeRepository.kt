package com.avfusionapps.game_2048.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.avfusionapps.game_2048.ui.theme.GameTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

class ThemeRepository(private val context: Context) {

    companion object {
        val SELECTED_THEME_KEY = stringPreferencesKey("selected_theme")
        const val DEFAULT_THEME = "Neon Pink"
    }

    val currentTheme: Flow<GameTheme> = context.themeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeName = preferences[SELECTED_THEME_KEY] ?: DEFAULT_THEME
            GameTheme.fromName(themeName)
        }

    suspend fun setTheme(theme: GameTheme) {
        context.themeDataStore.edit { preferences ->
            preferences[SELECTED_THEME_KEY] = theme.name
        }
    }
}
