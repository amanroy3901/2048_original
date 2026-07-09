package com.avfusionapps.game_2048

import com.avfusionapps.game_2048.game.RiseEngine
import com.avfusionapps.game_2048.model.DropTile
import com.avfusionapps.game_2048.model.RiseConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Unit tests for the Neon Rise rules layer (cascades are covered by DropMergeEngineTest). */
class RiseEngineTest {

    private fun engine() = RiseEngine(random = Random(7))

    private fun board(vararg columns: List<Int>): List<List<DropTile>> {
        var id = 5000L
        val result = MutableList(RiseConfig.LANES) { emptyList<DropTile>() }
        columns.forEachIndexed { i, values ->
            result[i] = values.map { DropTile(id = id++, value = it) }
        }
        return result
    }

    private fun values(columns: List<List<DropTile>>): List<List<Int>> =
        columns.map { col -> col.map { it.value } }

    // ── Shooting (thin wrapper sanity + no purge) ──

    @Test
    fun `non-matching shot sticks and extends the stack`() {
        val e = engine()
        val start = board(listOf(8, 4))
        val result = e.shoot(start, lane = 0, tile = e.freshTile(16), bestTileEver = 0)

        assertFalse(result.overflow)
        assertEquals(listOf(8, 4, 16), values(result.steps.last().columns)[0])
        assertEquals(0, result.totalGained)
    }

    @Test
    fun `matching shot merges at the tip`() {
        val e = engine()
        val start = board(listOf(8, 4))
        val result = e.shoot(start, lane = 0, tile = e.freshTile(4), bestTileEver = 0)

        // 4+4=8 then cascades into the 8 above → 16 (shared engine behavior).
        assertEquals(listOf(16), values(result.steps.last().columns)[0])
    }

    @Test
    fun `rise shots never purge low tiles even past 512`() {
        val e = engine()
        val start = board(listOf(256), listOf(2), listOf(4, 2))
        val result = e.shoot(start, lane = 0, tile = e.freshTile(256), bestTileEver = 256)

        val final = values(result.steps.last().columns)
        assertEquals(listOf(512), final[0])
        assertEquals(listOf(2), final[1]) // 2s survive — no purge in Rise
        assertEquals(listOf(4, 2), final[2])
    }

    @Test
    fun `rise board is taller than drop board`() {
        val e = engine()
        // 10 tiles would overflow Neon Drop (8 rows) but fits Rise (12 rows).
        val tall = List(10) { 1 shl (it % 2 + 1) } // alternating 2,4
        val start = board(tall)
        val result = e.shoot(start, lane = 0, tile = e.freshTile(8), bestTileEver = 0)

        assertFalse(result.overflow)
        assertEquals(11, result.steps.last().columns[0].size)
    }

    // ── Danger line ──

    @Test
    fun `over line only when a stack exceeds DANGER_ROW`() {
        val e = engine()
        val atLine = List(RiseConfig.DANGER_ROW) { 1 shl (it % 3 + 1) }
        assertFalse(e.isOverLine(board(atLine)))

        val over = List(RiseConfig.DANGER_ROW + 1) { 1 shl (it % 3 + 1) }
        assertTrue(e.isOverLine(board(over)))
    }

    // ── Pressure cadence ──

    @Test
    fun `cadence tightens with milestones and floors at minimum`() {
        val e = engine()
        assertEquals(8, e.pressureEvery(0))
        assertEquals(8, e.pressureEvery(256))
        assertEquals(7, e.pressureEvery(512))
        assertEquals(6, e.pressureEvery(1024))
        assertEquals(5, e.pressureEvery(2048))
        assertEquals(5, e.pressureEvery(8192)) // floored
    }

    // ── Pressure rows ──

    @Test
    fun `pressure row adds one tile to the ceiling of every lane`() {
        val e = engine()
        val start = board(listOf(8, 4), emptyList(), listOf(16))
        val after = e.pressureRow(start, bestTileEver = 0)

        after.forEachIndexed { lane, stack ->
            assertEquals(start[lane].size + 1, stack.size)
        }
        // Existing tiles keep their ids and shift down one row.
        assertEquals(start[0][0].id, after[0][1].id)
        assertEquals(start[0][1].id, after[0][2].id)
        assertEquals(start[2][0].id, after[2][1].id)
    }

    @Test
    fun `pressure tiles never equal left neighbor or tile below`() {
        val e = engine()
        repeat(60) {
            val start = board(listOf(2, 4), listOf(4), listOf(8), listOf(2), listOf(16, 2))
            val after = e.pressureRow(start, bestTileEver = 0)
            val newRow = after.map { it.first() }

            for (lane in 0 until RiseConfig.LANES) {
                if (lane > 0) {
                    assertNotEquals("lane $lane equals left", newRow[lane - 1].value, newRow[lane].value)
                }
                start[lane].firstOrNull()?.let { below ->
                    assertNotEquals("lane $lane equals below", below.value, newRow[lane].value)
                }
            }
        }
    }

    @Test
    fun `pressure values come from the low half of the spawn window`() {
        val e = engine()
        repeat(60) {
            val after = e.pressureRow(board(), bestTileEver = 0)
            after.forEach { stack ->
                assertTrue("value ${stack.first().value}", stack.first().value in listOf(2, 4, 8))
            }
        }
        repeat(60) {
            // best 1024 → spawn floor 8 → pressure pool 8/16/32
            val after = e.pressureRow(board(), bestTileEver = 1024)
            after.forEach { stack ->
                assertTrue("value ${stack.first().value}", stack.first().value in listOf(8, 16, 32))
            }
        }
    }
}
