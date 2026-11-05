package com.avfusionapps.game_2048.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.data.GameSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.pow

/**
 * The Elemental Sprites sound set, synthesised from the prototype's tone recipes (res/raw/sfx_sprite_*):
 * pops, slides, pitched merges, bonks, giggles, crunches and fanfares. Honours the Sound setting.
 */
object SpriteSfx {
    enum class Sound(val res: Int) {
        POP(R.raw.sfx_sprite_pop), SLIDE(R.raw.sfx_sprite_slide), MERGE(R.raw.sfx_sprite_merge),
        BONK(R.raw.sfx_sprite_bonk), CHIME(R.raw.sfx_sprite_chime), COMBO(R.raw.sfx_sprite_combo),
        SAD(R.raw.sfx_sprite_sad), CRUNCH(R.raw.sfx_sprite_crunch), COIN(R.raw.sfx_sprite_coin),
        GIGGLE(R.raw.sfx_sprite_giggle), GRR(R.raw.sfx_sprite_grr), WHOOSH(R.raw.sfx_sprite_whoosh),
        FREEZE(R.raw.sfx_sprite_freeze), FANFARE(R.raw.sfx_sprite_fanfare), SHOT(R.raw.sfx_sprite_shot),
        IMPACT(R.raw.sfx_sprite_impact), WAKE(R.raw.sfx_sprite_wake)
    }

    private var pool: SoundPool? = null
    private val ids = HashMap<Sound, Int>()
    private val ready = HashSet<Int>()
    @Volatile private var enabled = true
    private val handler = Handler(Looper.getMainLooper())

    fun init(context: Context) {
        if (pool != null) return
        val p = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .build()
        p.setOnLoadCompleteListener { _, id, status -> if (status == 0) synchronized(ready) { ready.add(id) } }
        val app = context.applicationContext
        Sound.entries.forEach { ids[it] = p.load(app, it.res, 1) }
        pool = p
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            GameSettingsRepository(app).soundEnabledFlow.collect { enabled = it }
        }
    }

    /** Plays [sound] after [delayMs], at [rate] (1 = as recorded). */
    fun play(sound: Sound, rate: Float = 1f, volume: Float = 1f, delayMs: Long = 0) {
        if (!enabled) return
        val p = pool ?: return
        val id = ids[sound] ?: return
        val go = Runnable { if (synchronized(ready) { id in ready }) p.play(id, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f)) }
        if (delayMs > 0) handler.postDelayed(go, delayMs) else go.run()
    }

    /** A merge's chime rises with the sprite made (Drip low, the Ember King high), like the prototype. */
    fun merge(value: Int, delayMs: Long = 0) {
        val i = (kotlin.math.log2(value.coerceAtLeast(2).toFloat()) - 1f).coerceIn(0f, 10f)
        play(Sound.MERGE, rate = 2f.pow(i / 9f) * 0.85f, delayMs = delayMs)
    }
}
