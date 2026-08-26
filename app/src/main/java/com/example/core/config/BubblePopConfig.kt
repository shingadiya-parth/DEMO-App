package com.example.core.config

/**
 * Score threshold tier for Bubble Pop coin rewards.
 */
data class BubbleScoreThreshold(
    val minScore: Int,
    val maxScore: Int,
    val rewardCoins: Long,
    val tierName: String
)

/**
 * Centralized configuration for Bubble Pop game.
 */
object BubblePopConfig {
    const val GAME_ID = "bubble_pop"
    const val GAME_TITLE = "Bubble Popper"

    var enabled: Boolean = true
    var dailyRoundLimit: Int = 5
    var roundDurationSeconds: Int = 30
    var pointsPerBubble: Int = 5
    var maxAllowedTapsPerSecond: Double = 8.0
    var cooldownSeconds: Long = 0L
    var adExtraAttemptEnabled: Boolean = false
    var maximumAdExtraRoundsPerDay: Int = 3

    val rewardThresholds: List<BubbleScoreThreshold> = listOf(
        BubbleScoreThreshold(minScore = 0, maxScore = 49, rewardCoins = 0L, tierName = "Beginner"),
        BubbleScoreThreshold(minScore = 50, maxScore = 99, rewardCoins = 10L, tierName = "Bronze Popper"),
        BubbleScoreThreshold(minScore = 100, maxScore = 149, rewardCoins = 25L, tierName = "Silver Popper"),
        BubbleScoreThreshold(minScore = 150, maxScore = Int.MAX_VALUE, rewardCoins = 50L, tierName = "Gold Champion")
    )

    fun calculateCoinsForScore(score: Int): Long {
        val tier = rewardThresholds.firstOrNull { score in it.minScore..it.maxScore }
        return tier?.rewardCoins ?: 0L
    }

    fun getTierForScore(score: Int): BubbleScoreThreshold {
        return rewardThresholds.firstOrNull { score in it.minScore..it.maxScore }
            ?: rewardThresholds.last()
    }
}
