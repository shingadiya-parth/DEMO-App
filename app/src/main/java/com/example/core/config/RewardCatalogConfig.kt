package com.example.core.config

import com.example.data.model.RedemptionReward
import com.example.data.model.RewardCategory
import com.example.data.model.RewardStockStatus

/**
 * Centralized configuration for the Rewards Catalog and redemption policies.
 * 
 * Rules:
 * - Dynamic & configurable rewards list (not hardcoded in UI)
 * - Transparent coin-to-value calculations based on CoinConfig (700 Coins = ₹1)
 * - Safe limits per user to prevent automated abuse
 */
object RewardCatalogConfig {

    /**
     * Default global limits
     */
    const val GLOBAL_DAILY_REDEMPTION_LIMIT_PER_USER: Int = 3
    const val GLOBAL_MONTHLY_REDEMPTION_LIMIT_PER_USER: Int = 20
    const val MINIMUM_ACCOUNT_AGE_HOURS: Long = 0L // Can be adjusted as needed
    const val MINIMUM_COMPLETED_GAMES_REQUIRED: Int = 0 // Can be adjusted as needed

    /**
     * Configurable list of catalog rewards.
     */
    fun getDefaultCatalog(): List<RedemptionReward> {
        return listOf(
            RedemptionReward(
                rewardId = "rew_inr_10_play",
                name = "Google Play ₹10 Code",
                description = "Digital redeem code for apps, games, in-game items, and books on Google Play.",
                imageKey = "ic_play_card",
                partnerBrand = "Google Play",
                category = RewardCategory.GAMING_VOUCHERS,
                value = 10.0,
                currency = "INR",
                requiredCoins = 7000L, // 700 coins = ₹1
                enabled = true,
                stockStatus = RewardStockStatus.AVAILABLE,
                dailyRedemptionLimit = 1,
                totalRedemptionLimit = 10,
                termsAndConditions = listOf(
                    "Delivered as a 16-character alphanumeric Google Play promo code.",
                    "Valid for Indian Google Play accounts only.",
                    "Redemption code is delivered to your registered email after security verification.",
                    "No expiry date once added to your Google Play balance."
                )
            ),
            RedemptionReward(
                rewardId = "rew_inr_10_amazon",
                name = "Amazon Pay ₹10 Voucher",
                description = "Add ₹10 to your Amazon Pay wallet balance for shopping, bills, and recharges.",
                imageKey = "ic_amazon_card",
                partnerBrand = "Amazon Pay",
                category = RewardCategory.GIFT_CARDS,
                value = 10.0,
                currency = "INR",
                requiredCoins = 7000L,
                enabled = true,
                stockStatus = RewardStockStatus.AVAILABLE,
                dailyRedemptionLimit = 1,
                totalRedemptionLimit = 10,
                termsAndConditions = listOf(
                    "Amazon Gift Card code will be delivered to your registered email.",
                    "Can be added directly to your Amazon Pay balance.",
                    "Valid for 1 year from the date of issuance.",
                    "Applicable on all Amazon.in orders and merchant payments."
                )
            ),
            RedemptionReward(
                rewardId = "rew_inr_25_upi",
                name = "UPI Direct ₹25 Transfer",
                description = "Direct instant transfer to your verified UPI ID (GPay / PhonePe / Paytm).",
                imageKey = "ic_upi_transfer",
                partnerBrand = "UPI Transfer",
                category = RewardCategory.UPI_REWARDS,
                value = 25.0,
                currency = "INR",
                requiredCoins = 17500L,
                enabled = true,
                stockStatus = RewardStockStatus.AVAILABLE,
                dailyRedemptionLimit = 1,
                totalRedemptionLimit = 5,
                termsAndConditions = listOf(
                    "Please double check your VPA / UPI ID before confirming.",
                    "Transfers are processed within 24-48 business hours following verification.",
                    "Invalid UPI IDs will result in automatic rejection with full coin refund."
                )
            ),
            RedemptionReward(
                rewardId = "rew_inr_25_amazon",
                name = "Amazon Pay ₹25 Voucher",
                description = "Shop anything on Amazon or pay bills with instant ₹25 gift voucher.",
                imageKey = "ic_amazon_card",
                partnerBrand = "Amazon Pay",
                category = RewardCategory.GIFT_CARDS,
                value = 25.0,
                currency = "INR",
                requiredCoins = 17500L,
                enabled = true,
                stockStatus = RewardStockStatus.AVAILABLE,
                dailyRedemptionLimit = 1,
                totalRedemptionLimit = 10,
                termsAndConditions = listOf(
                    "Claimable via the Amazon mobile app or website.",
                    "Applicable across Amazon Fresh, Electronics, Fashion, and Bill Pay.",
                    "Cannot be transferred to another Amazon account once claimed."
                )
            ),
            RedemptionReward(
                rewardId = "rew_inr_50_flipkart",
                name = "Flipkart ₹50 E-Gift Voucher",
                description = "Use this gift card to buy products across all categories on Flipkart.",
                imageKey = "ic_flipkart_card",
                partnerBrand = "Flipkart",
                category = RewardCategory.GIFT_CARDS,
                value = 50.0,
                currency = "INR",
                requiredCoins = 35000L,
                enabled = true,
                stockStatus = RewardStockStatus.AVAILABLE,
                dailyRedemptionLimit = 1,
                totalRedemptionLimit = 5,
                termsAndConditions = listOf(
                    "Card number and PIN will be sent to your email after verification.",
                    "Valid for 12 months from issuance date.",
                    "Can be combined with other Flipkart payment modes."
                )
            ),
            RedemptionReward(
                rewardId = "rew_inr_50_upi",
                name = "UPI Direct ₹50 Transfer",
                description = "Direct instant transfer of ₹50 to your verified UPI virtual payment address.",
                imageKey = "ic_upi_transfer",
                partnerBrand = "UPI Transfer",
                category = RewardCategory.UPI_REWARDS,
                value = 50.0,
                currency = "INR",
                requiredCoins = 35000L,
                enabled = true,
                stockStatus = RewardStockStatus.AVAILABLE,
                dailyRedemptionLimit = 1,
                totalRedemptionLimit = 5,
                termsAndConditions = listOf(
                    "Transfer to verified bank UPI ID only.",
                    "Requires valid active account with clean gameplay history.",
                    "Manual admin security audit conducted prior to transfer dispatch."
                )
            ),
            RedemptionReward(
                rewardId = "rew_inr_100_play",
                name = "Google Play ₹100 Code",
                description = "Top-tier digital gift code for premium apps, game passes, and movies.",
                imageKey = "ic_play_card",
                partnerBrand = "Google Play",
                category = RewardCategory.GAMING_VOUCHERS,
                value = 100.0,
                currency = "INR",
                requiredCoins = 70000L,
                enabled = true,
                stockStatus = RewardStockStatus.LOW_STOCK,
                dailyRedemptionLimit = 1,
                totalRedemptionLimit = 3,
                termsAndConditions = listOf(
                    "16-digit Google Play voucher delivered in-app upon approval.",
                    "Non-refundable once code is revealed/delivered.",
                    "Subject to Google Play Terms of Service."
                )
            ),
            RedemptionReward(
                rewardId = "rew_inr_100_amazon",
                name = "Amazon Pay ₹100 Gift Card",
                description = "High value ₹100 Amazon Pay gift voucher for electronics and shopping.",
                imageKey = "ic_amazon_card",
                partnerBrand = "Amazon Pay",
                category = RewardCategory.GIFT_CARDS,
                value = 100.0,
                currency = "INR",
                requiredCoins = 70000L,
                enabled = true,
                stockStatus = RewardStockStatus.AVAILABLE,
                dailyRedemptionLimit = 1,
                totalRedemptionLimit = 3,
                termsAndConditions = listOf(
                    "Instant digital code sent to your registered email address.",
                    "Valid for 1 year from the date of issue on Amazon.in."
                )
            ),
            RedemptionReward(
                rewardId = "rew_inr_100_bms",
                name = "BookMyShow ₹100 Voucher",
                description = "Enjoy ₹100 discount on movies, live events, and theater tickets.",
                imageKey = "ic_bms_card",
                partnerBrand = "BookMyShow",
                category = RewardCategory.DIGITAL_REWARDS,
                value = 100.0,
                currency = "INR",
                requiredCoins = 70000L,
                enabled = true,
                stockStatus = RewardStockStatus.AVAILABLE,
                dailyRedemptionLimit = 1,
                totalRedemptionLimit = 2,
                termsAndConditions = listOf(
                    "Applicable on movies, plays, concerts and sports tickets on BookMyShow app.",
                    "Single-use promo voucher code."
                )
            ),
            RedemptionReward(
                rewardId = "rew_inr_500_master",
                name = "Mega Shopping Voucher ₹500",
                description = "Elite rewards voucher for top-tier master players.",
                imageKey = "ic_gift_master",
                partnerBrand = "Brand Vouchers",
                category = RewardCategory.GIFT_CARDS,
                value = 500.0,
                currency = "INR",
                requiredCoins = 350000L,
                enabled = true,
                stockStatus = RewardStockStatus.LOW_STOCK,
                dailyRedemptionLimit = 1,
                totalRedemptionLimit = 1,
                termsAndConditions = listOf(
                    "High-value VIP rewards tier.",
                    "Requires multi-stage account audit and phone verification.",
                    "Dispatched via secure digital voucher delivery within 72 hours."
                )
            )
        )
    }
}
