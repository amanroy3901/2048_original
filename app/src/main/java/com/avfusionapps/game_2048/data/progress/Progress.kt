package com.avfusionapps.game_2048.data.progress

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.avfusionapps.game_2048.data.GameSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import kotlin.random.Random

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(name = "player_progress")

/** Where a game event came from. */
enum class GameMode { CLASSIC, TIME_ATTACK, SHOOTER }

/** Lifetime counters behind achievements, the stats page and the daily challenge. */
data class PlayerStats(
    val gamesPlayed: Int = 0,
    val classicGames: Int = 0,
    val timeAttackGames: Int = 0,
    val shooterGames: Int = 0,
    val merges: Int = 0,
    val bestCombo: Int = 0,
    val biggestTile: Int = 0,
    val shooterBestTile: Int = 0,
    val bestChain: Int = 0,
    val bestScore: Int = 0,
    val powerUpsUsed: Int = 0,
    val rocksSmashed: Int = 0,
    val pokes: Int = 0,
    val shots: Int = 0,
    val dailyCompleted: Int = 0,
    val dailyStreak: Int = 0,
    val bestDailyStreak: Int = 0,
    val spritesFound: Int = 0
)

enum class Tier(val coins: Int) { BRONZE(25), SILVER(50), GOLD(100) }

/** One achievement: shown with its sprite on a medal; earned when [done] holds for the stats. */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val sprite: Int,
    val tier: Tier,
    val target: Int,
    val value: (PlayerStats) -> Int
) {
    fun done(s: PlayerStats) = value(s) >= target
    fun progress(s: PlayerStats) = (value(s).toFloat() / target).coerceIn(0f, 1f)
}

object Achievements {
    val all: List<Achievement> = listOf(
        Achievement("first_merge", "First Spark", "Make your first merge", 2, Tier.BRONZE, 1) { it.merges },
        Achievement("merges_100", "Busy Bee", "Merge 100 times", 4, Tier.BRONZE, 100) { it.merges },
        Achievement("merges_1000", "Merge Maestro", "Merge 1,000 times", 128, Tier.SILVER, 1000) { it.merges },
        Achievement("merges_10000", "Endless Evolution", "Merge 10,000 times", 1024, Tier.GOLD, 10000) { it.merges },
        Achievement("tile_64", "Light My Fire", "Make Blaze (64)", 64, Tier.BRONZE, 64) { it.biggestTile },
        Achievement("tile_256", "Making Waves", "Make Splash (256)", 256, Tier.SILVER, 256) { it.biggestTile },
        Achievement("tile_512", "Star Power", "Make Starla (512)", 512, Tier.SILVER, 512) { it.biggestTile },
        Achievement("tile_1024", "Crystal Clear", "Make Prism (1024)", 1024, Tier.GOLD, 1024) { it.biggestTile },
        Achievement("tile_2048", "Long Live the King", "Raise the Ember King (2048)", 2048, Tier.GOLD, 2048) { it.biggestTile },
        Achievement("combo_3", "On a Roll", "Reach a x3 combo", 8, Tier.BRONZE, 3) { it.bestCombo },
        Achievement("combo_5", "Unstoppable", "Reach the x5 combo", 32, Tier.SILVER, 5) { it.bestCombo },
        Achievement("chain_3", "Chain Reaction", "Chain 3 merges with one shot", 16, Tier.BRONZE, 3) { it.bestChain },
        Achievement("chain_5", "Domino Effect", "Chain 5 merges with one shot", 256, Tier.GOLD, 5) { it.bestChain },
        Achievement("shooter_256", "Sharpshooter", "Make a 256 in Neon Shooter", 256, Tier.SILVER, 256) { it.shooterBestTile },
        Achievement("score_10000", "Five Figures", "Score 10,000 in one game", 128, Tier.SILVER, 10000) { it.bestScore },
        Achievement("score_50000", "High Roller", "Score 50,000 in one game", 2048, Tier.GOLD, 50000) { it.bestScore },
        Achievement("rock_1", "Rock Breaker", "Smash a Shadow Rock", -1, Tier.BRONZE, 1) { it.rocksSmashed },
        Achievement("power_25", "Gadgeteer", "Use 25 power-ups", 16, Tier.BRONZE, 25) { it.powerUpsUsed },
        Achievement("poke_20", "Tickle Monster", "Poke sprites 20 times", 4, Tier.BRONZE, 20) { it.pokes },
        Achievement("games_10", "Regular", "Play 10 games", 2, Tier.BRONZE, 10) { it.gamesPlayed },
        Achievement("games_100", "Sprite Keeper", "Play 100 games", 64, Tier.SILVER, 100) { it.gamesPlayed },
        Achievement("collect_11", "Gotta Merge 'Em All", "Find all 11 sprites", 2048, Tier.GOLD, 11) { it.spritesFound },
        Achievement("daily_1", "Daily Spark", "Finish a daily challenge", 8, Tier.BRONZE, 1) { it.dailyCompleted },
        Achievement("streak_7", "Week of Wonders", "Keep a 7-day challenge streak", 512, Tier.GOLD, 7) { it.bestDailyStreak }
    )
}

