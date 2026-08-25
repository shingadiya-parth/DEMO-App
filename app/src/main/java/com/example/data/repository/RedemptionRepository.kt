package com.example.data.repository

import com.example.core.config.CoinConfig
import com.example.core.security.SecurityValidator
import com.example.data.local.RedemptionDao
import com.example.data.model.RedemptionRequest
import com.example.data.model.RedemptionReward
import com.example.data.model.RedemptionStatus
import com.example.data.model.RewardCategory
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.UUID

sealed class RedemptionResult {
    data class Success(val request: RedemptionRequest, val remainingBalance: Long) : RedemptionResult()
    data class Error(val message: String) : RedemptionResult()
}

class RedemptionRepository(
    private val redemptionDao: RedemptionDao,
    private val walletRepository: WalletRepository,
    private val userRepository: UserRepository
) {

    /**
     * Redemption catalog generated with dynamic coin calculations according to CoinConfig.
     */
    fun getRewardCatalog(): List<RedemptionReward> {
        return listOf(
            RedemptionReward(
                rewardId = "voucher_inr_10_play",
                rewardName = "Google Play Store ₹10",
                description = "Digital code delivered in app after review.",
                category = RewardCategory.GAMING_CREDIT,
                requiredCoins = CoinConfig.currencyToRequiredCoins(10.0), // 7,000 coins
                rewardValueInr = 10.0,
                isAvailable = true,
                dailyRedemptionLimit = 1,
                iconKey = "ic_play_card",
                partnerBrand = "Google Play"
            ),
            RedemptionReward(
                rewardId = "voucher_inr_25_amazon",
                rewardName = "Amazon Pay Gift Card ₹25",
                description = "Shop anything on Amazon or pay bills.",
                category = RewardCategory.GIFT_CARD,
                requiredCoins = CoinConfig.currencyToRequiredCoins(25.0), // 17,500 coins
                rewardValueInr = 25.0,
                isAvailable = true,
                dailyRedemptionLimit = 1,
                iconKey = "ic_amazon_card",
                partnerBrand = "Amazon"
            ),
            RedemptionReward(
                rewardId = "voucher_inr_50_flipkart",
                rewardName = "Flipkart Gift Voucher ₹50",
                description = "Redeemable across all categories on Flipkart.",
                category = RewardCategory.GIFT_CARD,
                requiredCoins = CoinConfig.currencyToRequiredCoins(50.0), // 35,000 coins
                rewardValueInr = 50.0,
                isAvailable = true,
                dailyRedemptionLimit = 1,
                iconKey = "ic_flipkart_card",
                partnerBrand = "Flipkart"
            ),
            RedemptionReward(
                rewardId = "voucher_inr_100_amazon",
                rewardName = "Amazon Pay Gift Card ₹100",
                description = "Instant e-voucher added to your registered email.",
                category = RewardCategory.GIFT_CARD,
                requiredCoins = CoinConfig.currencyToRequiredCoins(100.0), // 70,000 coins
                rewardValueInr = 100.0,
                isAvailable = true,
                dailyRedemptionLimit = 1,
                iconKey = "ic_amazon_card",
                partnerBrand = "Amazon"
            ),
            RedemptionReward(
                rewardId = "voucher_inr_500_master",
                rewardName = "Mega Shopping Voucher ₹500",
                description = "High-tier rewards voucher for top players.",
                category = RewardCategory.GIFT_CARD,
                requiredCoins = CoinConfig.currencyToRequiredCoins(500.0), // 350,000 coins
                rewardValueInr = 500.0,
                isAvailable = true,
                dailyRedemptionLimit = 1,
                iconKey = "ic_gift_master",
                partnerBrand = "Brand Vouchers"
            )
        )
    }

    fun observeUserRequests(userId: String): Flow<List<RedemptionRequest>> {
        return redemptionDao.observeRedemptionRequests(userId)
    }

    /**
     * Submits a redemption request.
     * Flow:
     * 1. Validate security & balance
     * 2. Deduct coins via centralized WalletRepository (transaction type REDEMPTION_DEDUCTION)
     * 3. Insert RedemptionRequest record
     */
    suspend fun submitRedemptionRequest(
        userId: String,
        rewardId: String,
        destinationAccount: String
    ): RedemptionResult {
        val reward = getRewardCatalog().find { it.rewardId == rewardId }
            ?: return RedemptionResult.Error("Selected reward does not exist in catalog")

        val user = userRepository.getCurrentUser()
        val currentBalance = walletRepository.getCalculatedBalance(userId)

        val validation = SecurityValidator.validateRedemptionEligibility(user, currentBalance, reward.requiredCoins)
        if (validation is com.example.core.security.SecurityValidationResult.Rejected) {
            return RedemptionResult.Error(validation.reason)
        }

        val idempotencyKey = "REDEEM:$userId:$rewardId:${System.currentTimeMillis()}"

        val txResult = walletRepository.recordTransaction(
            userId = userId,
            type = TransactionType.REDEMPTION_DEDUCTION,
            source = "redemption_${reward.rewardId}",
            amount = -reward.requiredCoins,
            idempotencyKey = idempotencyKey,
            metadata = "Redemption of ${reward.rewardName} to $destinationAccount"
        )

        return when (txResult) {
            is TransactionResult.Success -> {
                val request = RedemptionRequest(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    rewardId = reward.rewardId,
                    rewardName = reward.rewardName,
                    requiredCoins = reward.requiredCoins,
                    rewardValueInr = reward.rewardValueInr,
                    destinationAccount = destinationAccount,
                    status = RedemptionStatus.REQUESTED,
                    requestedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    adminNote = "Request logged in verification queue"
                )
                redemptionDao.insertRequest(request)
                RedemptionResult.Success(request, txResult.newBalance)
            }
            is TransactionResult.Duplicate -> {
                RedemptionResult.Error("Duplicate redemption request detected")
            }
            is TransactionResult.Error -> {
                RedemptionResult.Error(txResult.message)
            }
        }
    }
}
