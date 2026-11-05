package com.avfusionapps.game_2048.shared.game

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

/**
 * Why a resolution step happened — lets the UI pick the right animation.
 * SETTLE is a board rearranged by a power-up (shuffle or remove), before it merges.
 */
enum class DropStepKind { PLACE, MERGE, PURGE, SETTLE }

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

/** Snapshot of the board before a shot or power-up, so Undo can rewind it. */
data class DropUndoSnapshot(
    val columns: DropColumns,
    val score: Int,
    val currentValue: Int,
    val nextValue: Int,
    val bestTileEver: Int,
    val freezeShots: Int = 0,
    val level: Int = 1,
    val xp: Int = 0
)

/** The four counted power-ups. NEXT (swap) is free once per shot, so it isn't one. */
enum class DropPower { UNDO, SHUFFLE, REMOVE, FREEZE }

/** Charges left of each power-up this game. When one runs out it costs [COST] coins. */
data class DropPowerUps(
    val undo: Int = START,
    val shuffle: Int = START,
    val remove: Int = START,
    val freeze: Int = START
) {
    operator fun get(power: DropPower): Int = when (power) {
        DropPower.UNDO -> undo
        DropPower.SHUFFLE -> shuffle
        DropPower.REMOVE -> remove
        DropPower.FREEZE -> freeze
    }

    fun plus(power: DropPower, delta: Int): DropPowerUps = when (power) {
        DropPower.UNDO -> copy(undo = (undo + delta).coerceAtLeast(0))
        DropPower.SHUFFLE -> copy(shuffle = (shuffle + delta).coerceAtLeast(0))
        DropPower.REMOVE -> copy(remove = (remove + delta).coerceAtLeast(0))
        DropPower.FREEZE -> copy(freeze = (freeze + delta).coerceAtLeast(0))
    }

    companion object {
        const val START = 3
        const val COST = 50
    }
}

/** A level-up the chest just paid out; [key] changes every time so the UI replays it. */
data class DropLevelUp(val level: Int, val reward: DropPower, val key: Int)

/** A short message shown over the board; [key] changes every time so it replays. */
data class DropNotice(val text: String, val key: Int)

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
    /** The cascade being played is a shuffle's or remove's, so the launcher stays loaded. */
    val powerResolving: Boolean = false,
    /** True when there's a shot or power-up to rewind (Undo still needs a charge or coins). */
    val canUndo: Boolean = false,
    /** One free NEXT swap (current ↔ next) per shot; resets after each shot. */
    val canSkip: Boolean = true,
    val moveCount: Int = 0,
    /** Bumped on every shot, shuffle and remove; keys the merge animations. */
    val actionCount: Int = 0,
    val powerUps: DropPowerUps = DropPowerUps(),
    /** Remove is armed: the next tap on a tile takes it off the board. */
    val removeArmed: Boolean = false,
    /** Shots left with the launcher frozen (it keeps the same tile). */
    val freezeShots: Int = 0,
    /** Player level and XP towards the next one (kept across games). */
    val level: Int = 1,
    val xp: Int = 0,
    /** The coin balance, for pricing power-ups that have run out. */
    val coins: Int = 0,
    /* ---- transient, animation-facing fields (cleared as steps advance) ---- */
    val lastStep: DropStep? = null,
    val comboCount: Int = 0,
    val justUnlockedValue: Int? = null,
    /** True on the frame an undo restores the board, so tiles split back in place
     *  instead of animating up from the launcher. Cleared on the next shot. */
    val justUndid: Boolean = false,
    /** Tile being hit by Remove right now (the hammer plays over it). */
    val removingTileId: Long? = null,
    /** Bumped on every shuffle so the sprites spin as they move. */
    val shuffleKey: Int = 0,
    val levelUp: DropLevelUp? = null,
    val notice: DropNotice? = null
) {
    val xpNeeded: Int get() = DropMergeConfig.xpForLevel(level)
}

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

    /** Freeze keeps the launcher's tile for this many shots. */
    const val FREEZE_SHOTS = 3

    /** Undo can rewind this many shots or power-ups back. */
    const val UNDO_DEPTH = 10

    /** XP (earned 1:1 with score) needed to finish [level]: 1,400 for level 1, +600 per level. */
    fun xpForLevel(level: Int): Int = 800 + 600 * level
}