enum class DailyKind { REACH_TILE, SCORE, MERGES, COMBO, SHOOTER_CHAIN, SHOOTER_TILE }

/** Today's challenge, the same for everyone on the same day. */
data class DailyChallenge(
    val date: String,
    val kind: DailyKind,
    val target: Int,
    val progress: Int,
    val claimed: Boolean,
    val streak: Int
) {
    val done get() = progress >= target
    val reward get() = 50 + 10 * streak.coerceAtMost(7)
    val title: String
        get() = when (kind) {
            DailyKind.REACH_TILE -> "Make a $target in Classic"
            DailyKind.SCORE -> "Score ${"%,d".format(target)} in one game"
            DailyKind.MERGES -> "Merge $target times today"
            DailyKind.COMBO -> "Reach a x$target combo in Classic"
            DailyKind.SHOOTER_CHAIN -> "Chain $target merges with one shot"
            DailyKind.SHOOTER_TILE -> "Make a $target in Neon Shooter"
        }
    /** The mode that best fits the challenge, for its Play button. */
    val mode: GameMode
        get() = when (kind) {
            DailyKind.SHOOTER_CHAIN, DailyKind.SHOOTER_TILE -> GameMode.SHOOTER
            else -> GameMode.CLASSIC
        }

    companion object {
        fun forDate(date: LocalDate): Pair<DailyKind, Int> {
            val rnd = Random(date.toEpochDay() * 7919L + 2048L)
            val kind = DailyKind.entries[rnd.nextInt(DailyKind.entries.size)]
            val target = when (kind) {
                DailyKind.REACH_TILE -> listOf(128, 256, 256, 512)[rnd.nextInt(4)]
                DailyKind.SCORE -> listOf(1500, 2500, 4000)[rnd.nextInt(3)]
                DailyKind.MERGES -> listOf(40, 60, 100)[rnd.nextInt(3)]
                DailyKind.COMBO -> listOf(3, 3, 4)[rnd.nextInt(3)]
                DailyKind.SHOOTER_CHAIN -> listOf(2, 3)[rnd.nextInt(2)]
                DailyKind.SHOOTER_TILE -> listOf(64, 128, 128)[rnd.nextInt(3)]
            }
            return kind to target
        }
    }
}

/** Something to celebrate on screen: an achievement or the daily challenge. */
sealed interface ProgressEvent {
    data class Unlocked(val achievement: Achievement) : ProgressEvent
    data class DailyDone(val challenge: DailyChallenge) : ProgressEvent
}

/**
 * The player's long-term progress: lifetime stats, achievements and the daily challenge.
 * Game view models report events here; screens read the flows. All writes are serialised.
 */
object Progress {
    private val STATS = listOf(
        "games", "classic_games", "ta_games", "shooter_games", "merges", "best_combo", "biggest_tile", "shooter_best",
        "best_chain", "best_score", "power_ups", "rocks", "pokes", "shots", "daily_done", "daily_streak", "best_streak"
    ).associateWith { intPreferencesKey("stat_$it") }
    private val UNLOCKED = stringSetPreferencesKey("achievements")
    private val DAILY_DATE = stringPreferencesKey("daily_date")
    private val DAILY_PROGRESS = intPreferencesKey("daily_progress")
    private val DAILY_CLAIMED = booleanPreferencesKey("daily_claimed")
    private val LAST_DAILY_DONE = stringPreferencesKey("last_daily_done")
    private val SEEN_WELCOME = booleanPreferencesKey("seen_elemental_welcome")
    private val AUTH_PROMPT_DONE = booleanPreferencesKey("auth_prompt_done")

    private lateinit var app: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val started = java.util.concurrent.atomic.AtomicBoolean(false)

