package com.example.repository

import android.content.Context
import com.example.R
import com.example.model.*
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
        try {
            val customId = context.getString(R.string.firestore_database_id)
            if (customId.isNotBlank() && customId != "(default)" && !customId.startsWith("ai-studio-")) {
                customId
            } else {
                "(default)"
            }
        } catch (e: Exception) {
            "(default)"
        }
    }

    private val db: FirebaseFirestore by lazy {
        try {
            if (databaseId != "(default)") {
                FirebaseFirestore.getInstance(databaseId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            FirebaseFirestore.getInstance()
        }
    }

    val auth: FirebaseAuth
        get() = Firebase.auth

    val currentUserId: String?
        get() = auth.currentUser?.uid

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing cloud services.")
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

    // Save or initialize user profile upon Authentication
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
                "lastScreenState" to "lobby",
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

    // Persist user's last screen position / session state in Firestore
    suspend fun updateLastScreenState(screenState: String) {
        val uid = currentUserId ?: return
        try {
            db.collection("users").document(uid).update(
                mapOf(
                    "lastScreenState" to screenState,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            // Non-fatal background sync
        }
    }

    // Fetch user profile once (for fast restore on launch / login)
    suspend fun fetchUserProfile(): FirebaseUserProfile? {
        val uid = currentUserId ?: return null
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            if (snapshot.exists()) {
                snapshot.toObject(FirebaseUserProfile::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
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
            updates["gems"] = FieldValue.increment(5L)
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

    // =========================================================================
    // REAL-TIME MULTIPLAYER ROOM CODE SYSTEM (PLAY WITH FRIENDS)
    // =========================================================================

    /**
     * Creates a new authoritative cloud multiplayer room for friends.
     * Host is assigned Seat 0 (PlayerColor.RED).
     */
    suspend fun createMultiplayerRoom(
        roomId: String,
        hostName: String,
        avatarId: String,
        stake: Int = 500,
        theme: String = "theme_royal"
    ): FirebaseGameRoom {
        val uid = requireUserId()
        val roomRef = db.collection("rooms").document(roomId)

        val hostPlayer = RoomPlayer(
            uid = uid,
            name = hostName.ifBlank { "Host" },
            avatarId = avatarId,
            color = PlayerColor.RED.name,
            seat = 0,
            isHost = true,
            isReady = true
        )

        val initialTokens = mutableListOf<RoomTokenData>()
        for (col in PlayerColor.values()) {
            for (id in 0..3) {
                initialTokens.add(RoomTokenData(id = id, color = col.name, state = "YARD", stepCounter = 0))
            }
        }

        val room = FirebaseGameRoom(
            roomId = roomId,
            hostId = uid,
            hostName = hostName.ifBlank { "Host" },
            stake = stake,
            theme = theme,
            status = "waiting",
            playerCount = 1,
            playerIds = listOf(uid),
            players = listOf(hostPlayer),
            currentTurnColor = PlayerColor.RED.name,
            diceValue = 1,
            hasRolled = false,
            turnPhase = TurnPhase.ROLL_DICE.name,
            consecutiveSixCount = 0,
            movableTokenIds = emptyList(),
            tokens = initialTokens,
            lastActionTimestamp = System.currentTimeMillis()
        )

        roomRef.set(room).await()
        roomRef.update(
            mapOf(
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        return room
    }

    /**
     * Joins an existing multiplayer room using roomCode.
     * Assigns the friend to the next available seat and color:
     * Seat 1 = GREEN, Seat 2 = YELLOW, Seat 3 = BLUE.
     */
    suspend fun joinMultiplayerRoom(
        roomId: String,
        playerName: String,
        avatarId: String
    ): Pair<FirebaseGameRoom, PlayerColor> {
        val uid = requireUserId()
        val roomRef = db.collection("rooms").document(roomId)
        val snapshot = roomRef.get().await()

        if (!snapshot.exists()) {
            throw IllegalStateException("Room '$roomId' not found! Please check the code with your friend.")
        }

        val room = snapshot.toObject(FirebaseGameRoom::class.java)
            ?: throw IllegalStateException("Invalid room data")

        // If already in this room, re-join gracefully
        val existingPlayer = room.players.firstOrNull { it.uid == uid }
        if (existingPlayer != null) {
            val assignedColor = PlayerColor.valueOf(existingPlayer.color)
            return Pair(room, assignedColor)
        }

        if (room.status != "waiting") {
            throw IllegalStateException("Game has already started in room '$roomId'!")
        }

        if (room.playerCount >= 4 || room.players.size >= 4) {
            throw IllegalStateException("Room '$roomId' is full (maximum 4 players)!")
        }

        val seatIndex = room.players.size
        val assignedColor = when (seatIndex) {
            1 -> PlayerColor.GREEN
            2 -> PlayerColor.YELLOW
            3 -> PlayerColor.BLUE
            else -> PlayerColor.GREEN
        }

        val newPlayer = RoomPlayer(
            uid = uid,
            name = playerName.ifBlank { "Player ${seatIndex + 1}" },
            avatarId = avatarId,
            color = assignedColor.name,
            seat = seatIndex,
            isHost = false,
            isReady = true
        )

        val updatedPlayers = room.players + newPlayer
        val updatedPlayerIds = room.playerIds + uid

        roomRef.update(
            mapOf(
                "players" to updatedPlayers,
                "playerIds" to updatedPlayerIds,
                "playerCount" to updatedPlayers.size,
                "updatedAt" to FieldValue.serverTimestamp(),
                "lastActionTimestamp" to System.currentTimeMillis()
            )
        ).await()

        val updatedRoom = room.copy(
            players = updatedPlayers,
            playerIds = updatedPlayerIds,
            playerCount = updatedPlayers.size
        )

        return Pair(updatedRoom, assignedColor)
    }

    /**
     * Host starts the match once at least 2 players have joined the waiting room.
     */
    suspend fun startMultiplayerMatch(roomId: String) {
        val uid = requireUserId()
        val roomRef = db.collection("rooms").document(roomId)
        val snapshot = roomRef.get().await()

        if (!snapshot.exists()) {
            throw IllegalStateException("Room '$roomId' not found")
        }

        val room = snapshot.toObject(FirebaseGameRoom::class.java)
            ?: throw IllegalStateException("Invalid room data")

        if (room.hostId != uid) {
            throw IllegalStateException("Only the room creator (Host) can start the match!")
        }

        if (room.playerCount < 2) {
            throw IllegalStateException("Need at least 2 players to start a match!")
        }

        val initialTokens = mutableListOf<RoomTokenData>()
        for (col in PlayerColor.values()) {
            for (id in 0..3) {
                initialTokens.add(RoomTokenData(id = id, color = col.name, state = "YARD", stepCounter = 0))
            }
        }

        roomRef.update(
            mapOf(
                "status" to "playing",
                "currentTurnColor" to PlayerColor.RED.name,
                "turnPhase" to TurnPhase.ROLL_DICE.name,
                "diceValue" to 1,
                "hasRolled" to false,
                "consecutiveSixCount" to 0,
                "movableTokenIds" to emptyList<Int>(),
                "tokens" to initialTokens,
                "lastActionTimestamp" to System.currentTimeMillis(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /**
     * Observes real-time room state changes: player joins, status transitions, dice rolls, token moves.
     */
    fun observeMultiplayerRoom(roomId: String): Flow<FirebaseGameRoom?> = flow {
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

    /**
     * Authoritatively synchronizes a dice roll across all room participants in real-time.
     */
    suspend fun syncOnlineDiceRoll(
        roomId: String,
        diceValue: Int,
        consecutiveSixCount: Int,
        legalMoves: List<Int>,
        nextPhase: String
    ) {
        val roomRef = db.collection("rooms").document(roomId)
        roomRef.update(
            mapOf(
                "diceValue" to diceValue,
                "hasRolled" to true,
                "consecutiveSixCount" to consecutiveSixCount,
                "movableTokenIds" to legalMoves,
                "turnPhase" to nextPhase,
                "lastActionTimestamp" to System.currentTimeMillis(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /**
     * Authoritatively synchronizes a piece movement across all room participants in real-time.
     */
    suspend fun syncOnlineTokenMove(
        roomId: String,
        updatedTokens: List<LudoToken>,
        nextTurnColor: PlayerColor,
        nextPhase: String,
        winnerColor: PlayerColor? = null,
        winnerName: String? = null,
        lastMoveAction: RoomMoveAction? = null
    ) {
        val roomRef = db.collection("rooms").document(roomId)
        val tokenDataList = updatedTokens.map {
            RoomTokenData(id = it.id, color = it.color.name, state = it.state.name, stepCounter = it.stepCounter)
        }

        val updates = mutableMapOf<String, Any>(
            "tokens" to tokenDataList,
            "currentTurnColor" to nextTurnColor.name,
            "turnPhase" to nextPhase,
            "hasRolled" to false,
            "movableTokenIds" to emptyList<Int>(),
            "lastActionTimestamp" to System.currentTimeMillis(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (lastMoveAction != null) {
            updates["lastMove"] = lastMoveAction
        }

        if (winnerColor != null) {
            updates["winnerColor"] = winnerColor.name
            updates["winnerName"] = winnerName ?: winnerColor.displayName
            updates["status"] = "finished"
        }

        roomRef.update(updates).await()
    }

    /**
     * Advances turn when no moves are possible or forfeiture occurs.
     */
    suspend fun syncOnlineAdvanceTurn(roomId: String, nextTurnColor: PlayerColor) {
        val roomRef = db.collection("rooms").document(roomId)
        roomRef.update(
            mapOf(
                "currentTurnColor" to nextTurnColor.name,
                "turnPhase" to TurnPhase.ROLL_DICE.name,
                "hasRolled" to false,
                "consecutiveSixCount" to 0,
                "movableTokenIds" to emptyList<Int>(),
                "lastActionTimestamp" to System.currentTimeMillis(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /**
     * Leaves or closes an online room.
     */
    suspend fun leaveMultiplayerRoom(roomId: String) {
        val uid = currentUserId ?: return
        val roomRef = db.collection("rooms").document(roomId)
        val snapshot = roomRef.get().await()
        if (!snapshot.exists()) return

        val room = snapshot.toObject(FirebaseGameRoom::class.java) ?: return

        if (room.hostId == uid && room.status == "waiting") {
            // Host leaves waiting room -> delete room
            roomRef.delete().await()
        } else {
            // Player leaves -> remove from player list
            val updatedPlayers = room.players.filter { it.uid != uid }
            val updatedPlayerIds = room.playerIds.filter { it != uid }
            if (updatedPlayers.isEmpty()) {
                roomRef.delete().await()
            } else {
                roomRef.update(
                    mapOf(
                        "players" to updatedPlayers,
                        "playerIds" to updatedPlayerIds,
                        "playerCount" to updatedPlayers.size,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }
}
