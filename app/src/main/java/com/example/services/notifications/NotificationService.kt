package com.example.services.notifications

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID

enum class NotificationType {
    REWARD_EARNED,
    DAILY_STREAK,
    REDEMPTION_UPDATE,
    GAME_UNLOCK,
    SECURITY_ALERT
}

data class InAppNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long = System.currentTimeMillis()
)

class NotificationService {

    private val _notifications = MutableSharedFlow<InAppNotification>(extraBufferCapacity = 64)
    val notifications: SharedFlow<InAppNotification> = _notifications.asSharedFlow()

    suspend fun emitRewardEarned(coins: Long, gameOrActivityName: String) {
        _notifications.emit(
            InAppNotification(
                title = "🎉 Coins Earned!",
                message = "You received +$coins coins from $gameOrActivityName",
                type = NotificationType.REWARD_EARNED
            )
        )
    }

    suspend fun emitStreakReminder(day: Int) {
        _notifications.emit(
            InAppNotification(
                title = "🔥 Keep your streak alive!",
                message = "Check in today to claim your Day $day bonus coins.",
                type = NotificationType.DAILY_STREAK
            )
        )
    }

    suspend fun emitRedemptionStatus(rewardTitle: String, statusText: String) {
        _notifications.emit(
            InAppNotification(
                title = "🎁 Reward Request",
                message = "$rewardTitle is now $statusText",
                type = NotificationType.REDEMPTION_UPDATE
            )
        )
    }
}
