package com.example.services.ads

import com.example.domain.engine.RewardEngine
import com.example.domain.engine.RewardGrantResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

interface IAdMobService {
    val isBannerEnabled: StateFlow<Boolean>
    val isInterstitialReady: StateFlow<Boolean>
    val isRewardedAdReady: StateFlow<Boolean>

    fun loadBanner(placement: AdPlacement)
    fun loadInterstitial(placement: AdPlacement)
    fun showInterstitial(placement: AdPlacement, onClosed: () -> Unit)
    fun loadRewardedAd(placement: AdPlacement)

    suspend fun showRewardedAd(
        userId: String,
        placement: AdPlacement,
        actionConfig: AdActionConfig,
        onRewardGranted: (RewardGrantResult) -> Unit,
        onAdFailedOrSkipped: (String) -> Unit
    )
}

/**
 * Centralized AdMob service implementation with verified reward callback pipeline.
 */
class AdMobService(
    private val rewardEngine: RewardEngine
) : IAdMobService {

    private val _isBannerEnabled = MutableStateFlow(true)
    override val isBannerEnabled: StateFlow<Boolean> = _isBannerEnabled.asStateFlow()

    private val _isInterstitialReady = MutableStateFlow(true)
    override val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady.asStateFlow()

    private val _isRewardedAdReady = MutableStateFlow(true)
    override val isRewardedAdReady: StateFlow<Boolean> = _isRewardedAdReady.asStateFlow()

    // Rate limiting & cooldown tracking per action key
    private val lastRewardedAdTimestamps = mutableMapOf<String, Long>()
    private val dailyRewardedAdCounts = mutableMapOf<String, Int>()

    override fun loadBanner(placement: AdPlacement) {
        // Ready for standard AdView binding
        _isBannerEnabled.value = true
    }

    override fun loadInterstitial(placement: AdPlacement) {
        _isInterstitialReady.value = true
    }

    override fun showInterstitial(placement: AdPlacement, onClosed: () -> Unit) {
        // In real integration, InterstitialAd.show(activity) with full callback lifecycle
        onClosed()
    }

    override fun loadRewardedAd(placement: AdPlacement) {
        _isRewardedAdReady.value = true
    }

    override suspend fun showRewardedAd(
        userId: String,
        placement: AdPlacement,
        actionConfig: AdActionConfig,
        onRewardGranted: (RewardGrantResult) -> Unit,
        onAdFailedOrSkipped: (String) -> Unit
    ) {
        val now = System.currentTimeMillis()
        val lastTimestamp = lastRewardedAdTimestamps[actionConfig.actionKey] ?: 0L
        val cooldownMillis = actionConfig.cooldownSeconds * 1000L

        if (now - lastTimestamp < cooldownMillis) {
            val remainingSec = ((cooldownMillis - (now - lastTimestamp)) / 1000).coerceAtLeast(1)
            onAdFailedOrSkipped("Ad cooldown active. Please wait $remainingSec seconds.")
            return
        }

        val currentCount = dailyRewardedAdCounts[actionConfig.actionKey] ?: 0
        if (currentCount >= actionConfig.dailyLimit) {
            onAdFailedOrSkipped("Daily limit of ${actionConfig.dailyLimit} video rewards reached for today.")
            return
        }

        // Simulating verified ad completion callback
        val verificationToken = UUID.randomUUID().toString()
        lastRewardedAdTimestamps[actionConfig.actionKey] = now
        dailyRewardedAdCounts[actionConfig.actionKey] = currentCount + 1

        // CRITICAL: Grant reward ONLY through centralized RewardEngine
        val result = rewardEngine.processActivityReward(
            userId = userId,
            activityType = actionConfig.rewardType,
            sourceTag = actionConfig.source,
            coins = actionConfig.rewardAmount,
            actionToken = verificationToken,
            metadata = "Ad Placement: ${placement.name} | Action: ${actionConfig.actionKey}"
        )

        onRewardGranted(result)
    }
}
