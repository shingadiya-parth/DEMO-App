package com.example.core.security

import com.example.core.config.CoinConfig
import com.example.data.model.AccountStatus
import com.example.data.model.GameDefinition
import com.example.data.model.GamePlayStats
import com.example.data.model.UserAccount

sealed class SecurityValidationResult {
    data object Allowed : SecurityValidationResult()
    data class Rejected(val reason: String) : SecurityValidationResult()
}

/**
 * Validates transactions and reward grants before execution.
 * Prevents cheating, invalid game states, or excessive payouts.
 */
object SecurityValidator {

    /**
     * Maximum coin amount allowed in a single gameplay transaction.
     */
    const val MAX_SINGLE_REWARD_CAP: Long = 5000L

    fun validateGameRewardEligibility(
        user: UserAccount?,
        game: GameDefinition,
        stats: GamePlayStats?,
        requestedCoins: Long
    ): SecurityValidationResult {
        if (user == null) {
            return SecurityValidationResult.Rejected("User account not found")
        }

        if (user.accountStatus != AccountStatus.ACTIVE) {
            return SecurityValidationResult.Rejected("Account is not active (${user.accountStatus})")
        }

        if (!game.isEnabled) {
            return SecurityValidationResult.Rejected("Game ${game.gameName} is currently disabled")
        }

        if (requestedCoins <= 0) {
            return SecurityValidationResult.Rejected("Reward coins must be greater than zero")
        }

        if (requestedCoins > MAX_SINGLE_REWARD_CAP) {
            return SecurityValidationResult.Rejected("Reward exceeds maximum single transaction cap")
        }

        // Daily play limit check
        if (stats != null && stats.playsCount >= game.maxDailyPlays) {
            return SecurityValidationResult.Rejected("Daily play limit reached for ${game.gameName} (${game.maxDailyPlays} plays max)")
        }

        // Daily reward cap check
        if (stats != null && (stats.coinsEarnedToday + requestedCoins) > game.maxDailyRewardCoins) {
            return SecurityValidationResult.Rejected("Daily coin cap reached for ${game.gameName} (Max ${game.maxDailyRewardCoins} coins)")
        }

        // Cooldown check
        if (stats != null && game.cooldownMinutes > 0 && stats.lastPlayedTimestamp > 0) {
            val cooldownMillis = game.cooldownMinutes * 60 * 1000L
            val timePassed = System.currentTimeMillis() - stats.lastPlayedTimestamp
            if (timePassed < cooldownMillis) {
                val remainingSeconds = ((cooldownMillis - timePassed) / 1000).coerceAtLeast(1)
                return SecurityValidationResult.Rejected("Game cooldown active. Please wait $remainingSeconds seconds.")
            }
        }

        return SecurityValidationResult.Allowed
    }

    fun validateRedemptionEligibility(
        user: UserAccount?,
        currentBalance: Long,
        requiredCoins: Long
    ): SecurityValidationResult {
        if (user == null) {
            return SecurityValidationResult.Rejected("User account not found")
        }

        if (user.accountStatus != AccountStatus.ACTIVE) {
            return SecurityValidationResult.Rejected("Account is suspended or pending verification")
        }

        if (requiredCoins < CoinConfig.MINIMUM_REDEMPTION_COINS) {
            return SecurityValidationResult.Rejected("Minimum redemption requirement is ${CoinConfig.MINIMUM_REDEMPTION_COINS} coins")
        }

        if (currentBalance < requiredCoins) {
            return SecurityValidationResult.Rejected("Insufficient coins. Balance: $currentBalance, Required: $requiredCoins")
        }

        return SecurityValidationResult.Allowed
    }
}
