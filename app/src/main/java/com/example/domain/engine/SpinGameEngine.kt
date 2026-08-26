package com.example.domain.engine

import com.example.core.config.SpinGameConfig
import com.example.core.config.SpinRewardSegment
import com.example.core.security.IdempotencyManager
import com.example.data.model.AccountStatus
import com.example.data.model.TransactionType
import com.example.data.repository.GameRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import java.util.UUID
import kotlin.random.Random

data class DailySpinStats(
    val dailyLimit: Int,
    val spinsUsedToday: Int,
    val spinsRemainingToday: Int,
    val resetDate: String
)

sealed class SpinResult {
    data class Success(
        val spinId: String,
        val segment: SpinRewardSegment,
        val segmentIndex: Int,
        val coinsAwarded: Long,
        val newBalance: Long,
        val spinsUsedToday: Int,
        val spinsRemainingToday: Int,
        val transactionId: String
    ) : SpinResult()

    data class LimitReached(
        val spinsUsedToday: Int,
        val limit: Int,
        val message: String
    ) : SpinResult()

    data class GameDisabled(
        val message: String
    ) : SpinResult()

    data class Error(
        val message: String
    ) : SpinResult()
}

/**
 * Authoritative Spin & Win Game Engine.
 * Manages game configuration, fair weighted reward selection,
 * daily spin quotas, idempotency, and atomicity with the central Reward Engine.
 */
class SpinGameEngine(
    private val rewardEngine: RewardEngine,
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository
) {

    /**
     * Returns the authoritative remaining spins for the user today.
     */
    suspend fun getDailySpinStats(userId: String): DailySpinStats {
        val todayStr = gameRepository.getTodayString()
        val stats = gameRepository.getTodayGameStats(userId, SpinGameConfig.GAME_ID)
        val limit = SpinGameConfig.dailySpinLimit
        val used = stats.playsCount
        val remaining = (limit - used).coerceAtLeast(0)

        return DailySpinStats(
            dailyLimit = limit,
            spinsUsedToday = used,
            spinsRemainingToday = remaining,
            resetDate = todayStr
        )
    }

    /**
     * Executes an authoritative game spin.
     * 1. Validates user & account status.
     * 2. Checks game enabled status & daily limits.
     * 3. Selects winning segment via server-side weighted probabilities.
     * 4. Issues atomic transaction via RewardEngine with unique idempotencyKey.
     * 5. Returns validated result with target wheel index.
     */
    suspend fun executeAuthoritativeSpin(
        userId: String,
        customSpinId: String? = null
    ): SpinResult {
        // 1. Verify Authentication & User
        val user = userRepository.getCurrentUser()
            ?: return SpinResult.Error("Authentication required to play.")

        if (user.userId != userId) {
            return SpinResult.Error("User session mismatch.")
        }

        if (user.accountStatus != AccountStatus.ACTIVE) {
            return SpinResult.Error("Account is not active (${user.accountStatus}).")
        }

        // 2. Check Game Enabled
        if (!SpinGameConfig.spinGameEnabled) {
            return SpinResult.GameDisabled("Spin & Win is currently unavailable.")
        }

        // 3. Verify Daily Limit
        val todayStats = getDailySpinStats(userId)
        if (todayStats.spinsRemainingToday <= 0) {
            return SpinResult.LimitReached(
                spinsUsedToday = todayStats.spinsUsedToday,
                limit = todayStats.dailyLimit,
                message = "Daily limit reached. Come back tomorrow for more free spins!"
            )
        }

        // 4. Fair Server-Authoritative Result Generation (Weighted Selection)
        val activeSegments = SpinGameConfig.getActiveSegments()
        if (activeSegments.isEmpty()) {
            return SpinResult.Error("No active wheel segments configured.")
        }

        val totalWeight = activeSegments.sumOf { it.weight }
        val randomPoint = Random.nextInt(totalWeight)

        var accumulatedWeight = 0
        var winningSegment = activeSegments.first()
        var winningIndex = 0

        for ((index, segment) in activeSegments.withIndex()) {
            accumulatedWeight += segment.weight
            if (randomPoint < accumulatedWeight) {
                winningSegment = segment
                winningIndex = index
                break
            }
        }

        // 5. Unique Spin Request & Idempotency Key
        val spinId = customSpinId ?: UUID.randomUUID().toString()
        val idempotencyKey = IdempotencyManager.generateToken(
            userId = userId,
            source = "SPIN_WIN",
            sessionId = spinId
        )

        // 6. Process Reward through Central Reward Engine
        val rewardResult = rewardEngine.processReward(
            userId = userId,
            rewardType = TransactionType.SPIN_REWARD,
            source = "game_spin_win",
            amount = winningSegment.amount,
            referenceId = spinId,
            idempotencyKey = idempotencyKey,
            metadata = "Spin & Win: ${winningSegment.label} Coins (Segment #$winningIndex)"
        )

        return when (rewardResult) {
            is RewardGrantResult.Success -> {
                // Record gameplay stats in database
                gameRepository.recordGamePlay(userId, SpinGameConfig.GAME_ID, winningSegment.amount)
                val updatedStats = getDailySpinStats(userId)

                SpinResult.Success(
                    spinId = spinId,
                    segment = winningSegment,
                    segmentIndex = winningIndex,
                    coinsAwarded = winningSegment.amount,
                    newBalance = rewardResult.newBalance,
                    spinsUsedToday = updatedStats.spinsUsedToday,
                    spinsRemainingToday = updatedStats.spinsRemainingToday,
                    transactionId = rewardResult.transactionId
                )
            }
            is RewardGrantResult.AlreadyClaimed -> {
                // Idempotent duplicate check
                val currentBalance = walletRepository.getCalculatedBalance(userId)
                val currentStats = getDailySpinStats(userId)
                SpinResult.Success(
                    spinId = spinId,
                    segment = winningSegment,
                    segmentIndex = winningIndex,
                    coinsAwarded = rewardResult.existingCoins,
                    newBalance = currentBalance,
                    spinsUsedToday = currentStats.spinsUsedToday,
                    spinsRemainingToday = currentStats.spinsRemainingToday,
                    transactionId = "DUP_$spinId"
                )
            }
            is RewardGrantResult.Rejected -> {
                SpinResult.Error(rewardResult.reason)
            }
        }
    }
}
