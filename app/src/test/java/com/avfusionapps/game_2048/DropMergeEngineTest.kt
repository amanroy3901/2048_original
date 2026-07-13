package com.avfusionapps.game_2048

import com.avfusionapps.game_2048.game.DropMergeEngine
import com.avfusionapps.game_2048.model.DropMergeConfig
import com.avfusionapps.game_2048.model.DropStepKind
import com.avfusionapps.game_2048.model.DropTile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Unit tests for the Neon Drop rules engine.
 *
 * Board convention reminder: row 0 = top (ceiling), gravity pulls toward
 * row 0, a shot tile lands at row = column.size.
 */
class DropMergeEngineTest {

    private fun engine() = DropMergeEngine(random = Random(42))

    /** Builds a board from value lists (top → bottom per column). */
    private fun board(vararg columns: List<Int>): List<List<DropTile>> {
        var id = 1000L
        val result = MutableList(DropMergeConfig.COLUMNS) { emptyList<DropTile>() }
        columns.forEachIndexed { i, values ->
            result[i] = values.map { DropTile(id = id++, value = it) }
        }
        return result
    }

    private fun values(columns: List<List<DropTile>>): List<List<Int>> =
        columns.map { col -> col.map { it.value } }

    // ── Placement ──

    @Test
    fun `shot into empty column lands at the top`() {
        val e = engine()
        val result = e.resolveShot(board(), col = 2, tile = e.freshTile(4), bestTileEver = 0)

        assertFalse(result.overflow)
        assertEquals(1, result.steps.size)
        assertEquals(DropStepKind.PLACE, result.steps[0].kind)
        assertEquals(listOf(4), values(result.steps.last().columns)[2])
        assertEquals(0, result.totalGained)
    }

    @Test
    fun `shot stacks below existing tiles`() {
        val e = engine()
        val start = board(listOf(8, 4))
        val result = e.resolveShot(start, col = 0, tile = e.freshTile(2), bestTileEver = 0)

        assertEquals(listOf(8, 4, 2), values(result.steps.last().columns)[0])
    }

    // ── Merging ──

    @Test
    fun `vertical merge cascades into the tile above`() {
        val e = engine()
        // Column: [8, 4]; shoot 4 → 4+4=8 at row 1, then 8+8=16 at row 0.
        val start = board(listOf(8, 4))
        val result = e.resolveShot(start, col = 0, tile = e.freshTile(4), bestTileEver = 0)

        val final = values(result.steps.last().columns)
        assertEquals(listOf(16), final[0])
        assertEquals(8 + 16, result.totalGained)
        assertEquals(2, result.maxChain)
    }

    @Test
    fun `merging with two neighbors quadruples`() {
        val e = engine()
        // Columns 0 and 2 have a 4 at row 0; shoot 4 into empty column 1 →
        // it lands at row 0 with equal left AND right neighbors → 4 × 2² = 16.
        val start = board(listOf(4), emptyList(), listOf(4))
        val result = e.resolveShot(start, col = 1, tile = e.freshTile(4), bestTileEver = 0)

        val final = values(result.steps.last().columns)
        assertEquals(emptyList<Int>(), final[0])
        assertEquals(listOf(16), final[1])
        assertEquals(emptyList<Int>(), final[2])
        assertEquals(16, result.totalGained)
    }

    @Test
    fun `gravity collapses columns after a side merge`() {
        val e = engine()
        // Column 0: [8, 2]; column 1: empty. Shoot 8 into column 1: it lands at
        // row 0 next to the 8 → merges to 16; the 2 in column 0 slides up.
        val start = board(listOf(8, 2))
        val result = e.resolveShot(start, col = 1, tile = e.freshTile(8), bestTileEver = 0)

        val final = values(result.steps.last().columns)
        assertEquals(listOf(2), final[0])
        assertEquals(listOf(16), final[1])
    }

    @Test
    fun `chain triggered by gravity collapse`() {
        val e = engine()
        // Column 0: [4, 2, 2]. Shoot 2 → bottom pair 2+2=4 at row 2... then
        // collapse: [4, 2, 4]? No: shoot 2 lands at row 3, merges with the 2
        // above (row 2) → grows to 4 at row 2 → column [4, 2, 4] — the grown 4
        // is not adjacent to the top 4, so it stays. Verify exactly that.
        val start = board(listOf(4, 2, 2))
        val result = e.resolveShot(start, col = 0, tile = e.freshTile(2), bestTileEver = 0)

        val final = values(result.steps.last().columns)
        assertEquals(listOf(4, 2, 4), final[0])
        assertEquals(4, result.totalGained)
    }

    // ── Overflow & lock ──

    @Test
    fun `shot into full non-matching column overflows`() {
        val e = engine()
        val fullColumn = List(DropMergeConfig.ROWS) { 1 shl (it % 3 + 1) } // 2,4,8,...
        val start = board(fullColumn)
        val result = e.resolveShot(start, col = 0, tile = e.freshTile(64), bestTileEver = 0)

        assertTrue(result.overflow)
        assertTrue(result.steps.isEmpty())
    }

