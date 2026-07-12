package com.avfusionapps.game_2048.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.avfusionapps.game_2048.R

/**
 * Plays short game sound effects through [SoundPool] from bundled res/raw assets.
 *
 * Samples are loaded once at construction. [playSound] is fire-and-forget and returns
 * immediately, so it is safe to call from any thread. Call [release] when done (e.g. onDispose)
 * to free the underlying SoundPool.
 *
 * The public API (constructor, SOUND_* ids, [playSound], [release]) is intentionally stable so
 * existing callers keep working. To add a sound: drop a .wav/.ogg into res/raw, add a SOUND_* id
 * and a load() entry, then map it in [playSound].
 */
class SoundManager(context: Context) {

    private val soundPool: SoundPool
    private val sampleIds: Map<Int, Int>
    private val loaded = mutableSetOf<Int>()

    companion object {
        // Sound effect IDs (values are arbitrary handles; keep them stable for callers).
        const val SOUND_LEVEL_UP = 1
        const val SOUND_MERGE = 2
        const val SOUND_MOVE = 3
        const val SOUND_GAME_OVER = 4
        const val SOUND_BUTTON_CLICK = 5
    }

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attributes)
            .build()

        // Track which samples have finished loading so we never play an unready one.
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded.add(sampleId)
        }

        val app = context.applicationContext
        sampleIds = mapOf(
            SOUND_MOVE to soundPool.load(app, R.raw.sfx_move, 1),
            SOUND_MERGE to soundPool.load(app, R.raw.sfx_merge, 1),
            SOUND_LEVEL_UP to soundPool.load(app, R.raw.sfx_level_up, 1),
            SOUND_GAME_OVER to soundPool.load(app, R.raw.sfx_game_over, 1),
            SOUND_BUTTON_CLICK to soundPool.load(app, R.raw.sfx_button, 1)
        )
    }

    /** Plays the given SOUND_* effect. No-ops if the sample isn't loaded yet. */
    fun playSound(soundId: Int) {
        val sample = sampleIds[soundId] ?: return
        if (sample !in loaded) return
        soundPool.play(sample, 1f, 1f, 1, 0, 1f)
    }

    /** Releases the SoundPool and its samples. */
    fun release() {
        soundPool.release()
        loaded.clear()
    }
}
