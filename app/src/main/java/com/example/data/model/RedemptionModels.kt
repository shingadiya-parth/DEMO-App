package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Category of reward redemption.
 */
enum class RewardCategory(val title: String) {
    GIFT_CARD("Gift Vouchers"),
    DIGITAL_WALLET("Direct Transfers"),
    GAMING_CREDIT("Gaming Passes")
}

/**
 * Available reward item definition in the redemption catalog.
 */
data class RedemptionReward(
    val rewardId: String,
    val rewardName: String,
    val description: String,
    val category: RewardCategory,
    val requiredCoins: Long,
    val rewardValueInr: Double,
    val isAvailable: Boolean = true,
    val dailyRedemptionLimit: Int = 1,
    val iconKey: String,
    val partnerBrand: String
)

/**
 * Status of user redemption requests.
 */
enum class RedemptionStatus(val label: String) {
    REQUESTED("Processing Requested"),
    UNDER_REVIEW("Security Review"),
    COMPLETED("Completed"),
    REJECTED("Rejected")
}

/**
 * User Redemption Request Entity.
 */
@Entity(tableName = "redemption_requests")
data class RedemptionRequest(
    @PrimaryKey
    @ColumnInfo(name = "request_id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "reward_id")
    val rewardId: String,

    @ColumnInfo(name = "reward_name")
    val rewardName: String,

    @ColumnInfo(name = "required_coins")
    val requiredCoins: Long,

    @ColumnInfo(name = "reward_value_inr")
    val rewardValueInr: Double,

    @ColumnInfo(name = "destination_account")
    val destinationAccount: String,

    @ColumnInfo(name = "status")
    val status: RedemptionStatus = RedemptionStatus.REQUESTED,

    @ColumnInfo(name = "requested_at")
    val requestedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "admin_note")
    val adminNote: String? = null
)
