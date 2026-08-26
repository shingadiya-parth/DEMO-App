package com.example.services.notifications

import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityType
import com.example.data.model.NotificationType
import com.example.data.repository.ActivityRepository
import com.example.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID

data class InAppNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val type: NotificationType,
    val deepLink: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class NotificationService(
    private val notificationRepository: NotificationRepository? = null,
    private val activityRepository: ActivityRepository? = null
) {

    private val _notifications = MutableSharedFlow<InAppNotification>(extraBufferCapacity = 64)
    val notifications: SharedFlow<InAppNotification> = _notifications.asSharedFlow()

    suspend fun emitRewardEarned(userId: String?, coins: Long, gameOrActivityName: String, relatedId: String? = null) {
        val title = "🎉 Reward Earned"
        val message = "You earned +$coins NestCoins from $gameOrActivityName!"
        _notifications.emit(
            InAppNotification(
                title = title,
                message = message,
                type = NotificationType.GAME_REWARD,
                deepLink = "wallet"
            )
        )
        if (!userId.isNullOrBlank()) {
            notificationRepository?.sendNotification(
                userId = userId,
                title = title,
                message = message,
                type = NotificationType.GAME_REWARD,
                deepLink = "wallet"
            )
        }
    }

    suspend fun emitDailyBonus(userId: String?, coins: Long, streakDays: Int) {
        val title = "🎁 Daily Bonus Claimed"
        val message = "Claimed +$coins NestCoins for Day $streakDays streak!"
        _notifications.emit(
            InAppNotification(
                title = title,
                message = message,
                type = NotificationType.DAILY_BONUS,
                deepLink = "home"
            )
        )
        if (!userId.isNullOrBlank()) {
            notificationRepository?.sendNotification(
                userId = userId,
                title = title,
                message = message,
                type = NotificationType.DAILY_BONUS,
                deepLink = "home"
            )
            activityRepository?.recordActivity(
                userId = userId,
                activityType = ActivityType.DAILY_BONUS_CLAIMED,
                category = ActivityCategory.REWARDS,
                title = "🎁 Daily Bonus",
                description = "Day $streakDays Streak Bonus claimed",
                relatedId = "daily_${System.currentTimeMillis() / (24 * 60 * 60 * 1000)}",
                result = "+$coins NestCoins"
            )
        }
    }

    suspend fun emitRedemptionStatus(userId: String?, rewardTitle: String, statusText: String, redemptionId: String? = null) {
        val title = "🎁 Redemption Update"
        val message = "Your request for $rewardTitle is $statusText."
        _notifications.emit(
            InAppNotification(
                title = title,
                message = message,
                type = NotificationType.REDEMPTION,
                deepLink = "rewards"
            )
        )
        if (!userId.isNullOrBlank()) {
            notificationRepository?.sendNotification(
                userId = userId,
                title = title,
                message = message,
                type = NotificationType.REDEMPTION,
                deepLink = "rewards"
            )
        }
    }

    suspend fun emitFriendJoined(userId: String?, referralCode: String) {
        val title = "👥 Friend Joined"
        val message = "A new friend signed up using your code ($referralCode)."
        _notifications.emit(
            InAppNotification(
                title = title,
                message = message,
                type = NotificationType.REFERRAL,
                deepLink = "refer_earn"
            )
        )
        if (!userId.isNullOrBlank()) {
            notificationRepository?.sendNotification(
                userId = userId,
                title = title,
                message = message,
                type = NotificationType.REFERRAL,
                deepLink = "refer_earn"
            )
        }
    }

    suspend fun emitReferralQualifying(userId: String?, progress: Int, target: Int) {
        val title = "⏳ Referral Progress"
        val message = "Your referred friend has completed $progress/$target qualifying games."
        _notifications.emit(
            InAppNotification(
                title = title,
                message = message,
                type = NotificationType.REFERRAL,
                deepLink = "refer_earn"
            )
        )
        if (!userId.isNullOrBlank()) {
            notificationRepository?.sendNotification(
                userId = userId,
                title = title,
                message = message,
                type = NotificationType.REFERRAL,
                deepLink = "refer_earn"
            )
        }
    }

    suspend fun emitReferralQualified(userId: String?, rewardCoins: Long) {
        val title = "🌟 Referral Qualified"
        val message = "Your friend completed requirements! +$rewardCoins NestCoins credited."
        _notifications.emit(
            InAppNotification(
                title = title,
                message = message,
                type = NotificationType.REFERRAL,
                deepLink = "refer_earn"
            )
        )
        if (!userId.isNullOrBlank()) {
            notificationRepository?.sendNotification(
                userId = userId,
                title = title,
                message = message,
                type = NotificationType.REFERRAL,
                deepLink = "refer_earn"
            )
        }
    }

    suspend fun emitReferralRewardCredited(userId: String?, coins: Long, isInviter: Boolean) {
        val title = "💰 Referral Bonus Credited"
        val message = if (isInviter) {
            "You received +$coins NestCoins for referring a friend!"
        } else {
            "Welcome bonus of +$coins NestCoins credited for joining!"
        }
        _notifications.emit(
            InAppNotification(
                title = title,
                message = message,
                type = NotificationType.REFERRAL,
                deepLink = "refer_earn"
            )
        )
        if (!userId.isNullOrBlank()) {
            notificationRepository?.sendNotification(
                userId = userId,
                title = title,
                message = message,
                type = NotificationType.REFERRAL,
                deepLink = "refer_earn"
            )
            activityRepository?.recordActivity(
                userId = userId,
                activityType = ActivityType.REFERRAL_BONUS_CREDITED,
                category = ActivityCategory.REFERRALS,
                title = if (isInviter) "👥 Referral Reward" else "🎁 Welcome Referral Bonus",
                description = message,
                relatedId = "ref_cred_${System.currentTimeMillis()}",
                result = "+$coins NestCoins"
            )
        }
    }

    suspend fun emitSecurityAlert(userId: String?, title: String, message: String) {
        _notifications.emit(
            InAppNotification(
                title = title,
                message = message,
                type = NotificationType.SECURITY,
                deepLink = "settings"
            )
        )
        if (!userId.isNullOrBlank()) {
            notificationRepository?.sendNotification(
                userId = userId,
                title = title,
                message = message,
                type = NotificationType.SECURITY,
                deepLink = "settings"
            )
        }
    }

    suspend fun emitStreakReminder(day: Int) {
        _notifications.emit(
            InAppNotification(
                title = "🔥 Keep your streak alive!",
                message = "Check in today to claim your Day $day bonus coins.",
                type = NotificationType.DAILY_BONUS,
                deepLink = "home"
            )
        )
    }
}
