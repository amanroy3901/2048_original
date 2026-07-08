package com.avfusionapps.game_2048

import com.avfusionapps.game_2048.data.model.LevelProgression
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun levelUnlockLogic_triggers_when_tile_exceeds_previous_level() {
        val previousLevel = 2 // e.g., achieved 128 previously
        val tileValue = 256
        val newLevel = LevelProgression.getLevelForTileValue(tileValue)
        assertTrue(newLevel > previousLevel)
    }

    @Test
    fun levelUnlockLogic_does_not_trigger_for_lower_tile() {
        val previousLevel = 3 // currently at 256
        val tileValue = 128
        val newLevel = LevelProgression.getLevelForTileValue(tileValue)
        assertFalse(newLevel > previousLevel)
    }
}
