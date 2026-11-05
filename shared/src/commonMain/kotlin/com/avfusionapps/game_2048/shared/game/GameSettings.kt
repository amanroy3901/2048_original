package com.avfusionapps.game_2048.shared.game

import com.avfusionapps.game_2048.shared.platform.KeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Settings and stats persisted in KeyValueStore (replaces the DataStore GameSettingsRepository). */
object GameSettingsRepository {
    const val STARTING_COINS = 120
    const val DEFAULT_PLAYER_NAME = "Player"
    const val DEFAULT_HIGH_SCORE = 0

    private const val K_NAME = "player_name"
    private const val K_HIGH = "high_score"
    private const val K_COINS = "coins"
    private const val K_COINS_INIT = "coins_init"
    private const val K_SOUND = "sound_enabled"
    private const val K_VIBRATION = "vibration_enabled"
    private const val K_REMINDERS = "reminders_enabled"
    private const val K_TUT_CLASSIC = "seen_tutorial_classic"
    private const val K_TUT_TA = "seen_tutorial_time_attack"
    private const val K_SEEN = "seen_sprites"
    private const val K_LEVEL = "current_level"
    private const val K_UNLOCKED = "unlocked_levels"

    private val _playerName = MutableStateFlow(KeyValueStore.getString(K_NAME) ?: DEFAULT_PLAYER_NAME)
    val playerNameFlow: StateFlow<String> = _playerName.asStateFlow()

    private val _highScore = MutableStateFlow(KeyValueStore.getInt(K_HIGH, DEFAULT_HIGH_SCORE))
    val highScoreFlow: StateFlow<Int> = _highScore.asStateFlow()

    private val _coins = MutableStateFlow(
        if (KeyValueStore.getBool(K_COINS_INIT)) KeyValueStore.getInt(K_COINS, STARTING_COINS) else {
            KeyValueStore.putBool(K_COINS_INIT, true); KeyValueStore.putInt(K_COINS, STARTING_COINS); STARTING_COINS
        }
    )
    val coinsFlow: StateFlow<Int> = _coins.asStateFlow()

    private val _sound = MutableStateFlow(KeyValueStore.getBool(K_SOUND, true))
    val soundEnabledFlow: StateFlow<Boolean> = _sound.asStateFlow()

    private val _vibration = MutableStateFlow(KeyValueStore.getBool(K_VIBRATION, true))
    val vibrationEnabledFlow: StateFlow<Boolean> = _vibration.asStateFlow()

    private val _reminders = MutableStateFlow(KeyValueStore.getBool(K_REMINDERS, true))
    val remindersEnabledFlow: StateFlow<Boolean> = _reminders.asStateFlow()

    private val _tutClassic = MutableStateFlow<Boolean?>(KeyValueStore.getBool(K_TUT_CLASSIC, false))
    val hasSeenClassicTutorialFlow: StateFlow<Boolean?> = _tutClassic.asStateFlow()

    private val _tutTa = MutableStateFlow<Boolean?>(KeyValueStore.getBool(K_TUT_TA, false))
    val hasSeenTimeAttackTutorialFlow: StateFlow<Boolean?> = _tutTa.asStateFlow()

    private val _seen = MutableStateFlow(
        (KeyValueStore.getString(K_SEEN) ?: "").split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    )
    val seenSpritesFlow: StateFlow<Set<Int>> = _seen.asStateFlow()

    /** The shared app always uses the Elemental Sprites look. */
    const val usesSprites: Boolean = true

