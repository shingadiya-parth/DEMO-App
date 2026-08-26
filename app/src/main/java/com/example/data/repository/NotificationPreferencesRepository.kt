package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.NotificationPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Manages user notification preference toggles stored locally.
 */
class NotificationPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "user_notification_preferences",
        Context.MODE_PRIVATE
    )

    fun getPreferences(userId: String): NotificationPreferences {
        if (userId.isBlank()) return NotificationPreferences()
        return NotificationPreferences(
            dailyBonusReminder = prefs.getBoolean(key(userId, "daily_bonus"), true),
            gameRewards = prefs.getBoolean(key(userId, "game_rewards"), true),
            referralUpdates = prefs.getBoolean(key(userId, "referrals"), true),
            redemptionUpdates = prefs.getBoolean(key(userId, "redemptions"), true),
            promotionalAndSystem = prefs.getBoolean(key(userId, "promo_system"), true),
            securityAlerts = true // Security alerts are mandatory and cannot be disabled
        )
    }

    fun observePreferences(userId: String): Flow<NotificationPreferences> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(getPreferences(userId))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getPreferences(userId))
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.distinctUntilChanged()

    fun updatePreferences(userId: String, preferences: NotificationPreferences) {
        if (userId.isBlank()) return
        prefs.edit()
            .putBoolean(key(userId, "daily_bonus"), preferences.dailyBonusReminder)
            .putBoolean(key(userId, "game_rewards"), preferences.gameRewards)
            .putBoolean(key(userId, "referrals"), preferences.referralUpdates)
            .putBoolean(key(userId, "redemptions"), preferences.redemptionUpdates)
            .putBoolean(key(userId, "promo_system"), preferences.promotionalAndSystem)
            .apply()
    }

    fun toggleCategory(userId: String, categoryKey: String, isEnabled: Boolean) {
        if (userId.isBlank()) return
        when (categoryKey.lowercase()) {
            "daily_bonus" -> prefs.edit().putBoolean(key(userId, "daily_bonus"), isEnabled).apply()
            "game_rewards" -> prefs.edit().putBoolean(key(userId, "game_rewards"), isEnabled).apply()
            "referrals" -> prefs.edit().putBoolean(key(userId, "referrals"), isEnabled).apply()
            "redemptions" -> prefs.edit().putBoolean(key(userId, "redemptions"), isEnabled).apply()
            "promo_system" -> prefs.edit().putBoolean(key(userId, "promo_system"), isEnabled).apply()
        }
    }

    private fun key(userId: String, suffix: String): String = "notif_pref_${userId}_$suffix"
}
