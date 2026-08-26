package com.example.services.ads

import com.example.data.model.TransactionType

/**
 * Placements for AdMob formats within the app.
 */
enum class AdPlacement {
    BANNER_HOME,
    BANNER_GAMES,
    BANNER_REWARDS,
    INTERSTITIAL_GAME_FINISH,
    INTERSTITIAL_PUZZLE_COMPLETE,
    REWARDED_SPIN_EXTRA,
    REWARDED_SCRATCH_EXTRA,
    REWARDED_PUZZLE_EXTRA,
    REWARDED_COIN_TOSS_EXTRA,
    REWARDED_TIC_TAC_TOE_EXTRA,
    REWARDED_BUBBLE_POP_EXTRA,
    REWARDED_DOUBLE_GAME_COINS,
    REWARDED_DIRECT_COINS;

    val slotId: String
        get() = when (this) {
            BANNER_HOME, BANNER_GAMES, BANNER_REWARDS -> AdMobConfig.getBannerAdUnitId()
            INTERSTITIAL_GAME_FINISH, INTERSTITIAL_PUZZLE_COMPLETE -> AdMobConfig.getInterstitialAdUnitId()
            else -> AdMobConfig.getRewardedAdUnitId()
        }
}

/**
 * Action configuration for Rewarded Ads.
 */
data class AdActionConfig(
    val rewardType: AdRewardType = AdRewardType.AD_COIN_REWARD,
    val source: String = "admob_reward",
    val rewardAmount: Long = rewardType.defaultRewardAmount,
    val cooldownSeconds: Long = rewardType.defaultCooldownSeconds,
    val dailyLimit: Int = rewardType.defaultDailyLimit,
    val title: String = rewardType.defaultTitle,
    val actionKeyOverride: String? = null
) {
    val actionKey: String get() = actionKeyOverride ?: rewardType.actionKey
    val transactionType: TransactionType get() = rewardType.transactionType
}

sealed class AdRewardCallbackResult {
    data class Completed(
        val actionConfig: AdActionConfig,
        val verificationToken: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : AdRewardCallbackResult()

    data class SkippedOrFailed(val reason: String) : AdRewardCallbackResult()
    data class CooldownActive(val remainingSeconds: Long) : AdRewardCallbackResult()
    data class DailyLimitReached(val maxLimit: Int) : AdRewardCallbackResult()
    data class AdUnavailable(val message: String = "Rewarded ad isn't available right now. Please try again later.") : AdRewardCallbackResult()
}