    private val _stats = MutableStateFlow(PlayerStats())
    val stats: StateFlow<PlayerStats> = _stats.asStateFlow()
    private val _unlocked = MutableStateFlow<Set<String>>(emptySet())
    val unlocked: StateFlow<Set<String>> = _unlocked.asStateFlow()
    private val _daily = MutableStateFlow<DailyChallenge?>(null)
    val daily: StateFlow<DailyChallenge?> = _daily.asStateFlow()
    /** null until loaded. */
    private val _seenWelcome = MutableStateFlow<Boolean?>(null)
    val seenWelcome: StateFlow<Boolean?> = _seenWelcome.asStateFlow()
    /** The home sign-in prompt was answered (signed in or chose guest); null until loaded. */
    private val _authPromptDone = MutableStateFlow<Boolean?>(null)
    val authPromptDone: StateFlow<Boolean?> = _authPromptDone.asStateFlow()
    private val _events = MutableSharedFlow<ProgressEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ProgressEvent> = _events.asSharedFlow()

    fun init(context: Context) {
        if (!started.compareAndSet(false, true)) return
        app = context.applicationContext
        scope.launch { mutex.withLock { load() } }
    }

    private val store get() = app.progressDataStore

    private suspend fun load() {
        val p = store.data.first()
        _stats.value = p.toStats(GameSettingsRepository(app).seenSpritesFlow.first().count { it in 2..2048 })
        _unlocked.value = p[UNLOCKED] ?: emptySet()
        _seenWelcome.value = p[SEEN_WELCOME] ?: false
        _authPromptDone.value = p[AUTH_PROMPT_DONE] ?: false
        _daily.value = refreshDaily()
    }

    private fun Preferences.stat(k: String) = this[STATS.getValue(k)] ?: 0

    private fun Preferences.toStats(found: Int) = PlayerStats(
        gamesPlayed = stat("games"), classicGames = stat("classic_games"), timeAttackGames = stat("ta_games"),
        shooterGames = stat("shooter_games"), merges = stat("merges"), bestCombo = stat("best_combo"),
        biggestTile = stat("biggest_tile"), shooterBestTile = stat("shooter_best"), bestChain = stat("best_chain"),
        bestScore = stat("best_score"), powerUpsUsed = stat("power_ups"), rocksSmashed = stat("rocks"), pokes = stat("pokes"),
        shots = stat("shots"), dailyCompleted = stat("daily_done"), dailyStreak = stat("daily_streak"),
        bestDailyStreak = stat("best_streak"), spritesFound = found
    )

    /** Today's challenge, rolling over (and breaking a lapsed streak) when the date changes. */
    private suspend fun refreshDaily(): DailyChallenge {
        val today = LocalDate.now()
        val (kind, target) = DailyChallenge.forDate(today)
        var result: DailyChallenge? = null
        store.edit { p ->
            if (p[DAILY_DATE] != today.toString()) {
                p[DAILY_DATE] = today.toString()
                p[DAILY_PROGRESS] = 0
                p[DAILY_CLAIMED] = false
            }
            val last = p[LAST_DAILY_DONE]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            if (last == null || last.isBefore(today.minusDays(1))) p[STATS.getValue("daily_streak")] = 0
            result = DailyChallenge(today.toString(), kind, target, p[DAILY_PROGRESS] ?: 0, p[DAILY_CLAIMED] ?: false, p.stat("daily_streak"))
        }
        return result!!
    }

    private fun update(block: (MutablePreferencesEditor) -> Unit) {
        if (!started.get()) return
        scope.launch {
            mutex.withLock {
                if (_daily.value?.date != LocalDate.now().toString()) _daily.value = refreshDaily()
                val daily = _daily.value
                var dailyNow: DailyChallenge? = null
                store.edit { p ->
                    val e = MutablePreferencesEditor(p, daily)
                    block(e)
                    if (daily != null && e.dailyProgress != daily.progress) {
                        p[DAILY_PROGRESS] = e.dailyProgress
                        dailyNow = daily.copy(progress = e.dailyProgress)
                    }
                }
                val found = GameSettingsRepository(app).seenSpritesFlow.first().count { it in 2..2048 }
                val p = store.data.first()
                val stats = p.toStats(found)
                _stats.value = stats
                dailyNow?.let { now ->
                    _daily.value = now
                    if (now.done && !(daily?.done ?: false)) _events.tryEmit(ProgressEvent.DailyDone(now))
                }
                checkAchievements(stats)
            }
        }
    }