    fun updatePlayerName(name: String) { _playerName.value = name; KeyValueStore.putString(K_NAME, name) }
    fun updateHighScoreIfHigher(score: Int) {
        if (score > _highScore.value) { _highScore.value = score; KeyValueStore.putInt(K_HIGH, score) }
    }
    fun addCoins(n: Int) { setCoins(_coins.value + n) }
    fun spendCoins(n: Int): Boolean {
        if (_coins.value < n) return false
        setCoins(_coins.value - n); return true
    }
    private fun setCoins(v: Int) { _coins.value = v; KeyValueStore.putInt(K_COINS, v) }
    fun updateSoundEnabled(b: Boolean) { _sound.value = b; KeyValueStore.putBool(K_SOUND, b) }
    fun updateVibrationEnabled(b: Boolean) { _vibration.value = b; KeyValueStore.putBool(K_VIBRATION, b) }
    fun updateRemindersEnabled(b: Boolean) { _reminders.value = b; KeyValueStore.putBool(K_REMINDERS, b) }
    fun updateHasSeenClassicTutorial(b: Boolean) { _tutClassic.value = b; KeyValueStore.putBool(K_TUT_CLASSIC, b) }
    fun updateHasSeenTimeAttackTutorial(b: Boolean) { _tutTa.value = b; KeyValueStore.putBool(K_TUT_TA, b) }
    fun markSpritesSeen(values: Set<Int>) {
        val all = _seen.value + values
        _seen.value = all
        KeyValueStore.putString(K_SEEN, all.joinToString(","))
    }

    fun loadCurrentLevel(): Int = KeyValueStore.getInt(K_LEVEL, 1)
    fun loadUnlockedLevels(): Set<Int> =
        (KeyValueStore.getString(K_UNLOCKED) ?: "1").split(",").mapNotNull { it.trim().toIntOrNull() }.toSet().ifEmpty { setOf(1) }
    fun saveLevels(current: Int, unlocked: Set<Int>) {
        KeyValueStore.putInt(K_LEVEL, current)
        KeyValueStore.putString(K_UNLOCKED, unlocked.sorted().joinToString(","))
    }
}

/** Level helpers (from LevelProgression). */
object LevelProgression {
    fun getLevelForTileValue(tileValue: Int): Int = when {
        tileValue < 128 -> 1
        tileValue >= 1024 -> 5
        tileValue >= 512 -> 4
        tileValue >= 256 -> 3
        else -> 2
    }
}

/** Time Attack best scores persisted in KeyValueStore (replaces TimeAttackRepository). */
object TimeAttackRepository {
    private const val K_HIGH = "ta_high_score"
    private const val K_BEST = "ta_best_time"
    private const val K_TOP = "ta_top_scores"
    const val MAX_SAVED_SCORES = 10

    private val _highScore = MutableStateFlow(KeyValueStore.getInt(K_HIGH, 0))
    val highScore: StateFlow<Int> = _highScore.asStateFlow()

    private val _best = MutableStateFlow((KeyValueStore.getString(K_BEST) ?: "0").toLongOrNull() ?: 0L)
    val bestTimeSurvived: StateFlow<Long> = _best.asStateFlow()

    private val _top = MutableStateFlow(decodeScores(KeyValueStore.getString(K_TOP) ?: ""))
    val topScores: StateFlow<List<TimeAttackScore>> = _top.asStateFlow()

    private fun decodeScores(s: String): List<TimeAttackScore> = s.split(";").mapNotNull { e ->
        val p = e.split(",")
        if (p.size != 3) null else {
            val sc = p[0].toIntOrNull(); val t = p[1].toLongOrNull(); val d = p[2].toLongOrNull()
            if (sc == null || t == null || d == null) null else TimeAttackScore(sc, t, d)
        }
    }

    fun saveTimeAttackHighScore(score: TimeAttackScore) {
        if (score.score > _highScore.value) { _highScore.value = score.score; KeyValueStore.putInt(K_HIGH, score.score) }
        if (score.timeSurvived > _best.value) { _best.value = score.timeSurvived; KeyValueStore.putString(K_BEST, score.timeSurvived.toString()) }
        val list = (_top.value + score).sortedByDescending { it.score }.take(MAX_SAVED_SCORES)
        _top.value = list
        KeyValueStore.putString(K_TOP, list.joinToString(";") { "${it.score},${it.timeSurvived},${it.date}" })
    }
}
