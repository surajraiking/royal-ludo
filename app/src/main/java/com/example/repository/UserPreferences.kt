package com.example.repository

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("royal_ludo_empire_prefs", Context.MODE_PRIVATE)

    var username: String
        get() = prefs.getString("username", "RoyalWarrior") ?: "RoyalWarrior"
        set(value) = prefs.edit().putString("username", value).apply()

    var avatarId: String
        get() = prefs.getString("avatar_id", "avatar_1") ?: "avatar_1"
        set(value) = prefs.edit().putString("avatar_id", value).apply()

    var coins: Int
        get() = prefs.getInt("coins", 1500)
        set(value) = prefs.edit().putInt("coins", value).apply()

    var gems: Int
        get() = prefs.getInt("gems", 100)
        set(value) = prefs.edit().putInt("gems", value).apply()

    var matchesPlayed: Int
        get() = prefs.getInt("matches_played", 0)
        set(value) = prefs.edit().putInt("matches_played", value).apply()

    var matchesWon: Int
        get() = prefs.getInt("matches_won", 0)
        set(value) = prefs.edit().putInt("matches_won", value).apply()

    var selectedDiceSkin: String
        get() = prefs.getString("selected_dice_skin", "Neon Gold") ?: "Neon Gold"
        set(value) = prefs.edit().putString("selected_dice_skin", value).apply()

    var unlockedSkins: Set<String>
        get() = prefs.getStringSet("unlocked_skins", setOf("Neon Gold")) ?: setOf("Neon Gold")
        set(value) = prefs.edit().putStringSet("unlocked_skins", value).apply()

    var lastSpinTimestamp: Long
        get() = prefs.getLong("last_spin_timestamp", 0L)
        set(value) = prefs.edit().putLong("last_spin_timestamp", value).apply()

    fun unlockSkin(skinName: String, costCoins: Int, costGems: Int): Boolean {
        if (coins >= costCoins && gems >= costGems) {
            coins -= costCoins
            gems -= costGems
            val updated = unlockedSkins.toMutableSet()
            updated.add(skinName)
            unlockedSkins = updated
            return true
        }
        return false
    }

    fun earnCoins(amount: Int) {
        coins += amount
    }

    fun earnGems(amount: Int) {
        gems += amount
    }

    fun recordMatch(isWin: Boolean) {
        matchesPlayed += 1
        if (isWin) {
            matchesWon += 1
            earnCoins(250) // Win bonus!
            earnGems(5)
        } else {
            earnCoins(50) // Consolation reward
        }
    }
}
