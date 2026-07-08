package com.avfusionapps.game_2048.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SoundManager(private val context: Context) {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Int, Int>()
    private var isInitialized = false
    
    companion object {
        // Sound effect IDs
        const val SOUND_LEVEL_UP = 1
        const val SOUND_MERGE = 2
        const val SOUND_MOVE = 3
        const val SOUND_GAME_OVER = 4
        const val SOUND_BUTTON_CLICK = 5
        
        // Level up music mapping
        private val LEVEL_UP_MELODIES = mapOf(
            2 to listOf(523, 659, 784, 1047), // C5, E5, G5, C6 - Triumph
            3 to listOf(440, 554, 659, 880),  // A4, C#5, E5, A5 - Achievement
            4 to listOf(392, 493, 587, 784),  // G4, B4, D5, G5 - Victory
            5 to listOf(349, 440, 523, 698)  // F4, A4, C5, F5 - Glory
        )
    }
    
    init {
        initializeSoundPool()
    }
    
    private fun initializeSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
            
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
            
        isInitialized = true
    }
    
    suspend fun playLevelUpMusic(level: Int) {
        if (!isInitialized) return
        
        withContext(Dispatchers.IO) {
            try {
                val melody = LEVEL_UP_MELODIES[level] ?: LEVEL_UP_MELODIES[2]!!
                playMelody(melody, tempo = 200) // Fast tempo for excitement
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private suspend fun playMelody(notes: List<Int>, tempo: Long) {
        notes.forEach { frequency ->
            playTone(frequency, duration = tempo)
            delay(tempo / 2)
        }
        
        // Add a flourish at the end
        delay(100)
        playTone(notes.last() * 2, duration = tempo * 2)
    }
    
    private fun playTone(frequency: Int, duration: Long) {
        try {
            val sampleRate = 8000
            val numSamples = (duration * sampleRate / 1000).toInt()
            val samples = DoubleArray(numSamples)
            val buffer = ShortArray(numSamples)
            
            for (i in 0 until numSamples) {
                samples[i] = Math.sin(2 * Math.PI * i / (sampleRate / frequency))
                buffer[i] = (samples[i] * Short.MAX_VALUE).toInt().toShort()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun playSound(soundId: Int) {
        if (!isInitialized) return
        
        try {
            when (soundId) {
                SOUND_MERGE -> playMergeSound()
                SOUND_MOVE -> playMoveSound()
                SOUND_BUTTON_CLICK -> playClickSound()
                SOUND_GAME_OVER -> playGameOverSound()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun playMergeSound() {
        // Quick ascending tone
        playTone(400, 100)
        delay(50)
        playTone(600, 100)
    }
    
    private fun playMoveSound() {
        // Short click sound
        playTone(200, 50)
    }
    
    private fun playClickSound() {
        // Soft click
        playTone(300, 30)
    }
    
    private fun playGameOverSound() {
        // Descending sad tone
        playTone(500, 200)
        delay(100)
        playTone(400, 200)
        delay(100)
        playTone(300, 300)
    }
    
    fun release() {
        soundPool?.release()
        soundPool = null
        isInitialized = false
    }
    
    private fun delay(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}