package com.example.core.config

/**
 * Configuration for Daily In-App Rewards and Streaks.
 * Centralizes the daily bonus coin reward and streak tiers.
 */
object DailyBonusConfig {
    /**
     * Standard configured daily bonus reward amount in NestCoins.
     */
    const val BONUS_AMOUNT_COINS: Long = 100L

    /**
     * Card labels and descriptions
     */
    const val BONUS_TITLE: String = "Daily Bonus 🎁"
    const val BONUS_SUBTITLE: String = "Come back every day and claim your bonus."

    /**
     * Progressive 7-day streak rewards roadmap
     */
    val STREAK_DAY_REWARDS = listOf(100L, 120L, 150L, 200L, 250L, 350L, 500L)

    fun getRewardForDay(day: Int): Long {
        if (day <= 1) return BONUS_AMOUNT_COINS
        val index = (day - 1).coerceIn(0, STREAK_DAY_REWARDS.size - 1)
        return STREAK_DAY_REWARDS[index]
    }
}
