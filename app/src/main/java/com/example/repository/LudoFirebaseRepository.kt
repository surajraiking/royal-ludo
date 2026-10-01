package com.example.repository

import android.content.Context
import com.example.R
import com.example.model.FirebaseGameRoom
import com.example.model.FirebaseMatchRecord
import com.example.model.FirebaseUserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LudoFirebaseRepository(private val context: Context) {
    // Custom database ID from firebase_applet_config.xml
    private val databaseId: String by lazy {
        context.getString(R.string.firestore_database_id)
    }

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(databaseId)
    }

    private val auth: FirebaseAuth
        get() = Firebase.auth

    val currentUserId: String?
        get() = auth.currentUser?.uid

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // Observe authenticated user's profile in real-time
    fun observeUserProfile(): Flow<FirebaseUserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(FirebaseUserProfile::class.java)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    // Save or initialize user profile upon Google Sign-In
    suspend fun saveOrInitUserProfile(
        username: String,
        avatarId: String,
        initialCoins: Int = 1500,
        initialGems: Int = 100
    ) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val snapshot = docRef.get().await()

        if (!snapshot.exists()) {
            val payload = mapOf(
                "userId" to uid,
                "username" to username.ifBlank { "RoyalWarrior" },
                "avatarId" to avatarId,
                "coins" to initialCoins,
                "gems" to initialGems,
                "matchesPlayed" to 0,
                "matchesWon" to 0,
                "unlockedDiceSkins" to listOf("skin_default"),
                "unlockedThemes" to listOf("theme_royal"),
                "selectedDiceSkin" to "skin_default",
                "selectedTheme" to "theme_royal",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } else {
            docRef.update(
                mapOf(
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    // Update profile credentials
    suspend fun updateProfile(username: String, avatarId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        docRef.update(
            mapOf(
                "username" to username,
                "avatarId" to avatarId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // Update currency after match, reward, or shop purchase
    suspend fun updateCurrency(coinsDelta: Int, gemsDelta: Int) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        docRef.update(
            mapOf(
                "coins" to FieldValue.increment(coinsDelta.toLong()),
                "gems" to FieldValue.increment(gemsDelta.toLong()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // Record completed match in Firestore
    suspend fun recordMatch(gameMode: String, won: Boolean, coinsEarned: Int) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val matchesCol = docRef.collection("matches")

        val matchId = "match_${System.currentTimeMillis()}"
        val matchPayload = mapOf(
            "matchId" to matchId,
            "userId" to uid,
            "gameMode" to gameMode,
            "won" to won,
            "coinsEarned" to coinsEarned,
            "createdAt" to FieldValue.serverTimestamp()
        )
        matchesCol.document(matchId).set(matchPayload).await()

        // Update aggregate user statistics
        val updates = mutableMapOf<String, Any>(
            "matchesPlayed" to FieldValue.increment(1L),
            "coins" to FieldValue.increment(coinsEarned.toLong()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (won) {
            updates["matchesWon"] = FieldValue.increment(1L)
            updates["gems"] = FieldValue.increment(5L) // Bonus gems on win!
        }
        docRef.update(updates).await()
    }

    // Global Top Warriors Leaderboard
    fun observeLeaderboard(): Flow<List<FirebaseUserProfile>> = flow {
        val path = "users"
        emitAll(
            db.collection(path)
                .orderBy("coins", Query.Direction.DESCENDING)
                .limit(25)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FirebaseUserProfile::class.java)
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // Create a new multiplayer room
    suspend fun createRoom(roomId: String, hostName: String, stake: Int, theme: String) {
        val uid = requireUserId()
        val roomRef = db.collection("rooms").document(roomId)
        val payload = mapOf(
            "roomId" to roomId,
            "hostId" to uid,
            "hostName" to hostName,
            "stake" to stake,
            "theme" to theme,
            "status" to "waiting",
            "playerCount" to 1,
            "playerIds" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        roomRef.set(payload).await()
    }

    // Observe rooms for friend matchmaking
    fun observeRoom(roomId: String): Flow<FirebaseGameRoom?> = flow {
        val path = "rooms/$roomId"
        emitAll(
            db.collection("rooms").document(roomId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(FirebaseGameRoom::class.java)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }
}
