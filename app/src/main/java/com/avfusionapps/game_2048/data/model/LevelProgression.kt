package com.avfusionapps.game_2048.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Data class representing a player's level progression in the game.
 * This model is used for Firebase Firestore storage.
 */
data class LevelProgression(
    @get:PropertyName("playerId")
    @set:PropertyName("playerId")
    var playerId: String = "",

    @get:PropertyName("playerName")
    @set:PropertyName("playerName")
    var playerName: String = "",

    @get:PropertyName("currentLevel")
    @set:PropertyName("currentLevel")
    var currentLevel: Int = 1,

    @get:PropertyName("unlockedLevels")
    @set:PropertyName("unlockedLevels")
    var unlockedLevels: List<Int> = listOf(1),

    @get:PropertyName("levelUnlockTimes")
    @set:PropertyName("levelUnlockTimes")
    var levelUnlockTimes: Map<String, Timestamp> = emptyMap(),

    @get:PropertyName("lastUpdated")
    @set:PropertyName("lastUpdated")
    var lastUpdated: Timestamp = Timestamp.now(),

    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Timestamp = Timestamp.now()
) {
    companion object {
        /**
         * Level targets based on tile values - TESTING VERSION
         * Level 1: 2 (starting level)
         * Level 2: 128
         * Level 3: 256
         * Level 4: 512
         * Level 5: 1024
         * etc.
         */
        fun getTargetForLevel(level: Int): Int {
            return when (level) {
                1 -> 2
                else -> Math.pow(2.0, (level + 6).toDouble()).toInt() // 128, 256, 512, 1024, etc.
            }
        }

        /**
         * Get level for a given tile value - TESTING VERSION
         */
        fun getLevelForTileValue(tileValue: Int): Int {
            return when {
                tileValue < 128 -> 1
                tileValue >= 1024 -> 5 // Cap at level 5 for very high values
                else -> {
                    // Calculate level for values between 128 and 1024
                    val logValue = (Math.log(tileValue.toDouble()) / Math.log(2.0)).toInt()
                    when {
                        logValue >= 7 && logValue < 8 -> 2 // 128-255
                        logValue >= 8 && logValue < 9 -> 3 // 256-511
                        logValue >= 9 && logValue < 10 -> 4 // 512-1023
                        else -> 1
                    }
                }
            }
        }

        /**
         * Get the next level target after achieving current level
         */
        fun getNextLevelTarget(currentLevel: Int): Int {
            return getTargetForLevel(currentLevel + 1)
        }
    }
}