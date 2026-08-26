package com.example.services.notifications

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Prepares and securely handles client push notification tokens (Firebase Cloud Messaging / Provider).
 * Tokens are stored per user session and cleared upon logout.
 */
class PushTokenManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "push_notification_session",
        Context.MODE_PRIVATE
    )

    suspend fun registerPushToken(userId: String, token: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank() || token.isBlank()) return@withContext false
        prefs.edit()
            .putString(key(userId, "token"), token)
            .putLong(key(userId, "timestamp"), System.currentTimeMillis())
            .putString(key(userId, "device"), "${Build.MANUFACTURER} ${Build.MODEL}")
            .putBoolean(key(userId, "is_active"), true)
            .apply()
        true
    }

    suspend fun getActiveToken(userId: String): String? = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext null
        val isActive = prefs.getBoolean(key(userId, "is_active"), false)
        if (!isActive) return@withContext null
        prefs.getString(key(userId, "token"), null)
    }

    suspend fun clearPushToken(userId: String) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        prefs.edit()
            .remove(key(userId, "token"))
            .remove(key(userId, "timestamp"))
            .remove(key(userId, "device"))
            .putBoolean(key(userId, "is_active"), false)
            .apply()
    }

    suspend fun isTokenRegistered(userId: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext false
        prefs.getBoolean(key(userId, "is_active"), false) && prefs.getString(key(userId, "token"), null) != null
    }

    private fun key(userId: String, suffix: String): String = "push_${userId}_$suffix"
}
