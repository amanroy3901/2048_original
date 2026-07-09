package com.avfusionapps.game_2048.model

/**
 * Models for the Neon Drop (shoot-up merge) game mode.
 *
 * Board convention: 5 columns, each column is an ordered list of tiles whose
 * index IS the row, with row 0 anchored at the TOP of the board. Gravity pulls
 * tiles toward row 0 (the ceiling). New tiles land at row = column.size.
 */

/** A tile on the Neon Drop board. Stable [id] drives position/value animations. */
data class DropTile(
    val id: Long,
    val value: Int
)

/** Board = 5 columns of stacked tiles, hanging from the top. */
typealias DropColumns = List<List<DropTile>>

/** Why a resolution step happened — lets the UI pick the right animation. */
enum class DropStepKind { PLACE, MERGE, PURGE }

/**
 * One snapshot in a shot's resolution cascade. The UI animates tiles (by id)
 * from the previous snapshot to this one.
 *
 * @param columns        full board state after this step
 * @param kind           what happened in this step
 * @param mergedTileId   tile that just grew (pop + glow it), if any
 * @param consumed       tiles removed this step with the position they fly to / vanish at
 * @param gained         score gained in this step
 * @param chainIndex     0 for the landing, 1..n for cascade steps (drives COMBO text)
 * @param unlockedValue  a brand-new max tile value first reached in this step, if any
 */
data class DropStep(
    val columns: DropColumns,
    val kind: DropStepKind,
    val mergedTileId: Long? = null,
    val consumed: List<ConsumedTile> = emptyList(),
    val gained: Int = 0,
    val chainIndex: Int = 0,
    val unlockedValue: Int? = null
)

/** A tile removed during a step, with where it visually converges. */
data class ConsumedTile(
    val tile: DropTile,
    val fromCol: Int,
    val fromRow: Int,
    val toCol: Int,
    val toRow: Int
)

/** Result of resolving one shot. */
data class DropShotResult(
    val steps: List<DropStep>,
    val totalGained: Int,
    val maxChain: Int,
    /** The shot overflowed a full, non-matching column → game over. */
    val overflow: Boolean
)

/** Snapshot used for single-step undo. */
data class DropUndoSnapshot(
    val columns: DropColumns,
    val score: Int,
    val currentValue: Int,
    val nextValue: Int,
    val bestTileEver: Int
)

/** Full UI state for the Neon Drop screen. */
data class DropMergeState(
    val columns: DropColumns = List(DropMergeConfig.COLUMNS) { emptyList() },
    val score: Int = 0,
    val currentValue: Int = 2,
    val nextValue: Int = 4,
    /** Highest tile ever built across all games (drives spawn window + unlock badge). */
    val bestTileEver: Int = 0,
    /** Next milestone tile shown on the locked badge. */
    val unlockTarget: Int = DropMergeConfig.FIRST_UNLOCK_TARGET,
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false,
    /** True while a shot's cascade is being played back — input is locked. */
    val isResolving: Boolean = false,
    val canUndo: Boolean = false,
    /** One free skip (swap current → next) per turn; resets after each shot. */
    val canSkip: Boolean = true,
    val moveCount: Int = 0,
    /* ---- transient, animation-facing fields (cleared as steps advance) ---- */
    val lastStep: DropStep? = null,
    val comboCount: Int = 0,
    val justUnlockedValue: Int? = null
)

object DropMergeConfig {
    const val COLUMNS = 5
    const val ROWS = 8

    /** Spawn window spans this many consecutive powers of two. */
    const val SPAWN_WINDOW = 5

    /** Weights for the spawn window, smallest value first. */
    val SPAWN_WEIGHTS = intArrayOf(6, 5, 4, 2, 1)

    /** First milestone shown as the locked badge target. */
    const val FIRST_UNLOCK_TARGET = 256

    /** Reaching this best tile starts purging the lowest tier (512 → purge 2s). */
    const val FIRST_PURGE_TRIGGER = 512

    /** A column with fewer than this many free cells is "in danger". */
    const val DANGER_FREE_CELLS = 2
}
