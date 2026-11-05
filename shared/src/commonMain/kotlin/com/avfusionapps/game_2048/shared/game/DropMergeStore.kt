package com.avfusionapps.game_2048.shared.game

import com.avfusionapps.game_2048.shared.platform.KeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistence for the Neon Drop mode (KeyValueStore "drop_merge_*"): best score, best tile ever
 * built, games played, and the player's shooter level and XP. Coins live in [Progress].
 */
class DropMergeRepository {
    private companion object {
        const val BEST_SCORE_KEY = "drop_merge_best_score"
        const val BEST_TILE_KEY = "drop_merge_best_tile"
        const val GAMES_PLAYED_KEY = "drop_merge_games_played"
        const val LEVEL_KEY = "drop_merge_level"
        const val XP_KEY = "drop_merge_xp"
    }

    private val _bestScore = MutableStateFlow(KeyValueStore.getInt(BEST_SCORE_KEY, 0))
    val bestScore: StateFlow<Int> = _bestScore.asStateFlow()
    private val _bestTile = MutableStateFlow(KeyValueStore.getInt(BEST_TILE_KEY, 0))
    val bestTile: StateFlow<Int> = _bestTile.asStateFlow()
    private val _gamesPlayed = MutableStateFlow(KeyValueStore.getInt(GAMES_PLAYED_KEY, 0))
    val gamesPlayed: StateFlow<Int> = _gamesPlayed.asStateFlow()

    /** Shooter level (starts at 1) and XP towards the next level. */
    fun levelAndXp(): Pair<Int, Int> = KeyValueStore.getInt(LEVEL_KEY, 1) to KeyValueStore.getInt(XP_KEY, 0)

    fun saveLevel(level: Int, xp: Int) {
        KeyValueStore.putInt(LEVEL_KEY, level)
        KeyValueStore.putInt(XP_KEY, xp)
    }

    fun updateBests(score: Int, tile: Int) {
        if (score > _bestScore.value) { _bestScore.value = score; KeyValueStore.putInt(BEST_SCORE_KEY, score) }
        if (tile > _bestTile.value) { _bestTile.value = tile; KeyValueStore.putInt(BEST_TILE_KEY, tile) }
    }

    fun incrementGamesPlayed() {
        _gamesPlayed.value += 1
        KeyValueStore.putInt(GAMES_PLAYED_KEY, _gamesPlayed.value)
    }
}
