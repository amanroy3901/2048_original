package com.avfusionapps.game_2048.data.repository

import android.content.Context
import android.util.Log
import com.avfusionapps.game_2048.data.model.LevelProgression
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing level progression data in Firebase Firestore.
 * Handles saving, loading, and updating player level progression.
 */
class LevelProgressionRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    companion object {
        private const val TAG = "LevelProgressionRepo"
        private const val COLLECTION_NAME = "level_progressions"
    }

    /**
     * Get the current user's level progression as a Flow
     */
    fun getLevelProgression(): Flow<LevelProgression?> = callbackFlow {
        val user = auth.currentUser
        if (user == null) {
            Log.w(TAG, "No authenticated user found")
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val documentRef = firestore.collection(COLLECTION_NAME).document(user.uid)
        
        val listener = documentRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to level progression", error)
                trySend(null)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val progression = snapshot.toObject(LevelProgression::class.java)
                trySend(progression)
            } else {
                // No existing progression, create default one
                val defaultProgression = createDefaultProgression(user.uid, user.displayName ?: "Player")
                Log.d(TAG, "No existing progression found, creating default")
                trySend(defaultProgression)
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Save or update level progression to Firestore
     */
    suspend fun saveLevelProgression(progression: LevelProgression) {
        val user = auth.currentUser
        if (user == null) {
            Log.w(TAG, "Cannot save progression: No authenticated user")
            return
        }

        try {
            val documentRef = firestore.collection(COLLECTION_NAME).document(user.uid)
            
            // Update timestamps
            val updatedProgression = progression.copy(
                playerId = user.uid,
                playerName = user.displayName ?: progression.playerName,
                lastUpdated = com.google.firebase.Timestamp.now()
            )

            documentRef.set(updatedProgression).await()
            Log.d(TAG, "Level progression saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving level progression", e)
            throw e
        }
    }

    /**
     * Unlock a new level for the player
     */
    suspend fun unlockLevel(newLevel: Int, playerName: String = "Player") {
        val user = auth.currentUser
        if (user == null) {
            Log.w(TAG, "Cannot unlock level: No authenticated user")
            return
        }

        try {
            val currentProgression = getLevelProgression().first()
            val progression = currentProgression ?: createDefaultProgression(user.uid, playerName)

            val updatedUnlockedLevels = if (newLevel !in progression.unlockedLevels) {
                (progression.unlockedLevels + newLevel).sorted()
            } else {
                progression.unlockedLevels
            }

            val updatedUnlockTimes = progression.levelUnlockTimes.toMutableMap()
            updatedUnlockTimes[newLevel.toString()] = com.google.firebase.Timestamp.now()

            val updatedProgression = progression.copy(
                currentLevel = maxOf(progression.currentLevel, newLevel),
                unlockedLevels = updatedUnlockedLevels,
                levelUnlockTimes = updatedUnlockTimes,
                playerName = playerName
            )

            saveLevelProgression(updatedProgression)
            Log.d(TAG, "Level $newLevel unlocked successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error unlocking level $newLevel", e)
            throw e
        }
    }

    /**
     * Check if a level is unlocked
     */
    suspend fun isLevelUnlocked(level: Int): Boolean {
        val progression = getLevelProgression().first()
        return progression?.unlockedLevels?.contains(level) ?: (level == 1)
    }

    /**
     * Get the highest unlocked level
     */
    suspend fun getHighestUnlockedLevel(): Int {
        val progression = getLevelProgression().first()
        return progression?.unlockedLevels?.maxOrNull() ?: 1
    }

    /**
     * Create a default level progression for a new player
     */
    private fun createDefaultProgression(playerId: String, playerName: String): LevelProgression {
        return LevelProgression(
            playerId = playerId,
            playerName = playerName,
            currentLevel = 1,
            unlockedLevels = listOf(1),
            levelUnlockTimes = mapOf("1" to com.google.firebase.Timestamp.now()),
            createdAt = com.google.firebase.Timestamp.now(),
            lastUpdated = com.google.firebase.Timestamp.now()
        )
    }

    /**
     * Delete level progression (for testing/debugging)
     */
    suspend fun deleteLevelProgression() {
        val user = auth.currentUser
        if (user == null) {
            Log.w(TAG, "Cannot delete progression: No authenticated user")
            return
        }

        try {
            firestore.collection(COLLECTION_NAME).document(user.uid).delete().await()
            Log.d(TAG, "Level progression deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting level progression", e)
            throw e
        }
    }
}