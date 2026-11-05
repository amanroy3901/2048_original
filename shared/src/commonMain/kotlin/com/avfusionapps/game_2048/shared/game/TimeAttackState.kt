package com.avfusionapps.game_2048.shared.game

import com.avfusionapps.game_2048.shared.platform.nowMillis

data class TimeAttackState(
    val grid: List<List<Int>> = createEmptyGrid(),
    val previousGrid: List<List<Int>>? = null,
    val previousScore: Int = 0,
    val previousTimeRemainingMillis: Long = 60_000L,
    val previousMultiplier: Float = 1.0f,
    val score: Int = 0,
    val totalTimeConfiguredMillis: Long = 60_000L,
    val timeRemainingMillis: Long = 60_000L,
    val isGameOver: Boolean = false,
    val timeSurvivedMillis: Long = 0L,
    val isPaused: Boolean = false,
    val multiplier: Float = 1.0f,
    val lastBonus: BonusType? = null,
    val tileAnimationInfo: Map<Pair<Int, Int>, TileAnimationInfo> = emptyMap(),
    val moveCount: Int = 0
) {
    companion object {
        fun createEmptyGrid(size: Int = 4): List<List<Int>> = List(size) { List(size) { 0 } }
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
    val date: Long = nowMillis()
)