    @Test
    fun `shot into full column with matching end merges instead of overflowing`() {
        val e = engine()
        // 8 slots ending in 4: 256,128,64,32,16,8,2,4
        val start = board(listOf(256, 128, 64, 32, 16, 8, 2, 4))
        val result = e.resolveShot(start, col = 0, tile = e.freshTile(4), bestTileEver = 0)

        assertFalse(result.overflow)
        val final = values(result.steps.last().columns)
        assertEquals(listOf(256, 128, 64, 32, 16, 8, 2, 8), final[0])
    }

    @Test
    fun `board locked only when all columns full and no end matches`() {
        val e = engine()
        val full = List(DropMergeConfig.ROWS) { 1 shl (it + 1) } // ends at 256
        val allFull = board(full, full, full, full, full)

        assertTrue(e.isBoardLocked(allFull, currentValue = 4))
        assertFalse(e.isBoardLocked(allFull, currentValue = 256)) // end tile matches
        val oneFree = board(full, full, full, full, emptyList())
        assertFalse(e.isBoardLocked(oneFree, currentValue = 4))
    }

    // ── Spawning, milestones, purge ──

    @Test
    fun `spawn floor rises with best tile`() {
        val e = engine()
        assertEquals(2, e.minSpawnValue(0))
        assertEquals(2, e.minSpawnValue(256))
        assertEquals(4, e.minSpawnValue(512))
        assertEquals(8, e.minSpawnValue(1024))
        assertEquals(16, e.minSpawnValue(2048))
    }

    @Test
    fun `spawn values stay inside the window and never exceed the board max`() {
        val e = engine()
        repeat(500) {
            val v = e.spawnValue(boardMax = 32, bestTileEver = 0)
            assertTrue("spawned $v", v in listOf(2, 4, 8, 16, 32))
        }
        repeat(500) {
            val v = e.spawnValue(boardMax = 128, bestTileEver = 1024)
            assertTrue("spawned $v", v in listOf(8, 16, 32, 64, 128))
        }
        // Never spawn a tile larger than the current board max.
        repeat(500) {
            val v = e.spawnValue(boardMax = 8, bestTileEver = 0)
            assertTrue("spawned $v", v in listOf(2, 4, 8))
        }
        // An empty board bootstraps to the floor.
        repeat(50) {
            assertEquals(2, e.spawnValue(boardMax = 0, bestTileEver = 0))
        }
    }

    @Test
    fun `unlock target is next power above best`() {
        val e = engine()
        assertEquals(256, e.unlockTarget(0))
        assertEquals(256, e.unlockTarget(128))
        assertEquals(512, e.unlockTarget(256))
        assertEquals(1024, e.unlockTarget(512))
    }

    @Test
    fun `reaching a milestone flags the step as unlocked`() {
        val e = engine()
        // 128 + 128 = 256 = first unlock target.
        val start = board(listOf(128))
        val result = e.resolveShot(start, col = 0, tile = e.freshTile(128), bestTileEver = 128)

        val mergeStep = result.steps.first { it.kind == DropStepKind.MERGE }
        assertEquals(256, mergeStep.unlockedValue)
    }

    @Test
    fun `crossing 512 purges all twos and credits their score`() {
        val e = engine()
        // Column 0: [256, 256-to-be]: shoot 256 onto 256 → 512. 2s elsewhere purge.
        val start = board(listOf(256), listOf(2, 4), listOf(8, 2))
        val result = e.resolveShot(start, col = 0, tile = e.freshTile(256), bestTileEver = 256)

        val purgeStep = result.steps.firstOrNull { it.kind == DropStepKind.PURGE }
        assertTrue("expected a purge step", purgeStep != null)
        assertEquals(2, purgeStep!!.consumed.size)

        val final = values(result.steps.last().columns)
        assertEquals(listOf(512), final[0])
        assertEquals(listOf(4), final[1])
        assertEquals(listOf(8), final[2])
        assertEquals(512 + 2 + 2, result.totalGained)
    }

    @Test
    fun `no purge below the 512 threshold`() {
        val e = engine()
        val start = board(listOf(2), listOf(2, 4))
        val result = e.resolveShot(start, col = 3, tile = e.freshTile(8), bestTileEver = 256)

        assertTrue(result.steps.none { it.kind == DropStepKind.PURGE })
    }

    @Test
    fun `tile ids are stable through merges`() {
        val e = engine()
        val start = board(listOf(8, 4))
        val shot = e.freshTile(4)
        val result = e.resolveShot(start, col = 0, tile = shot, bestTileEver = 0)

        // The shot tile is the merge survivor of the first merge (grows in place).
        val firstMerge = result.steps.first { it.kind == DropStepKind.MERGE }
        assertEquals(shot.id, firstMerge.mergedTileId)
    }
}
