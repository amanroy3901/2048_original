package com.avfusionapps.game_2048.shared.platform

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("StaticFieldLeak")
object AndroidPlatform {
    lateinit var context: Context
    val prefs: SharedPreferences by lazy { context.getSharedPreferences("shared_game", Context.MODE_PRIVATE) }
}

actual object KeyValueStore {
    actual fun getString(key: String): String? = AndroidPlatform.prefs.getString(key, null)
    actual fun putString(key: String, value: String) { AndroidPlatform.prefs.edit().putString(key, value).apply() }
    actual fun getInt(key: String, default: Int): Int = AndroidPlatform.prefs.getInt(key, default)
    actual fun putInt(key: String, value: Int) { AndroidPlatform.prefs.edit().putInt(key, value).apply() }
    actual fun getBool(key: String, default: Boolean): Boolean = AndroidPlatform.prefs.getBoolean(key, default)
    actual fun putBool(key: String, value: Boolean) { AndroidPlatform.prefs.edit().putBoolean(key, value).apply() }
}

actual object SoundPlayer {
    actual fun preload(name: String, bytes: ByteArray) {}
    actual fun play(name: String, rate: Float, volume: Float) {}
}

actual fun nowMillis(): Long = System.currentTimeMillis()
actual fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
actual fun hapticTap() {}
