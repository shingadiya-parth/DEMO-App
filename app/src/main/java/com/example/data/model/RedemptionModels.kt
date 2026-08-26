package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Category of reward redemption.
 */
enum class RewardCategory(val title: String, val description: String) {
    GIFT_CARDS("Gift Cards", "Digital gift cards from top brands"),
    DIGITAL_REWARDS("Digital Rewards", "Entertainment, OTT & online passes"),
    UPI_REWARDS("UPI Rewards", "Direct UPI / Bank transfer rewards"),
    GAMING_VOUCHERS("Gaming Vouchers", "Play Store, gaming credits & passes"),
    OTHER_REWARDS("Other Rewards", "Exclusive partner perks")
}

/**
 * Real-time stock / availability state for a reward.
 */
enum class RewardStockStatus(val label: String, val badgeText: String) {
    AVAILABLE("Available", "In Stock"),
    LOW_STOCK("Low Stock", "Limited Stock"),
    OUT_OF_STOCK("Out of Stock", "Sold Out"),
    DISABLED("Unavailable", "Disabled")
}

/**
 * Status of user redemption requests.
 * Stored permanently in the database and immutable by unauthorized clients.
 */
enum class RedemptionStatus(val label: String, val description: String) {
    PENDING("Pending", "Request received and queued for review"),
    PROCESSING("Processing", "Being processed by admin verification team"),
    APPROVED("Approved", "Approved and scheduled for voucher generation"),
    FULFILLED("Fulfilled", "Voucher/code delivered to your registered destination"),
    REJECTED("Rejected", "Request declined by system review"),
    CANCELLED("Cancelled", "Cancelled by user or security policy"),
    REFUNDED("Refunded", "Coins refunded back to your wallet")
}

/**
 * Centralized, configurable reward catalog item.
 */
data class RedemptionReward(
    val rewardId: String,
    val name: String,
    val description: String,
    val imageKey: String = "ic_gift",
    val partnerBrand: String = "Brand Voucher",
    val category: RewardCategory,
    val value: Double,
    val currency: String = "INR",
    val requiredCoins: Long,
    val enabled: Boolean = true,
    val stockStatus: RewardStockStatus = RewardStockStatus.AVAILABLE,
    val dailyRedemptionLimit: Int = 1,
    val totalRedemptionLimit: Int = 10,
    val minimumAccountAgeDays: Long = 0L,
    val minimumCompletedGames: Int = 0,
    val termsAndConditions: List<String> = listOf(
        "Voucher code delivered in app and via email within 24-48 business hours after manual review.",
        "Ensure your delivery details (email/UPI ID) are accurate before confirmation.",
        "Redemptions once submitted deduct coins immediately from your wallet ledger.",
        "In case of rejection or cancellation, full coins are refunded atomically with an auditable transaction.",
        "Non-transferable and subject to partner brand terms."
    ),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Backward compatibility aliases
    val rewardName: String get() = name
    val rewardValueInr: Double get() = value
    val iconKey: String get() = imageKey
    val isAvailable: Boolean
        get() = enabled && (stockStatus == RewardStockStatus.AVAILABLE || stockStatus == RewardStockStatus.LOW_STOCK)
}

/**
 * Result of checking a user's eligibility for a specific reward.
 */
sealed class RedemptionEligibilityResult {
    data object Eligible : RedemptionEligibilityResult()
    data class Ineligible(
        val reason: String,
        val shortBadge: String,
        val coinsNeeded: Long = 0L
    ) : RedemptionEligibilityResult()
}

/**
 * User Redemption Request Entity.
 * 
 * Includes snapshot fields for reward name, value, and required coins
 * to preserve immutable historical accuracy even if catalog prices change later.
 */
@Entity(
    tableName = "redemption_requests",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["status"]),
        Index(value = ["idempotency_key"], unique = true),
        Index(value = ["created_at"])
    ]
)
data class RedemptionRequest(
    @PrimaryKey
    @ColumnInfo(name = "redemption_id")
    val redemptionId: String = "red_${UUID.randomUUID()}",

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "reward_id")
    val rewardId: String,

    @ColumnInfo(name = "reward_name_snapshot")
    val rewardNameSnapshot: String,

    @ColumnInfo(name = "reward_value_snapshot")
    val rewardValueSnapshot: Double,

    @ColumnInfo(name = "required_coins_snapshot")
    val requiredCoinsSnapshot: Long,

    @ColumnInfo(name = "currency_snapshot")
    val currencySnapshot: String = "INR",

    @ColumnInfo(name = "destination_account")
    val destinationAccount: String,

    @ColumnInfo(name = "status")
    val status: RedemptionStatus = RedemptionStatus.PENDING,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "processed_at")
    val processedAt: Long? = null,

    @ColumnInfo(name = "transaction_id")
    val transactionId: String? = null,

    @ColumnInfo(name = "idempotency_key")
    val idempotencyKey: String = "idemp_red_${UUID.randomUUID()}",

    @ColumnInfo(name = "admin_note")
    val adminNote: String? = null,

    @ColumnInfo(name = "failure_reason")
    val failureReason: String? = null
) {
    // Backward compatibility aliases
    val id: String get() = redemptionId
    val requestedAt: Long get() = createdAt
    val rewardName: String get() = rewardNameSnapshot
    val rewardValueInr: Double get() = rewardValueSnapshot
    val requiredCoins: Long get() = requiredCoinsSnapshot
}
