package com.avfusionapps.game_2048.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.avfusionapps.game_2048.shared.game.GameSettingsRepository
import com.avfusionapps.game_2048.shared.game.GameViewModel

/** The main menu route: saved-game info from [game], everything else from the settings store; wired to [Nav]. */
@Composable
fun MainMenu(game: GameViewModel) {
    val name by GameSettingsRepository.playerNameFlow.collectAsState()
    val best by GameSettingsRepository.highScoreFlow.collectAsState()
    val coins by GameSettingsRepository.coinsFlow.collectAsState()
    val seen by GameSettingsRepository.seenSpritesFlow.collectAsState()
    val hasSaved by game.hasSavedGameFlow.collectAsState()
    val state by game.gameStateFlow.collectAsState()
    SpriteHomeScreen(
        playerName = name,
        coins = coins,
        best = best,
        hasSaved = hasSaved,
        savedScore = state.score,
        savedGrid = state.grid,
        spritesFound = seen.count { it in 2..2048 },
        actions = HomeActions(
            onContinue = { game.resumeSavedGame(); Nav.navigate("game?resume=true") },
            onNewClassic = { game.initializeGame(); Nav.navigate("game?resume=false") },
            onTimeAttack = { Nav.navigate("timeAttack") },
            onShooter = { Nav.navigate("dropMerge") },
            onProfile = { Nav.navigate("profile") },
            onSettings = { Nav.navigate("profile") },
            onCollection = { /* SpriteCollectionSheet not ported */ },
            onAchievements = { Nav.navigate("achievements") }
        )
    )
}
