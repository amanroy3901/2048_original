package com.avfusionapps.game_2048.shared.game

import com.avfusionapps.game_2048.shared.platform.SoundPlayer
import com.avfusionapps.game_2048.shared.res.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.math.pow

/**
 * The Elemental Sprites sound set (composeResources/files/sfx_sprite_*.wav):
 * pops, slides, pitched merges, bonks, giggles, crunches and fanfares. Honours [enabled].
 */
object SpriteSfx {
    enum class Sound(val file: String) {
        POP("sfx_sprite_pop"), SLIDE("sfx_sprite_slide"), MERGE("sfx_sprite_merge"),
        BONK("sfx_sprite_bonk"), CHIME("sfx_sprite_chime"), COMBO("sfx_sprite_combo"),
        SAD("sfx_sprite_sad"), CRUNCH("sfx_sprite_crunch"), COIN("sfx_sprite_coin"),
        GIGGLE("sfx_sprite_giggle"), GRR("sfx_sprite_grr"), WHOOSH("sfx_sprite_whoosh"),
        FREEZE("sfx_sprite_freeze"), FANFARE("sfx_sprite_fanfare"), SHOT("sfx_sprite_shot"),
        IMPACT("sfx_sprite_impact"), WAKE("sfx_sprite_wake")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var loaded = false

    /** Sound on/off (the Sound setting); defaults to the stored "sound_enabled" value. */
    var enabled: Boolean = GameSettingsRepository.soundEnabledFlow.value

    /** Reads every effect's wav and hands it to [SoundPlayer]; call once at app start. */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun loadAll() {
        if (loaded) return
        loaded = true
        Sound.entries.forEach { s ->
            runCatching { SoundPlayer.preload(s.file, Res.readBytes("files/${s.file}.wav")) }
        }
    }

    /** Plays [sound] after [delayMs], at [rate] (1 = as recorded). */
    fun play(sound: Sound, rate: Float = 1f, volume: Float = 1f, delayMs: Long = 0) {
        if (!enabled) return
        val r = rate.coerceIn(0.5f, 2f)
        if (delayMs > 0) {
            scope.launch {
                delay(delayMs)
                if (enabled) SoundPlayer.play(sound.file, r, volume)
            }
        } else {
            SoundPlayer.play(sound.file, r, volume)
        }
    }

    /** A merge's chime rises with the sprite made (Drip low, the Ember King high), like the prototype. */
    fun merge(value: Int, delayMs: Long = 0) {
        val i = (kotlin.math.log2(value.coerceAtLeast(2).toFloat()) - 1f).coerceIn(0f, 10f)
        play(Sound.MERGE, rate = 2f.pow(i / 9f) * 0.85f, delayMs = delayMs)
    }
}
