package com.example.core.config

/**
 * Supported sides for coin flip.
 */
enum class CoinSide(val displayName: String) {
    HEADS("Heads"),
    TAILS("Tails")
}

/**
 * Centralized configuration for Lucky Coin Toss game.
 */
object CoinTossConfig {
    const val GAME_ID = "coin_toss"
    const val GAME_TITLE = "Lucky Coin Toss"

    var enabled: Boolean = true
    var dailyLimit: Int = 5
    var winningReward: Long = 20L
    var losingReward: Long = 0L
    var animationDurationMs: Long = 1600L
    var cooldownSeconds: Long = 0L
    var adExtraAttemptEnabled: Boolean = false
    var maximumAdExtraAttemptsPerDay: Int = 3
}
