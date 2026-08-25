package com.example.domain.engine

import com.example.data.model.GameRewardConfig
import java.util.concurrent.ConcurrentHashMap

/**
 * Centralized Game & Activity Reward Configuration Manager.
 * 
 * Defines reward amounts, daily play caps, cooldowns, and validation thresholds.
 * Central single source of truth for reward rules.
 */
object GameRewardConfigManager {

    private val configs = ConcurrentHashMap<String, GameRewardConfig>().apply {
        put("spin_win", GameRewardConfig(
            gameId = "spin_win",
            title = "Spin & Win",
            rewardAmount = 50L,
            dailyLimit = 10,
            cooldownSeconds = 300L, // 5 minutes
            enabled = true,
            maxDailyReward = 500L,
            minimumCompletionRequirement = "Complete full wheel spin animation"
        ))

        put("scratch_card", GameRewardConfig(
            gameId = "scratch_card",
            title = "Scratch & Reveal",
            rewardAmount = 40L,
            dailyLimit = 10,
            cooldownSeconds = 180L, // 3 minutes
            enabled = true,
            maxDailyReward = 400L,
            minimumCompletionRequirement = "Reveal at least 70% of scratch surface"
        ))

        put("tile_puzzle", GameRewardConfig(
            gameId = "tile_puzzle",
            title = "Puzzle Master",
            rewardAmount = 60L,
            dailyLimit = 15,
            cooldownSeconds = 60L,
            enabled = true,
            maxDailyReward = 900L,
            minimumCompletionRequirement = "Assemble all 9 sliding tiles correctly"
        ))

        put("coin_toss", GameRewardConfig(
            gameId = "coin_toss",
            title = "Coin Toss",
            rewardAmount = 25L,
            dailyLimit = 20,
            cooldownSeconds = 30L,
            enabled = true,
            maxDailyReward = 500L,
            minimumCompletionRequirement = "Predict coin flip outcome"
        ))

        put("tic_tac_toe", GameRewardConfig(
            gameId = "tic_tac_toe",
            title = "Tic-Tac-Toe",
            rewardAmount = 35L,
            dailyLimit = 15,
            cooldownSeconds = 45L,
            enabled = true,
            maxDailyReward = 525L,
            minimumCompletionRequirement = "Win or draw 3 in a row match"
        ))

        put("word_guess", GameRewardConfig(
            gameId = "word_guess",
            title = "Word Puzzle",
            rewardAmount = 50L,
            dailyLimit = 12,
            cooldownSeconds = 60L,
            enabled = true,
            maxDailyReward = 600L,
            minimumCompletionRequirement = "Unscramble target vocabulary word"
        ))

        put("bubble_pop", GameRewardConfig(
            gameId = "bubble_pop",
            title = "Bubble Pop",
            rewardAmount = 45L,
            dailyLimit = 15,
            cooldownSeconds = 45L,
            enabled = true,
            maxDailyReward = 675L,
            minimumCompletionRequirement = "Pop 15 target color bubbles within 30 seconds"
        ))

        put("daily_challenge", GameRewardConfig(
            gameId = "daily_challenge",
            title = "Daily Challenge",
            rewardAmount = 100L,
            dailyLimit = 1,
            cooldownSeconds = 86400L, // 24 hours
            enabled = true,
            maxDailyReward = 100L,
            minimumCompletionRequirement = "Complete daily streak challenge task"
        ))
    }

    fun getConfig(gameId: String): GameRewardConfig? {
        return configs[gameId]
    }

    fun getAllConfigs(): List<GameRewardConfig> {
        return configs.values.toList()
    }

    fun updateConfig(config: GameRewardConfig) {
        configs[config.gameId] = config
    }

    fun isGameEnabled(gameId: String): Boolean {
        return configs[gameId]?.enabled ?: false
    }
}
