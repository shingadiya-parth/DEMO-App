package com.example.services.ads

import com.example.data.model.TransactionType

/**
 * Placements for AdMob formats within the app.
 */
enum class AdPlacement(val slotId: String) {
    BANNER_HOME("ca-app-pub-3940256099942544/6300978111"), // AdMob sample test ID
    BANNER_GAMES("ca-app-pub-3940256099942544/6300978111"),
    INTERSTITIAL_GAME_FINISH("ca-app-pub-3940256099942544/1033173712"),
    REWARDED_SPIN_EXTRA("ca-app-pub-3940256099942544/5224354917"),
    REWARDED_DOUBLE_GAME_COINS("ca-app-pub-3940256099942544/5224354917"),
    REWARDED_DAILY_BONUS_BOOST("ca-app-pub-3940256099942544/5224354917")
}

/**
 * Action configuration for Rewarded Ads.
 * Each rewarded ad action specifies:
 * - reward type
 * - reward amount
 * - source
 * - cooldown in seconds
 * - daily limit
 */
data class AdActionConfig(
    val actionKey: String,
    val rewardType: TransactionType = TransactionType.AD_REWARD,
    val rewardAmount: Long,
    val source: String,
    val cooldownSeconds: Long = 60L,
    val dailyLimit: Int = 10,
    val title: String = "Watch Video for Bonus Coins"
)

sealed class AdRewardCallbackResult {
    data class Completed(
        val actionConfig: AdActionConfig,
        val verificationToken: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : AdRewardCallbackResult()

    data class SkippedOrFailed(val reason: String) : AdRewardCallbackResult()
    data class CooldownActive(val remainingSeconds: Long) : AdRewardCallbackResult()
    data class DailyLimitReached(val maxLimit: Int) : AdRewardCallbackResult()
}
