package com.avfusionapps.game_2048.model

data class TileAnimationInfo(
    val startPosition: Pair<Int, Int>? = null,
    val mergedFromPosition: Pair<Int, Int>? = null,
    val isNew: Boolean = false,
    val isMerged: Boolean = false
)
