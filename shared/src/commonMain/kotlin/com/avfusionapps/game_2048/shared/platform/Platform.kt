package com.avfusionapps.game_2048.shared.platform

/** Small key-value store (SharedPreferences on Android, NSUserDefaults on iOS) for saves, stats and settings. */
expect object KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getInt(key: String, default: Int = 0): Int
    fun putInt(key: String, value: Int)
    fun getBool(key: String, default: Boolean = false): Boolean
    fun putBool(key: String, value: Boolean)
}

/** Plays the game's short sound effects (files under composeResources/files, e.g. "sfx_sprite_pop.wav"). */
expect object SoundPlayer {
    /** Loads the bytes of every effect once; called by the app at start. */
    fun preload(name: String, bytes: ByteArray)
    fun play(name: String, rate: Float = 1f, volume: Float = 1f)
}

/** Wall-clock milliseconds. */
expect fun nowMillis(): Long

/** Today's local date as "yyyy-MM-dd" (daily challenge key). */
expect fun todayKey(): String

/** Light haptic tap. */
expect fun hapticTap()
