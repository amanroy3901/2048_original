package com.avfusionapps.game_2048.game

import com.avfusionapps.game_2048.model.DropColumns
import com.avfusionapps.game_2048.model.DropShotResult
import com.avfusionapps.game_2048.model.DropTile
import com.avfusionapps.game_2048.model.RiseConfig
import kotlin.random.Random

/**
 * Pure rules engine for the Neon Rise mode. Merge cascades are delegated to
 * the shared [DropMergeEngine] (same top-anchored board model); this layer
 * adds the rack-shooter specifics: pressure rows, cadence, and the danger line.
 */
class RiseEngine(
    private val random: Random = Random.Default,
    startId: Long = 1L
) {
    /** Shared merge core, sized for the taller Rise board. */
    val core = DropMergeEngine(random = random, startId = startId, rows = RiseConfig.ROWS)

    fun freshTile(value: Int): DropTile = core.freshTile(value)

    fun spawnValue(bestTileEver: Int): Int = core.spawnValue(bestTileEver)

    fun unlockTarget(bestTileEver: Int): Int = core.unlockTarget(bestTileEver)

    /** Fire [tile] up [lane]. Same cascade as Neon Drop, but never purges. */
    fun shoot(columns: DropColumns, lane: Int, tile: DropTile, bestTileEver: Int): DropShotResult =
        core.resolveShot(columns, lane, tile, bestTileEver, purgeEnabled = false)

    /** Any stack past the danger line? Checked after every settle. */
    fun isOverLine(columns: DropColumns): Boolean =
        columns.any { it.size > RiseConfig.DANGER_ROW }

    /** Shots per pressure cycle — tightens as milestones rise, floored. */
    fun pressureEvery(bestTileEver: Int): Int = when {
        bestTileEver >= 2048 -> RiseConfig.PRESSURE_MIN
        bestTileEver >= 1024 -> 6
        bestTileEver >= 512 -> 7
        else -> RiseConfig.PRESSURE_BASE
    }

    /**
     * Insert a pressure row: one fodder tile at the ceiling of every lane,
     * pushing all stacks down one row. Values come from the LOW half of the
     * spawn window and are chosen to never equal their left neighbor or the
     * tile directly below — a pressure row must add pressure, not auto-merge.
     */
    fun pressureRow(columns: DropColumns, bestTileEver: Int): DropColumns {
        val minValue = core.minSpawnValue(bestTileEver)
        // Low half of the window: 3 consecutive values from the floor.
        val pool = listOf(minValue, minValue * 2, minValue * 4)

        val newRow = mutableListOf<DropTile>()
        val result = columns.mapIndexed { lane, stack ->
            val leftValue = newRow.lastOrNull()?.value
            val belowValue = stack.firstOrNull()?.value
            val candidates = pool.filter { it != leftValue && it != belowValue }
                .ifEmpty { pool } // degenerate case: accept a merge rather than fail
            val tile = freshTile(candidates[random.nextInt(candidates.size)])
            newRow += tile
            listOf(tile) + stack
        }
        return result
    }
}
