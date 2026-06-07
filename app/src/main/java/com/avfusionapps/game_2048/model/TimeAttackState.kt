package com.avfusionapps.game_2048.model

data class TimeAttackState(
    val grid: List<List<Int>> = createEmptyGrid(),
    val score: Int = 0,
    val timeRemainingMillis: Long = 60_000L, // 60 seconds default
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false,
    val multiplier: Float = 1.0f,
    val lastBonus: BonusType? = null
) {
    companion object {
        fun createEmptyGrid(size: Int = 4): List<List<Int>> {
            return List(size) { List(size) { 0 } }
        }
    }
}

sealed class BonusType(val timeBonusMillis: Long, val message: String) {
    object MergeBonus : BonusType(5000L, "+5s Merge!")
    object Tile128Bonus : BonusType(15000L, "+15s 128 Tile!")
    object Tile256Bonus : BonusType(15000L, "+15s 256 Tile!")
    object Tile512Bonus : BonusType(20000L, "+20s 512 Tile!")
    object Tile1024Bonus : BonusType(30000L, "+30s 1024 Tile!")
    object Tile2048Bonus : BonusType(60000L, "+60s 2048 Tile!")
}

data class TimeAttackScore(
    val score: Int,
    val timeSurvived: Long,
    val date: Long = System.currentTimeMillis()
)
