package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Types of user activity events across the application.
 */
enum class ActivityType {
    // Game Events
    SPIN_COMPLETED,
    SCRATCH_COMPLETED,
    PUZZLE_COMPLETED,
    COIN_TOSS_COMPLETED,
    TIC_TAC_TOE_COMPLETED,
    BUBBLE_POP_COMPLETED,

    // Earn & Reward Events
    DAILY_BONUS_CLAIMED,
    AD_REWARD_CLAIMED,

    // Referral Events
    REFERRAL_JOINED,
    REFERRAL_QUALIFIED,
    REFERRAL_BONUS_CREDITED,

    // Redemption Events
    REDEMPTION_REQUESTED,
    REDEMPTION_PROCESSING,
    REDEMPTION_FULFILLED,
    REDEMPTION_REJECTED,
    REDEMPTION_REFUNDED,

    // Account & Security Events
    ACCOUNT_CREATED,
    PROFILE_UPDATED
}

/**
 * High-level categories for UI filtering.
 */
enum class ActivityCategory {
    ALL,
    GAMES,
    REWARDS,
    REFERRALS,
    REDEMPTIONS
}

/**
 * Unified Activity History Entity representing a user-facing event/milestone.
 * Distinct from the financial CoinTransaction ledger.
 */
@Entity(
    tableName = "user_activities",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "category"]),
        Index(value = ["userId", "createdAt"]),
        Index(value = ["relatedId"])
    ]
)
data class UserActivityRecord(
    @PrimaryKey
    val activityId: String = "act_${UUID.randomUUID().toString().take(12)}",
    val userId: String,
    val activityType: ActivityType,
    val category: ActivityCategory,
    val title: String,
    val description: String,
    val relatedId: String? = null,
    val result: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
