package com.example.data.model

/**
 * Configuration definition for games and earning activities.
 * 
 * Rewards are never hard-coded in UI components; they are defined and validated
 * through this configuration.
 */
data class GameRewardConfig(
    val gameId: String,
    val title: String,
    val rewardAmount: Long,
    val dailyLimit: Int,
    val cooldownSeconds: Long,
    val enabled: Boolean = true,
    val maxDailyReward: Long,
    val minimumCompletionRequirement: String
)
