package com.avfusionapps.game_2048.shared.game

import com.avfusionapps.game_2048.shared.platform.KeyValueStore
import com.avfusionapps.game_2048.shared.platform.Telemetry
import com.avfusionapps.game_2048.shared.platform.todayKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

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

/** "1,234,567" (the Android "%,d"). */
fun formatThousands(n: Int): String {
    val s = kotlin.math.abs(n.toLong()).toString()
    val sb = StringBuilder()
    s.forEachIndexed { i, c ->
        if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
        sb.append(c)
    }
    return if (n < 0) "-$sb" else sb.toString()
}

/** Days since 1970-01-01 for a "yyyy-MM-dd" key, or null if it doesn't parse. */
internal fun epochDayOf(key: String): Long? {
    val parts = key.split("-")
    if (parts.size != 3) return null
    val y0 = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    // Howard Hinnant's days_from_civil.
    val y = if (m <= 2) y0 - 1 else y0
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val doy = (153 * (m + (if (m > 2) -3 else 9)) + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era.toLong() * 146097L + doe - 719468L
}

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
            DailyKind.SCORE -> "Score ${formatThousands(target)} in one game"
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
        /** [date] is a "yyyy-MM-dd" key (see todayKey()). */
        fun forDate(date: String): Pair<DailyKind, Int> {
            val rnd = Random((epochDayOf(date) ?: 0L) * 7919L + 2048L)
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
 * The player's long-term progress: lifetime stats, achievements, coins and the daily challenge.
 * Game view models report events here; screens read the flows. All writes are serialised.
 * Persisted in [KeyValueStore] under "progress_*" keys.
 */
object Progress {
    private val STATS = listOf(
        "games", "classic_games", "ta_games", "shooter_games", "merges", "best_combo", "biggest_tile", "shooter_best",
        "best_chain", "best_score", "power_ups", "rocks", "pokes", "shots", "daily_done", "daily_streak", "best_streak"
    ).associateWith { "progress_stat_$it" }
    private const val UNLOCKED = "progress_achievements" // comma-separated ids
    private const val DAILY_DATE = "progress_daily_date"
    private const val DAILY_PROGRESS = "progress_daily_progress"
    private const val DAILY_CLAIMED = "progress_daily_claimed"
    private const val LAST_DAILY_DONE = "progress_last_daily_done"
    private const val SEEN_WELCOME = "progress_seen_elemental_welcome"
    private const val AUTH_PROMPT_DONE = "progress_auth_prompt_done"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private var started = false

    private val _stats = MutableStateFlow(PlayerStats())
    val stats: StateFlow<PlayerStats> = _stats.asStateFlow()
    private val _unlocked = MutableStateFlow<Set<String>>(emptySet())
    val unlocked: StateFlow<Set<String>> = _unlocked.asStateFlow()
    private val _daily = MutableStateFlow<DailyChallenge?>(null)
    val daily: StateFlow<DailyChallenge?> = _daily.asStateFlow()
    /** null until loaded. */
    private val _seenWelcome = MutableStateFlow<Boolean?>(null)
    val seenWelcome: StateFlow<Boolean?> = _seenWelcome.asStateFlow()
    /** The home sign-in prompt was answered; null until loaded. */
    private val _authPromptDone = MutableStateFlow<Boolean?>(null)
    val authPromptDone: StateFlow<Boolean?> = _authPromptDone.asStateFlow()
    private val _events = MutableSharedFlow<ProgressEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ProgressEvent> = _events.asSharedFlow()

    /** The coin balance (shared by every mode; lives in [GameSettingsRepository]). */
    val coins: StateFlow<Int> get() = GameSettingsRepository.coinsFlow

    /** Loads everything synchronously from [KeyValueStore]; safe to call more than once. */
    fun init() {
        if (started) return
        started = true
        _unlocked.value = readSet(UNLOCKED)
        _seenWelcome.value = KeyValueStore.getBool(SEEN_WELCOME, false)
        _authPromptDone.value = KeyValueStore.getBool(AUTH_PROMPT_DONE, false)
        _daily.value = refreshDaily()
        _stats.value = readStats()
    }

    private fun readSet(key: String): Set<String> =
        KeyValueStore.getString(key)?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()


    private fun stat(k: String) = KeyValueStore.getInt(STATS.getValue(k), 0)
    private fun putStat(k: String, v: Int) = KeyValueStore.putInt(STATS.getValue(k), v)

    private fun spritesFound() = GameSettingsRepository.seenSpritesFlow.value.count { it in 2..2048 }

    private fun readStats() = PlayerStats(
        gamesPlayed = stat("games"), classicGames = stat("classic_games"), timeAttackGames = stat("ta_games"),
        shooterGames = stat("shooter_games"), merges = stat("merges"), bestCombo = stat("best_combo"),
        biggestTile = stat("biggest_tile"), shooterBestTile = stat("shooter_best"), bestChain = stat("best_chain"),
        bestScore = stat("best_score"), powerUpsUsed = stat("power_ups"), rocksSmashed = stat("rocks"), pokes = stat("pokes"),
        shots = stat("shots"), dailyCompleted = stat("daily_done"), dailyStreak = stat("daily_streak"),
        bestDailyStreak = stat("best_streak"), spritesFound = spritesFound()
    )

    /** Today's challenge, rolling over (and breaking a lapsed streak) when the date changes. */
    private fun refreshDaily(): DailyChallenge {
        val today = todayKey()
        val (kind, target) = DailyChallenge.forDate(today)
        if (KeyValueStore.getString(DAILY_DATE) != today) {
            KeyValueStore.putString(DAILY_DATE, today)
            KeyValueStore.putInt(DAILY_PROGRESS, 0)
            KeyValueStore.putBool(DAILY_CLAIMED, false)
        }
        val last = KeyValueStore.getString(LAST_DAILY_DONE)?.let { epochDayOf(it) }
        val todayDay = epochDayOf(today) ?: 0L
        if (last == null || last < todayDay - 1) putStat("daily_streak", 0)
        return DailyChallenge(
            today, kind, target, KeyValueStore.getInt(DAILY_PROGRESS, 0),
            KeyValueStore.getBool(DAILY_CLAIMED, false), stat("daily_streak")
        )
    }

    private fun update(block: (MutablePreferencesEditor) -> Unit) {
        if (!started) init()
        scope.launch {
            mutex.withLock {
                if (_daily.value?.date != todayKey()) _daily.value = refreshDaily()
                val daily = _daily.value
                var dailyNow: DailyChallenge? = null
                val e = MutablePreferencesEditor(daily)
                block(e)
                if (daily != null && e.dailyProgress != daily.progress) {
                    KeyValueStore.putInt(DAILY_PROGRESS, e.dailyProgress)
                    dailyNow = daily.copy(progress = e.dailyProgress)
                }
                val stats = readStats()
                _stats.value = stats
                dailyNow?.let { now ->
                    _daily.value = now
                    if (now.done && !(daily?.done ?: false)) {
                        _events.tryEmit(ProgressEvent.DailyDone(now))
                        Telemetry.event("daily_challenge_done", "kind" to now.kind.name.lowercase(), "target" to now.target)
                    }
                }
                checkAchievements(stats)
            }
        }
    }

    private fun checkAchievements(stats: PlayerStats) {
        val have = _unlocked.value
        val fresh = Achievements.all.filter { it.id !in have && it.done(stats) }
        if (fresh.isEmpty()) return
        val all = have + fresh.map { a -> a.id }
        KeyValueStore.putString(UNLOCKED, all.joinToString(","))
        _unlocked.value = all
        addCoins(fresh.sumOf { it.tier.coins })
        fresh.forEach {
            _events.tryEmit(ProgressEvent.Unlocked(it))
            Telemetry.event("achievement_unlocked", "achievement_id" to it.id, "tier" to it.tier.name.lowercase())
        }
    }

    // ── Coins (delegated to GameSettingsRepository) ──

    fun addCoins(delta: Int) = GameSettingsRepository.addCoins(delta)

    /** Takes [cost] coins if the player has them; returns whether it did. */
    fun spendCoins(cost: Int): Boolean = GameSettingsRepository.spendCoins(cost)

    /** Collects the daily reward once the challenge is done; returns the coins paid. */
    suspend fun claimDaily(): Int {
        if (!started) init()
        return mutex.withLock {
            val d = _daily.value ?: return@withLock 0
            if (!d.done || d.claimed) return@withLock 0
            val reward = d.reward
            KeyValueStore.putBool(DAILY_CLAIMED, true)
            KeyValueStore.putString(LAST_DAILY_DONE, d.date)
            val streak = stat("daily_streak") + 1
            putStat("daily_streak", streak)
            putStat("best_streak", maxOf(stat("best_streak"), streak))
            putStat("daily_done", stat("daily_done") + 1)
            addCoins(reward)
            Telemetry.event("daily_challenge_claimed", "reward" to reward, "streak" to streak)
            _daily.value = d.copy(claimed = true, streak = stat("daily_streak"))
            val stats = readStats()
            _stats.value = stats
            checkAchievements(stats)
            reward
        }
    }

    fun markAuthPromptDone() {
        _authPromptDone.value = true
        KeyValueStore.putBool(AUTH_PROMPT_DONE, true)
    }

    fun markWelcomeSeen() {
        _seenWelcome.value = true
        KeyValueStore.putBool(SEEN_WELCOME, true)
    }

    // ── Events from the games ──

    fun gameStarted(mode: GameMode) = update { e ->
        Telemetry.event("game_start", "mode" to mode.name.lowercase())
        Telemetry.breadcrumb("game_start ${mode.name.lowercase()}")
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
    class MutablePreferencesEditor internal constructor(val daily: DailyChallenge?) {
        var dailyProgress: Int = daily?.progress ?: 0
        fun add(k: String, n: Int) { putStat(k, stat(k) + n) }
        fun max(k: String, n: Int) { putStat(k, maxOf(stat(k), n)) }
    }
}
