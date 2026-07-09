package com.avfusionapps.game_2048.model

/**
 * Models for the Neon Rise (rack-shooter merge) game mode.
 *
 * Board convention is shared with Neon Drop: columns hang from the top,
 * row 0 is the ceiling, gravity pulls toward row 0. The rack is a row of
 * five tiles on the floor, each bound to its lane.
 */

object RiseConfig {
    const val LANES = 5
    const val ROWS = 12

    /** A stack longer than this after a settle is past the danger line. */
    const val DANGER_ROW = 9

    /** Shots between pressure rows at the start; tightens with milestones. */
    const val PRESSURE_BASE = 8
    const val PRESSURE_MIN = 5

    /** Rack slot refill delay after firing (millis). */
    const val REFILL_DELAY = 400L

    /** Free re-racks (rack tile rerolls) per pressure cycle. */
    const val RERACKS_PER_CYCLE = 1
}

/** Snapshot for the single-step undo (restores everything a shot changed). */
data class RiseUndoSnapshot(
    val columns: DropColumns,
    val rack: List<DropTile?>,
    val score: Int,
    val bestTileEver: Int,
    val shotsUntilPressure: Int,
    val canReRack: Boolean,
    val graceActive: Boolean
)

/** Full UI state for the Neon Rise screen. */
data class RiseState(
    val columns: DropColumns = List(RiseConfig.LANES) { emptyList() },
    /** Floor rack, one slot per lane; null while a slot is refilling. */
    val rack: List<DropTile?> = List(RiseConfig.LANES) { null },
    val score: Int = 0,
    val bestTileEver: Int = 0,
    val unlockTarget: Int = DropMergeConfig.FIRST_UNLOCK_TARGET,
    /** Shots remaining before the next pressure row drops. */
    val shotsUntilPressure: Int = RiseConfig.PRESSURE_BASE,
    /** Current cadence (total shots per pressure cycle) for the ring UI. */
    val pressureEvery: Int = RiseConfig.PRESSURE_BASE,
    /** One free rack reroll per pressure cycle. */
    val canReRack: Boolean = true,
    /** A pressure row pushed a stack past the line — one grace shot to fix it. */
    val graceActive: Boolean = false,
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false,
    val isResolving: Boolean = false,
    val canUndo: Boolean = false,
    val moveCount: Int = 0,
    /* ---- transient, animation-facing fields ---- */
    val lastStep: DropStep? = null,
    val comboCount: Int = 0,
    val justUnlockedValue: Int? = null
)
