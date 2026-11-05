package com.avfusionapps.game_2048.shared.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

actual object KeyValueStore {
    private val d = NSUserDefaults.standardUserDefaults
    actual fun getString(key: String): String? = d.stringForKey(key)
    actual fun putString(key: String, value: String) = d.setObject(value, key)
    actual fun getInt(key: String, default: Int): Int = if (d.objectForKey(key) == null) default else d.integerForKey(key).toInt()
    actual fun putInt(key: String, value: Int) = d.setInteger(value.toLong(), key)
    actual fun getBool(key: String, default: Boolean): Boolean = if (d.objectForKey(key) == null) default else d.boolForKey(key)
    actual fun putBool(key: String, value: Boolean) = d.setBool(value, key)
}

@OptIn(ExperimentalForeignApi::class)
actual object SoundPlayer {
    private val data = HashMap<String, NSData>()
    private val players = HashMap<String, MutableList<AVAudioPlayer>>()

    actual fun preload(name: String, bytes: ByteArray) {
        if (bytes.isEmpty()) return
        data[name] = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    }

    actual fun play(name: String, rate: Float, volume: Float) {
        val bytes = data[name] ?: return
        val pool = players.getOrPut(name) { mutableListOf() }
        val p = pool.firstOrNull { !it.playing } ?: AVAudioPlayer(data = bytes, error = null).also { it.enableRate = true; it.prepareToPlay(); if (pool.size < 4) pool.add(it) }
        p.rate = rate.coerceIn(0.5f, 2f)
        p.volume = volume
        p.currentTime = 0.0
        p.play()
    }
}

actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun todayKey(): String = NSDateFormatter().apply { dateFormat = "yyyy-MM-dd" }.stringFromDate(NSDate())

actual fun hapticTap() { UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight).impactOccurred() }
