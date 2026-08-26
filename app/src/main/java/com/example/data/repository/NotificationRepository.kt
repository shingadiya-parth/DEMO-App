package com.example.data.repository

import com.example.data.local.NotificationDao
import com.example.data.model.AppNotificationRecord
import com.example.data.model.NotificationPreferences
import com.example.data.model.NotificationType
import com.example.services.notifications.PushTokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Centralized repository for managing persistent notifications, unread badges,
 * notification preferences, and push token lifecycle.
 */
class NotificationRepository(
    private val notificationDao: NotificationDao,
    private val preferencesRepository: NotificationPreferencesRepository,
    private val pushTokenManager: PushTokenManager
) {

    fun observeNotifications(userId: String): Flow<List<AppNotificationRecord>> {
        return notificationDao.observeNotifications(userId)
    }

    suspend fun getNotifications(userId: String, limit: Int = 100, offset: Int = 0): List<AppNotificationRecord> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) emptyList()
        else notificationDao.getNotifications(userId, limit, offset)
    }

    fun observeUnreadCount(userId: String): Flow<Int> {
        return notificationDao.observeUnreadCount(userId)
    }

    suspend fun getUnreadCount(userId: String): Int = withContext(Dispatchers.IO) {
        if (userId.isBlank()) 0
        else notificationDao.getUnreadCount(userId)
    }

    suspend fun sendNotification(
        userId: String,
        title: String,
        message: String,
        type: NotificationType,
        deepLink: String? = null,
        expiresAt: Long? = null
    ): AppNotificationRecord? = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext null

        // Check if user has opted into this category
        val prefs = preferencesRepository.getPreferences(userId)
        val isAllowed = when (type) {
            NotificationType.DAILY_BONUS -> prefs.dailyBonusReminder
            NotificationType.GAME_REWARD -> prefs.gameRewards
            NotificationType.AD_REWARD -> prefs.gameRewards
            NotificationType.REFERRAL -> prefs.referralUpdates
            NotificationType.REDEMPTION -> prefs.redemptionUpdates
            NotificationType.GIVEAWAY -> prefs.promotionalAndSystem
            NotificationType.SYSTEM -> prefs.promotionalAndSystem
            NotificationType.SECURITY -> true // Security alerts cannot be silenced
        }

        if (!isAllowed) return@withContext null

        val record = AppNotificationRecord(
            notificationId = "notif_${UUID.randomUUID().toString().take(12)}",
            userId = userId,
            title = title,
            message = message,
            type = type,
            deepLink = deepLink,
            isRead = false,
            createdAt = System.currentTimeMillis(),
            expiresAt = expiresAt
        )

        notificationDao.insertNotification(record)
        record
    }

    suspend fun markAsRead(notificationId: String, userId: String) = withContext(Dispatchers.IO) {
        if (notificationId.isNotBlank() && userId.isNotBlank()) {
            notificationDao.markAsRead(notificationId, userId)
        }
    }

    suspend fun markAllAsRead(userId: String) = withContext(Dispatchers.IO) {
        if (userId.isNotBlank()) {
            notificationDao.markAllAsRead(userId)
        }
    }

    suspend fun deleteNotification(notificationId: String, userId: String) = withContext(Dispatchers.IO) {
        if (notificationId.isNotBlank() && userId.isNotBlank()) {
            notificationDao.deleteNotification(notificationId, userId)
        }
    }

    suspend fun clearExpired() = withContext(Dispatchers.IO) {
        notificationDao.deleteExpiredNotifications(System.currentTimeMillis())
    }

    // Preferences pass-through
    fun getPreferences(userId: String): NotificationPreferences = preferencesRepository.getPreferences(userId)

    fun observePreferences(userId: String): Flow<NotificationPreferences> = preferencesRepository.observePreferences(userId)

    fun updatePreferences(userId: String, preferences: NotificationPreferences) {
        preferencesRepository.updatePreferences(userId, preferences)
    }

    fun toggleCategory(userId: String, categoryKey: String, isEnabled: Boolean) {
        preferencesRepository.toggleCategory(userId, categoryKey, isEnabled)
    }

    // Push token pass-through
    suspend fun registerPushToken(userId: String, token: String): Boolean =
        pushTokenManager.registerPushToken(userId, token)

    suspend fun getActivePushToken(userId: String): String? =
        pushTokenManager.getActiveToken(userId)

    suspend fun clearPushToken(userId: String) =
        pushTokenManager.clearPushToken(userId)
}
