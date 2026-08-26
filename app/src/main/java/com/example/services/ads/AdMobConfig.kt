package com.example.services.ads

import com.example.data.model.TransactionType

/**
 * AdMob Environment mode.
 */
enum class AdEnvironment {
    DEVELOPMENT_TEST,
    PRODUCTION
}

/**
 * Supported AdMob Rewarded Action Types.
 */
enum class AdRewardType(
    val actionKey: String,
    val defaultTitle: String,
    val defaultRewardAmount: Long,
    val transactionType: TransactionType,
    val defaultDailyLimit: Int = 10,
    val defaultCooldownSeconds: Long = 60L
) {
    AD_EXTRA_SPIN(
        actionKey = "ad_extra_spin",
        defaultTitle = "Watch Video for Extra Spin",
        defaultRewardAmount = 25L,
        transactionType = TransactionType.AD_REWARD,
        defaultDailyLimit = 10,
        defaultCooldownSeconds = 60L
    ),
    AD_EXTRA_SCRATCH(
        actionKey = "ad_extra_scratch",
        defaultTitle = "Watch Video for Extra Scratch",
        defaultRewardAmount = 25L,
        transactionType = TransactionType.AD_REWARD,
        defaultDailyLimit = 10,
        defaultCooldownSeconds = 60L
    ),
    AD_EXTRA_PUZZLE(
        actionKey = "ad_extra_puzzle",
        defaultTitle = "Watch Video for Extra Puzzle",
        defaultRewardAmount = 25L,
        transactionType = TransactionType.AD_REWARD,
        defaultDailyLimit = 10,
        defaultCooldownSeconds = 60L
    ),
    AD_EXTRA_COIN_TOSS(
        actionKey = "ad_extra_coin_toss",
        defaultTitle = "Watch Video for Extra Toss",
        defaultRewardAmount = 25L,
        transactionType = TransactionType.AD_REWARD,
        defaultDailyLimit = 10,
        defaultCooldownSeconds = 60L
    ),
    AD_EXTRA_TIC_TAC_TOE(
        actionKey = "ad_extra_tictactoe",
        defaultTitle = "Watch Video for Extra Match",
        defaultRewardAmount = 25L,
        transactionType = TransactionType.AD_REWARD,
        defaultDailyLimit = 10,
        defaultCooldownSeconds = 60L
    ),
    AD_EXTRA_BUBBLE_POP(
        actionKey = "ad_extra_bubble_pop",
        defaultTitle = "Watch Video for Extra Round",
        defaultRewardAmount = 25L,
        transactionType = TransactionType.AD_REWARD,
        defaultDailyLimit = 10,
        defaultCooldownSeconds = 60L
    ),
    AD_COIN_REWARD(
        actionKey = "ad_coin_reward",
        defaultTitle = "Watch Video for +25 NestCoins",
        defaultRewardAmount = 25L,
        transactionType = TransactionType.AD_REWARD,
        defaultDailyLimit = 10,
        defaultCooldownSeconds = 60L
    ),
    AD_DOUBLE_GAME_COINS(
        actionKey = "ad_double_coins",
        defaultTitle = "Watch Video for 2x Game Coins",
        defaultRewardAmount = 25L,
        transactionType = TransactionType.AD_REWARD,
        defaultDailyLimit = 10,
        defaultCooldownSeconds = 30L
    )
}

/**
 * Centralized AdMob Configuration Manager.
 * 
 * Securely manages App ID and Ad Unit IDs for Banner, Interstitial, and Rewarded ads.
 * Easily toggles between Development/Test (using Google's official sample IDs) and Production.
 */
object AdMobConfig {

    // Current operating environment
    var environment: AdEnvironment = AdEnvironment.DEVELOPMENT_TEST

    // Google Official AdMob Sample Test IDs
    const val GOOGLE_TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val GOOGLE_TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val GOOGLE_TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val GOOGLE_TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    const val GOOGLE_TEST_REWARDED_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/5354046379"

    // Configurable Production IDs (placeholders until provided in production secrets)
    var productionAppId: String? = null
    var productionBannerId: String? = null
    var productionInterstitialId: String? = null
    var productionRewardedId: String? = null

    // Interstitial Policy Controls
    var isInterstitialEnabled: Boolean = true
    var interstitialCooldownSeconds: Long = 120L // Minimum 2 minutes between interstitials
    var maxInterstitialsPerSession: Int = 5

    // Rewarded Ad Global Controls
    var maxDailyRewardedAdsPerUser: Int = 10
    var rewardedAdCooldownSeconds: Long = 60L // 60 seconds cooldown between rewarded ad claims

    // Per-game Rewarded Ad enablement
    var isSpinRewardedAdEnabled: Boolean = true
    var isScratchRewardedAdEnabled: Boolean = true
    var isPuzzleRewardedAdEnabled: Boolean = true
    var isCoinTossRewardedAdEnabled: Boolean = true
    var isTicTacToeRewardedAdEnabled: Boolean = true
    var isBubblePopRewardedAdEnabled: Boolean = true
    var isDirectCoinRewardedAdEnabled: Boolean = true

    /**
     * Resolves the active App ID.
     */
    fun getAppId(): String {
        return when (environment) {
            AdEnvironment.DEVELOPMENT_TEST -> GOOGLE_TEST_APP_ID
            AdEnvironment.PRODUCTION -> productionAppId ?: GOOGLE_TEST_APP_ID
        }
    }

    /**
     * Resolves the active Banner Ad Unit ID.
     */
    fun getBannerAdUnitId(): String {
        return when (environment) {
            AdEnvironment.DEVELOPMENT_TEST -> GOOGLE_TEST_BANNER_ID
            AdEnvironment.PRODUCTION -> productionBannerId ?: GOOGLE_TEST_BANNER_ID
        }
    }

    /**
     * Resolves the active Interstitial Ad Unit ID.
     */
    fun getInterstitialAdUnitId(): String {
        return when (environment) {
            AdEnvironment.DEVELOPMENT_TEST -> GOOGLE_TEST_INTERSTITIAL_ID
            AdEnvironment.PRODUCTION -> productionInterstitialId ?: GOOGLE_TEST_INTERSTITIAL_ID
        }
    }

    /**
     * Resolves the active Rewarded Ad Unit ID.
     */
    fun getRewardedAdUnitId(): String {
        return when (environment) {
            AdEnvironment.DEVELOPMENT_TEST -> GOOGLE_TEST_REWARDED_ID
            AdEnvironment.PRODUCTION -> productionRewardedId ?: GOOGLE_TEST_REWARDED_ID
        }
    }

    /**
     * Checks if real production IDs are missing when operating in production mode.
     */
    fun getMissingProductionConfigs(): List<String> {
        val missing = mutableListOf<String>()
        if (productionAppId.isNullOrBlank()) missing.add("AdMob App ID (productionAppId)")
        if (productionBannerId.isNullOrBlank()) missing.add("Banner Ad Unit ID (productionBannerId)")
        if (productionInterstitialId.isNullOrBlank()) missing.add("Interstitial Ad Unit ID (productionInterstitialId)")
        if (productionRewardedId.isNullOrBlank()) missing.add("Rewarded Ad Unit ID (productionRewardedId)")
        return missing
    }
}
