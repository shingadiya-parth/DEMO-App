package com.example.core.config

/**
 * Reward option for Scratch & Reveal game.
 */
data class ScratchRewardTier(
    val rewardId: String,
    val rewardAmount: Long,
    val weight: Int,
    val label: String,
    val iconEmoji: String = "🎁",
    val enabled: Boolean = true
)

/**
 * Centralized Scratch & Reveal Game Configuration.
 * Governs reward tiers, weighted probabilities, daily limits,
 * reveal threshold, and ad extension parameters.
 */
object ScratchGameConfig {
    const val GAME_ID = "scratch_card"
    const val GAME_TITLE = "Scratch & Reveal"

    var scratchGameEnabled: Boolean = true
    var dailyScratchLimit: Int = 5
    var revealThresholdPercent: Float = 0.70f // Reveal card at 70% scratched
    var adExtraScratchEnabled: Boolean = false
    var maximumAdExtraScratchesPerDay: Int = 3

    /**
     * Authoritative reward tiers with configured weights.
     * Higher weight = higher probability of selection.
     */
    val rewardTiers: List<ScratchRewardTier> = listOf(
        ScratchRewardTier(rewardId = "tier_10", rewardAmount = 10L, weight = 25, label = "10 NestCoins", iconEmoji = "🪙"),
        ScratchRewardTier(rewardId = "tier_20", rewardAmount = 20L, weight = 20, label = "20 NestCoins", iconEmoji = "⭐"),
        ScratchRewardTier(rewardId = "tier_25", rewardAmount = 25L, weight = 18, label = "25 NestCoins", iconEmoji = "✨"),
        ScratchRewardTier(rewardId = "tier_50", rewardAmount = 50L, weight = 15, label = "50 NestCoins", iconEmoji = "🎉"),
        ScratchRewardTier(rewardId = "tier_75", rewardAmount = 75L, weight = 10, label = "75 NestCoins", iconEmoji = "💎"),
        ScratchRewardTier(rewardId = "tier_100", rewardAmount = 100L, weight = 7, label = "100 NestCoins", iconEmoji = "👑"),
        ScratchRewardTier(rewardId = "tier_150", rewardAmount = 150L, weight = 4, label = "150 NestCoins", iconEmoji = "🔥"),
        ScratchRewardTier(rewardId = "tier_200", rewardAmount = 200L, weight = 1, label = "200 NestCoins", iconEmoji = "🏆")
    )

    fun getActiveTiers(): List<ScratchRewardTier> = rewardTiers.filter { it.enabled }
}
