package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Status lifecycle of a referral record.
 */
enum class ReferralStatus(val displayName: String) {
    PENDING("Pending"),
    QUALIFYING("Qualifying"),
    QUALIFIED("Qualified"),
    REWARDED("Rewarded"),
    REJECTED("Rejected"),
    EXPIRED("Expired")
}

/**
 * Anti-fraud risk state.
 */
enum class ReferralRiskState {
    NORMAL,
    REVIEW,
    BLOCKED
}

/**
 * Supported referral qualification requirements.
 */
enum class ReferralQualificationType {
    GAMES_COMPLETED,
    MINIMUM_COINS_EARNED,
    FIRST_ACTIVITY
}

/**
 * Authoritative Referral Record Entity.
 */
@Entity(tableName = "referral_record")
data class ReferralRecord(
    @PrimaryKey
    @ColumnInfo(name = "referral_id")
    val referralId: String,

    @ColumnInfo(name = "referrer_user_id")
    val referrerUserId: String,

    @ColumnInfo(name = "referred_user_id")
    val referredUserId: String,

    @ColumnInfo(name = "referral_code")
    val referralCode: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "qualified_at")
    val qualifiedAt: Long? = null,

    @ColumnInfo(name = "status")
    val status: ReferralStatus = ReferralStatus.PENDING,

    @ColumnInfo(name = "qualification_progress")
    val qualificationProgress: Int = 0,

    @ColumnInfo(name = "qualification_target")
    val qualificationTarget: Int = 3,

    @ColumnInfo(name = "referrer_reward_amount")
    val referrerRewardAmount: Long = 500L,

    @ColumnInfo(name = "referred_user_reward_amount")
    val referredUserRewardAmount: Long = 100L,

    @ColumnInfo(name = "referrer_reward_transaction_id")
    val referrerRewardTransactionId: String? = null,

    @ColumnInfo(name = "referred_user_reward_transaction_id")
    val referredUserRewardTransactionId: String? = null,

    @ColumnInfo(name = "idempotency_key")
    val idempotencyKey: String,

    @ColumnInfo(name = "risk_state")
    val riskState: ReferralRiskState = ReferralRiskState.NORMAL,

    @ColumnInfo(name = "notes")
    val notes: String? = null
)

/**
 * Dashboard summary presented on the Refer & Earn screen.
 */
data class ReferralSummary(
    val referralCode: String,
    val shareableLink: String,
    val totalFriendsReferred: Int,
    val qualifiedReferrals: Int,
    val pendingReferrals: Int,
    val rejectedReferrals: Int,
    val totalCoinsEarned: Long,
    val hasAppliedReferralCode: Boolean,
    val referredByCode: String? = null,
    val recentReferrals: List<SafeReferralDisplayItem>
)

/**
 * Safe, privacy-preserving display model for referred friends.
 * Does NOT expose emails, phone numbers, or private user credentials.
 */
data class SafeReferralDisplayItem(
    val referralId: String,
    val friendLabel: String, // e.g. "Friend #1", "Friend #2"
    val status: ReferralStatus,
    val rewardCoins: Long,
    val progress: Int,
    val target: Int,
    val createdAt: Long,
    val qualifiedAt: Long? = null
)
