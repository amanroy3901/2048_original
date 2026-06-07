package com.avfusionapps.game_2048.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avfusionapps.game_2048.data.repository.ThemeRepository
import com.avfusionapps.game_2048.ui.theme.GameTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ThemeRepository(application.applicationContext)

    val currentTheme: StateFlow<GameTheme> = repository.currentTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GameTheme.NeonRush
        )

    fun setTheme(theme: GameTheme) {
        viewModelScope.launch {
            repository.setTheme(theme)
        }
    }
}
