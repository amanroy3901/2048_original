package com.avfusionapps.game_2048.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.avfusionapps.game_2048.shared.game.DropMergeViewModel
import com.avfusionapps.game_2048.shared.game.GameViewModel
import com.avfusionapps.game_2048.shared.game.SpriteSfx
import com.avfusionapps.game_2048.shared.game.TimeAttackViewModel
import com.avfusionapps.game_2048.shared.platform.Telemetry
import com.avfusionapps.game_2048.shared.sprites.ProvideSpriteClock

/** The whole game: one set of view models, the sprite clock, and a screen per Nav route. */
@Composable
fun App() {
    val game = remember { GameViewModel() }
    val timeAttack = remember { TimeAttackViewModel() }
    val dropMerge = remember { DropMergeViewModel() }
    LaunchedEffect(Unit) { SpriteSfx.loadAll() }
    remember { Telemetry.ensureLoaded() }
    MaterialTheme(colorScheme = darkColorScheme()) {
        ProvideSpriteClock {
            Box(Modifier.fillMaxSize()) {
                when (Nav.route) {
                    "game?resume=true" -> ClassicScreen(game, resume = true)
                    "game?resume=false" -> ClassicScreen(game, resume = false)
                    "timeAttack" -> TimeAttackScreen(timeAttack)
                    "dropMerge" -> ShooterScreen(dropMerge)
                    "achievements" -> AchievementsScreen()
                    "profile" -> ProfileScreen()
                    else -> MainMenu(game)
                }
                ProgressCelebrations()
                ConsentPrompt()
            }
        }
    }
}
