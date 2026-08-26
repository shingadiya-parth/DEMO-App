package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Valid notification types supported by the centralized notification architecture.
 */
enum class NotificationType {
    DAILY_BONUS,
    GAME_REWARD,
    AD_REWARD,
    REFERRAL,
    REDEMPTION,
    GIVEAWAY,
    SYSTEM,
    SECURITY
}

/**
 * Immutable database entity representing an in-app / push notification.
 */
@Entity(
    tableName = "app_notifications",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "isRead"]),
        Index(value = ["userId", "createdAt"])
    ]
)
data class AppNotificationRecord(
    @PrimaryKey
    val notificationId: String = "notif_${UUID.randomUUID().toString().take(12)}",
    val userId: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val deepLink: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null
)

/**
 * User-configurable notification preferences stored per account.
 */
data class NotificationPreferences(
    val dailyBonusReminder: Boolean = true,
    val gameRewards: Boolean = true,
    val referralUpdates: Boolean = true,
    val redemptionUpdates: Boolean = true,
    val promotionalAndSystem: Boolean = true,
    val securityAlerts: Boolean = true // Immutable: Security alerts are always active
)
