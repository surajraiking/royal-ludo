package com.example.model

import com.google.firebase.Timestamp

data class FirebaseUserProfile(
    val userId: String = "",
    val username: String = "",
    val avatarId: String = "avatar_crown",
    val coins: Int = 1500,
    val gems: Int = 100,
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val unlockedDiceSkins: List<String> = listOf("skin_default"),
    val unlockedThemes: List<String> = listOf("theme_royal"),
    val selectedDiceSkin: String = "skin_default",
    val selectedTheme: String = "theme_royal",
    val lastScreenState: String = "lobby",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class FirebaseMatchRecord(
    val matchId: String = "",
    val userId: String = "",
    val gameMode: String = "Offline",
    val won: Boolean = false,
    val coinsEarned: Int = 0,
    val createdAt: Timestamp? = null
)

data class RoomPlayer(
    val uid: String = "",
    val name: String = "",
    val avatarId: String = "avatar_crown",
    val color: String = "RED", // "RED", "GREEN", "YELLOW", "BLUE"
    val seat: Int = 0, // 0..3
    val isHost: Boolean = false,
    val isReady: Boolean = true
)

data class RoomTokenData(
    val id: Int = 0,
    val color: String = "RED",
    val state: String = "YARD", // "YARD", "TRACK", "HOME_STRETCH", "GOAL"
    val stepCounter: Int = 0
)

data class RoomMoveAction(
    val playerColor: String = "",
    val tokenId: Int = 0,
    val diceValue: Int = 0,
    val wasCapture: Boolean = false,
    val reachedGoal: Boolean = false,
    val timestamp: Long = 0L
)

data class FirebaseGameRoom(
    val roomId: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val stake: Int = 500,
    val theme: String = "theme_royal",
    val status: String = "waiting", // "waiting", "playing", "finished"
    val playerCount: Int = 1,
    val playerIds: List<String> = emptyList(),
    val players: List<RoomPlayer> = emptyList(),
    val currentTurnColor: String = "RED",
    val diceValue: Int = 1,
    val hasRolled: Boolean = false,
    val turnPhase: String = "ROLL_DICE",
    val consecutiveSixCount: Int = 0,
    val movableTokenIds: List<Int> = emptyList(),
    val tokens: List<RoomTokenData> = emptyList(),
    val winnerName: String? = null,
    val winnerColor: String? = null,
    val lastMove: RoomMoveAction? = null,
    val lastActionTimestamp: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
