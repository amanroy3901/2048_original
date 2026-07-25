package com.avfusionapps.game_2048.game

import com.avfusionapps.game_2048.model.ConsumedTile
import com.avfusionapps.game_2048.model.DropColumns
import com.avfusionapps.game_2048.model.DropMergeConfig
import com.avfusionapps.game_2048.model.DropShotResult
import com.avfusionapps.game_2048.model.DropStep
import com.avfusionapps.game_2048.model.DropStepKind
import com.avfusionapps.game_2048.model.DropTile
import kotlin.random.Random

/**
 * Pure rules engine for the Neon Drop mode. No Android dependencies — fully
 * unit-testable. All randomness is injected through [random].
 *
 * Board convention (see DropMergeModels): columns hang from the top, row 0 is
 * the ceiling, gravity pulls toward row 0. Because a column is a dense list,
 * removing a tile collapses the stack automatically — list index IS the row.
 */
class DropMergeEngine(
    private val random: Random = Random.Default,
    startId: Long = 1L
) {
    private var nextId: Long = startId

    fun freshTile(value: Int): DropTile = DropTile(id = nextId++, value = value)

    // ─────────────────────────────── Spawning ───────────────────────────────

    /**
     * Smallest value the generator may spawn, driven by the best tile ever
     * built: 512 → 4, 1024 → 8, 2048 → 16, … (below 512 the floor is 2).
     * Tiles below this floor are also purged from the board when the floor rises.
     */
    fun minSpawnValue(bestTileEver: Int): Int =
        if (bestTileEver >= DropMergeConfig.FIRST_PURGE_TRIGGER) {
            1 shl (log2(bestTileEver) - 7)
        } else 2

    /** Highest tile value currently on the board (0 if the board is empty). */
    fun boardMax(columns: DropColumns): Int =
        columns.maxOfOrNull { col -> col.maxOfOrNull { it.value } ?: 0 } ?: 0

    /**
     * Weighted random spawn, **capped so the launcher never offers a tile bigger
     * than the largest tile currently on the board** — every spawned tile stays
     * mergeable instead of wasting a lane.
     *
     * The window is up to [DropMergeConfig.SPAWN_WINDOW] consecutive powers of two
     * ending at the board max (so it slides up as the board grows), floored at the
     * purge floor from the best tile ever. An empty board bootstraps to the floor.
     *
     * @param boardMax     highest tile currently on the board (see [boardMax]); 0 if empty
     * @param bestTileEver drives the purge floor (smallest value worth spawning)
     */
    fun spawnValue(boardMax: Int, bestTileEver: Int): Int {
        val floor = minSpawnValue(bestTileEver)
        // Never below the floor; empty/low board bootstraps up to the floor.
        val ceiling = maxOf(floor, boardMax)
        val topExp = log2(ceiling)
        val minExp = log2(floor)
        val botExp = maxOf(minExp, topExp - (DropMergeConfig.SPAWN_WINDOW - 1))
        val n = topExp - botExp + 1 // number of candidate powers of two (1..SPAWN_WINDOW)

        val weights = DropMergeConfig.SPAWN_WEIGHTS // smallest value weighted heaviest
        val total = (0 until n).sumOf { weights[it] }
        var pick = random.nextInt(total)
        for (i in 0 until n) {
            pick -= weights[i]
            if (pick < 0) return 1 shl (botExp + i)
        }
        return 1 shl botExp
    }

    /** Next milestone shown on the locked badge. */
    fun unlockTarget(bestTileEver: Int): Int {
        var target = DropMergeConfig.FIRST_UNLOCK_TARGET
        while (target <= bestTileEver) target = target shl 1
        return target
    }

    // ─────────────────────────────── Queries ────────────────────────────────

    fun isColumnFull(columns: DropColumns, col: Int): Boolean =
        columns[col].size >= DropMergeConfig.ROWS

    /**
     * Board is locked (game over) when every column is full and the current
     * tile can't merge with any column end — every possible shot would be fatal.
     */
    fun isBoardLocked(columns: DropColumns, currentValue: Int): Boolean =
        columns.all { it.size >= DropMergeConfig.ROWS } &&
            columns.none { it.isNotEmpty() && it.last().value == currentValue }

    // ─────────────────────────────── Resolution ─────────────────────────────

    /**
     * Resolve one shot of [tile] into [col]. Returns the animation steps
     * (placement → merges/collapses → purges) and the score gained.
     *
     * Firing into a full column whose end tile doesn't match is an overflow →
     * game over ([DropShotResult.overflow]); no steps are produced.
     */
    fun resolveShot(
        columns: DropColumns,
        col: Int,
        tile: DropTile,
        bestTileEver: Int
    ): DropShotResult {
        val full = isColumnFull(columns, col)
        val matchesEnd = columns[col].isNotEmpty() && columns[col].last().value == tile.value
        if (full && !matchesEnd) {
            return DropShotResult(steps = emptyList(), totalGained = 0, maxChain = 0, overflow = true)
        }

        val cols = columns.map { it.toMutableList() }.toMutableList()
        val steps = mutableListOf<DropStep>()
        var runningBest = bestTileEver
        var totalGained = 0
        var chain = 0
        var maxChain = 0

        // 1) Placement. When the column is full-but-matching the tile briefly
        //    sits one row past capacity and fuses on the very next step.
        cols[col].add(tile)
        steps += DropStep(columns = snapshot(cols), kind = DropStepKind.PLACE)

        // 2) The landed tile merges first (genre rule: the action point may
        //    fuse with up/left/right at once → ×2 / ×4 / ×8).
        var focus: DropTile? = tile

        while (true) {
            // 2a) Cascade around the focus tile, then sweep the rest of the board.
            while (true) {
                val target = focus?.takeIf { findTile(cols, it.id) != null && hasEqualNeighbor(cols, it) }
                    ?: findScanFocus(cols)
                    ?: break

                val (tCol, tRow) = findTile(cols, target.id)!!
                val neighbors = equalNeighbors(cols, tCol, tRow)
                val newValue = target.value shl neighbors.size
                chain += 1
                maxChain = maxOf(maxChain, chain)
                totalGained += newValue

                val consumed = neighbors.map { (nCol, nRow) ->
                    ConsumedTile(
                        tile = cols[nCol][nRow],
                        fromCol = nCol, fromRow = nRow,
                        toCol = tCol, toRow = tRow
                    )
                }
                // Remove consumed tiles (per column, bottom-up so indices stay valid),
                // then grow the focus tile in place. List removal = gravity collapse.
                neighbors.groupBy { it.first }.forEach { (c, cells) ->
                    cells.map { it.second }.sortedDescending().forEach { r -> cols[c].removeAt(r) }
                }
                val grown = target.copy(value = newValue)
                replaceTile(cols, target.id, grown)

                val unlocked = if (newValue >= unlockTarget(runningBest)) newValue else null
                if (newValue > runningBest) runningBest = newValue

                steps += DropStep(
                    columns = snapshot(cols),
                    kind = DropStepKind.MERGE,
                    mergedTileId = grown.id,
                    consumed = consumed,
                    gained = newValue,
                    chainIndex = chain,
                    unlockedValue = unlocked
                )
                focus = grown
            }

            // 2b) Board is merge-stable — purge tiers below the spawn floor.
            val floor = minSpawnValue(runningBest)
            val purged = mutableListOf<ConsumedTile>()
            for (c in cols.indices) {
                for (r in cols[c].indices.reversed()) {
                    if (cols[c][r].value < floor) {
                        purged += ConsumedTile(cols[c][r], c, r, c, r)
                        cols[c].removeAt(r)
                    }
                }
            }
            if (purged.isEmpty()) break

            val purgeGained = purged.sumOf { it.tile.value }
            totalGained += purgeGained
            steps += DropStep(
                columns = snapshot(cols),
                kind = DropStepKind.PURGE,
                consumed = purged,
                gained = purgeGained,
                chainIndex = chain
            )
            focus = null // collapse may open new merges — sweep again
        }

        return DropShotResult(steps, totalGained, maxChain, overflow = false)
    }

    // ─────────────────────────────── Internals ──────────────────────────────

    /**
     * Row-major scan (top→bottom, left→right) for the first tile with an equal
     * orthogonal neighbor. Scanning this order makes the survivor of a vertical
     * pair the UPPER tile (gravity side) and of a horizontal pair the LEFT one,
     * and keeps resolution fully deterministic.
     */
    private fun findScanFocus(cols: List<List<DropTile>>): DropTile? {
        val maxRows = cols.maxOf { it.size }
        for (r in 0 until maxRows) {
            for (c in cols.indices) {
                val tile = cols[c].getOrNull(r) ?: continue
                if (equalNeighbors(cols, c, r).isNotEmpty()) return tile
            }
        }
        return null
    }

    private fun hasEqualNeighbor(cols: List<List<DropTile>>, tile: DropTile): Boolean {
        val (c, r) = findTile(cols, tile.id) ?: return false
        return equalNeighbors(cols, c, r).isNotEmpty()
    }

    /** Orthogonal (up/down/left/right) neighbors holding the same value. */
    private fun equalNeighbors(cols: List<List<DropTile>>, col: Int, row: Int): List<Pair<Int, Int>> {
        val value = cols[col][row].value
        val candidates = listOf(col to row - 1, col to row + 1, col - 1 to row, col + 1 to row)
        return candidates.filter { (c, r) ->
            c in cols.indices && r >= 0 && cols[c].getOrNull(r)?.value == value
        }
    }

    private fun findTile(cols: List<List<DropTile>>, id: Long): Pair<Int, Int>? {
        for (c in cols.indices) {
            val r = cols[c].indexOfFirst { it.id == id }
            if (r >= 0) return c to r
        }
        return null
    }

    private fun replaceTile(cols: List<MutableList<DropTile>>, id: Long, newTile: DropTile) {
        val (c, r) = findTile(cols, id) ?: return
        cols[c][r] = newTile
    }

    private fun snapshot(cols: List<List<DropTile>>): DropColumns = cols.map { it.toList() }

    private fun log2(value: Int): Int = 31 - Integer.numberOfLeadingZeros(value)
}
