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

data class FirebaseGameRoom(
    val roomId: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val stake: Int = 500,
    val theme: String = "theme_royal",
    val status: String = "waiting", // "waiting", "playing", "finished"
    val playerCount: Int = 1,
    val playerIds: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
