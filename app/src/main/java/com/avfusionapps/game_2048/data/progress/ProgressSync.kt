package com.avfusionapps.game_2048.data.progress

import android.content.Context
import android.util.Log
import com.avfusionapps.game_2048.data.GameSettingsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Keeps a signed-in player's progress in Firestore (player_progress/{uid}): lifetime stats, medals, coins and
 * the sprites found. On sign-in the cloud copy is merged in (higher counters win, medals and sprites combine);
 * after that, changes are pushed a few seconds after they settle. Guests stay local only.
 */
object ProgressSync {
    private const val TAG = "ProgressSync"
    private const val COLLECTION = "player_progress"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var pulledFor: String? = null
    @Volatile private var started = false

    @OptIn(FlowPreview::class)
    fun start(context: Context) {
        if (started) return
        started = true
        val settings = GameSettingsRepository(context.applicationContext)
        val auth = FirebaseAuth.getInstance()
        auth.addAuthStateListener { a ->
            val uid = a.currentUser?.uid ?: run { pulledFor = null; return@addAuthStateListener }
            if (pulledFor != uid) scope.launch { pull(uid, settings) }
        }
        scope.launch {
            combine(Progress.stats, Progress.unlocked, settings.coinsFlow, settings.seenSpritesFlow) { a, b, c, d -> listOf(a, b, c, d) }
                .drop(1)
                .debounce(4000)
                .collect { push(settings) }
        }
    }

    private suspend fun pull(uid: String, settings: GameSettingsRepository) {
        runCatching {
            val doc = FirebaseFirestore.getInstance().collection(COLLECTION).document(uid).get().await()
            if (doc.exists()) {
                @Suppress("UNCHECKED_CAST")
                val stats = (doc.get("stats") as? Map<String, Any?>).orEmpty().mapNotNull { (k, v) -> (v as? Number)?.toInt()?.let { k to it } }.toMap()
                @Suppress("UNCHECKED_CAST")
                val medals = (doc.get("achievements") as? List<Any?>).orEmpty().filterIsInstance<String>().toSet()
                @Suppress("UNCHECKED_CAST")
                val sprites = (doc.get("sprites") as? List<Any?>).orEmpty().mapNotNull { (it as? Number)?.toInt() }
                val coins = (doc.get("coins") as? Number)?.toInt() ?: 0
                Progress.importFromCloud(stats, medals)
                if (sprites.isNotEmpty()) settings.markSpritesSeen(sprites)
                val local = settings.coinsFlow.first()
                if (coins > local) settings.addCoins(coins - local)
            }
            pulledFor = uid
            push(settings)
        }.onFailure { Log.w(TAG, "Couldn't fetch cloud progress", it) }
    }

    private suspend fun push(settings: GameSettingsRepository) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (pulledFor != uid) return // never overwrite the cloud before merging it in
        runCatching {
            val data = mapOf(
                "stats" to Progress.exportStats(),
                "achievements" to Progress.unlocked.value.toList(),
                "coins" to settings.coinsFlow.first().coerceIn(0, 9_999_999),
                "sprites" to settings.seenSpritesFlow.first().filter { it in 2..2048 }.sorted(),
                "version" to 1,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            FirebaseFirestore.getInstance().collection(COLLECTION).document(uid).set(data).await()
        }.onFailure { Log.w(TAG, "Couldn't save cloud progress", it) }
    }
}
