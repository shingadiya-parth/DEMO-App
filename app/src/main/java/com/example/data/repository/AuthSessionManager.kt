package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences

class AuthSessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "playrewards_auth_session"
        private const val KEY_ACTIVE_USER_ID = "key_active_user_id"
        private const val KEY_SESSION_TIMESTAMP = "key_session_timestamp"
    }

    fun getActiveUserId(): String? {
        return prefs.getString(KEY_ACTIVE_USER_ID, null)
    }

    fun setActiveUserId(userId: String?) {
        prefs.edit().apply {
            if (userId != null) {
                putString(KEY_ACTIVE_USER_ID, userId)
                putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis())
            } else {
                remove(KEY_ACTIVE_USER_ID)
                remove(KEY_SESSION_TIMESTAMP)
            }
            apply()
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun hasActiveSession(): Boolean {
        return getActiveUserId() != null
    }
}