    private suspend fun checkAchievements(stats: PlayerStats) {
        val have = _unlocked.value
        val fresh = Achievements.all.filter { it.id !in have && it.done(stats) }
        if (fresh.isEmpty()) return
        store.edit { it[UNLOCKED] = have + fresh.map { a -> a.id } }
        _unlocked.value = have + fresh.map { it.id }
        GameSettingsRepository(app).addCoins(fresh.sumOf { it.tier.coins })
        fresh.forEach { _events.tryEmit(ProgressEvent.Unlocked(it)) }
    }

    /** Collects the daily reward once the challenge is done; returns the coins paid. */
    suspend fun claimDaily(): Int {
        if (!started.get()) return 0
        return mutex.withLock {
            val d = _daily.value ?: return@withLock 0
            if (!d.done || d.claimed) return@withLock 0
            val reward = d.reward
            store.edit { p ->
                p[DAILY_CLAIMED] = true
                p[LAST_DAILY_DONE] = d.date
                val streak = p.stat("daily_streak") + 1
                p[STATS.getValue("daily_streak")] = streak
                p[STATS.getValue("best_streak")] = maxOf(p.stat("best_streak"), streak)
                p[STATS.getValue("daily_done")] = p.stat("daily_done") + 1
            }
            GameSettingsRepository(app).addCoins(reward)
            val p = store.data.first()
            _daily.value = d.copy(claimed = true, streak = p.stat("daily_streak"))
            val stats = p.toStats(_stats.value.spritesFound)
            _stats.value = stats
            checkAchievements(stats)
            reward
        }
    }

    fun markAuthPromptDone() {
        if (!started.get()) return
        _authPromptDone.value = true
        scope.launch { store.edit { it[AUTH_PROMPT_DONE] = true } }
    }

    fun markWelcomeSeen() {
        if (!started.get()) return
        _seenWelcome.value = true
        scope.launch { store.edit { it[SEEN_WELCOME] = true } }
    }

    // ── Events from the games ──

    fun gameStarted(mode: GameMode) = update { e ->
        e.add("games", 1)
        e.add(when (mode) { GameMode.CLASSIC -> "classic_games"; GameMode.TIME_ATTACK -> "ta_games"; GameMode.SHOOTER -> "shooter_games" }, 1)
    }

    /** One move or shot's merges: [values] are the tiles made. */
    fun merged(mode: GameMode, values: List<Int>) {
        if (values.isEmpty()) return
        update { e ->
            e.add("merges", values.size)
            val top = values.max()
            if (mode == GameMode.SHOOTER) e.max("shooter_best", top) else e.max("biggest_tile", top)
            when (e.daily?.kind) {
                DailyKind.MERGES -> e.dailyProgress += values.size
                DailyKind.REACH_TILE -> if (mode == GameMode.CLASSIC) e.dailyProgress = maxOf(e.dailyProgress, top)
                DailyKind.SHOOTER_TILE -> if (mode == GameMode.SHOOTER) e.dailyProgress = maxOf(e.dailyProgress, top)
                else -> Unit
            }
        }
    }

    fun combo(n: Int) = update { e ->
        e.max("best_combo", n)
        if (e.daily?.kind == DailyKind.COMBO) e.dailyProgress = maxOf(e.dailyProgress, n)
    }

    fun chain(n: Int) = update { e ->
        e.max("best_chain", n)
        if (e.daily?.kind == DailyKind.SHOOTER_CHAIN) e.dailyProgress = maxOf(e.dailyProgress, n)
    }

    fun score(n: Int) = update { e ->
        e.max("best_score", n)
        if (e.daily?.kind == DailyKind.SCORE) e.dailyProgress = maxOf(e.dailyProgress, n)
    }

    fun powerUpUsed() = update { it.add("power_ups", 1) }
    fun rockSmashed() = update { it.add("rocks", 1) }
    fun poked() = update { it.add("pokes", 1) }
    fun shot() = update { it.add("shots", 1) }
    /** Re-checks the collection achievements after new sprites are found. */
    fun spritesChanged() = update { }

    /** A small editor so event handlers read and bump counters in one transaction. */
    class MutablePreferencesEditor internal constructor(private val p: androidx.datastore.preferences.core.MutablePreferences, val daily: DailyChallenge?) {
        var dailyProgress: Int = daily?.progress ?: 0
        fun add(k: String, n: Int) { val key = STATS.getValue(k); p[key] = (p[key] ?: 0) + n }
        fun max(k: String, n: Int) { val key = STATS.getValue(k); p[key] = maxOf(p[key] ?: 0, n) }
    }
}
