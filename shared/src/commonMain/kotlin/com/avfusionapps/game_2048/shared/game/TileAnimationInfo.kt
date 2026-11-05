package com.avfusionapps.game_2048.shared.game

data class TileAnimationInfo(
    val startPosition: Pair<Int, Int>? = null,
    val mergedFromPosition: Pair<Int, Int>? = null,
    val isNew: Boolean = false,
    val isMerged: Boolean = false,
    /** Moved by the Shuffle power-up (spins and swings into place) rather than slid by a swipe. */
    val shuffled: Boolean = false
)
