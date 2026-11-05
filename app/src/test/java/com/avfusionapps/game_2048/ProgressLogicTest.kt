package com.avfusionapps.game_2048

import com.avfusionapps.game_2048.data.progress.Achievements
import com.avfusionapps.game_2048.data.progress.DailyChallenge
import com.avfusionapps.game_2048.data.progress.DailyKind
import com.avfusionapps.game_2048.data.progress.GameMode
import com.avfusionapps.game_2048.data.progress.PlayerStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProgressLogicTest {

    @Test
    fun dailyChallengeIsTheSameAllDayAndChangesOverTime() {
        val day = LocalDate.of(2026, 11, 5)
        assertEquals(DailyChallenge.forDate(day), DailyChallenge.forDate(day))
        val month = (0 until 30).map { DailyChallenge.forDate(day.plusDays(it.toLong())) }
        assertTrue("a month should mix several kinds", month.map { it.first }.toSet().size >= 3)
    }

    @Test
    fun dailyTargetsAreReachable() {
        val start = LocalDate.of(2026, 1, 1)
        repeat(400) {
            val (kind, target) = DailyChallenge.forDate(start.plusDays(it.toLong()))
            when (kind) {
                DailyKind.REACH_TILE -> assertTrue(target in setOf(128, 256, 512))
                DailyKind.SHOOTER_TILE -> assertTrue(target in setOf(64, 128))
                DailyKind.COMBO -> assertTrue(target in 3..4)
                DailyKind.SHOOTER_CHAIN -> assertTrue(target in 2..3)
                DailyKind.SCORE -> assertTrue(target in 1500..4000)
                DailyKind.MERGES -> assertTrue(target in 40..100)
            }
        }
    }

    @Test
    fun dailyRewardGrowsWithStreakAndCaps() {
        fun d(streak: Int) = DailyChallenge("2026-11-05", DailyKind.MERGES, 40, 40, false, streak)
        assertEquals(50, d(0).reward)
        assertEquals(80, d(3).reward)
        assertEquals(120, d(7).reward)
        assertEquals(120, d(30).reward)
        assertTrue(d(0).done)
        assertEquals(GameMode.CLASSIC, d(0).mode)
        assertEquals(GameMode.SHOOTER, DailyChallenge("x", DailyKind.SHOOTER_CHAIN, 2, 0, false, 0).mode)
    }

    @Test
    fun achievementsHaveUniqueIdsAndTrackProgress() {
        val ids = Achievements.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(24, Achievements.all.size)
        val firstMerge = Achievements.all.first { it.id == "first_merge" }
        assertFalse(firstMerge.done(PlayerStats()))
        assertTrue(firstMerge.done(PlayerStats(merges = 1)))
        val king = Achievements.all.first { it.id == "tile_2048" }
        assertEquals(0.5f, king.progress(PlayerStats(biggestTile = 1024)), 0.0001f)
        assertTrue(king.done(PlayerStats(biggestTile = 4096)))
    }
}
