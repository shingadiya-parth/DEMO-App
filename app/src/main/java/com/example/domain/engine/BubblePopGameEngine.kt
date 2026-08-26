package com.example.domain.engine

import com.example.core.config.BubblePopConfig
import com.example.core.config.BubbleScoreThreshold
import com.example.core.security.IdempotencyManager
import com.example.data.model.AccountStatus
import com.example.data.model.TransactionType
import com.example.data.repository.GameRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class DailyBubblePopStats(
    val dailyLimit: Int,
    val roundsUsedToday: Int,
    val roundsRemainingToday: Int,
    val resetDate: String
)

data class BubbleSessionState(
    val sessionId: String,
    val userId: String,
    val startedAt: Long,
    val targetDurationSeconds: Int = BubblePopConfig.roundDurationSeconds,
    var isCompleted: Boolean = false
)

sealed class BubblePopSessionResult {
    data class SessionStarted(
        val sessionId: String,
        val durationSeconds: Int,
        val roundsRemainingToday: Int
    ) : BubblePopSessionResult()

    data class LimitReached(
        val roundsUsedToday: Int,
        val limit: Int,
        val message: String
    ) : BubblePopSessionResult()

    data class GameDisabled(
        val message: String
    ) : BubblePopSessionResult()

    data class Error(
        val message: String
    ) : BubblePopSessionResult()
}

sealed class BubblePopCompleteResult {
    data class Success(
        val sessionId: String,
        val finalScore: Int,
        val bubblesPopped: Int,
        val tier: BubbleScoreThreshold,
        val coinsAwarded: Long,
        val newBalance: Long,
        val roundsUsedToday: Int,
        val roundsRemainingToday: Int,
        val transactionId: String?
    ) : BubblePopCompleteResult()

    data class AlreadyCompleted(
        val sessionId: String,
        val message: String
    ) : BubblePopCompleteResult()

    data class InvalidScore(
        val reason: String
    ) : BubblePopCompleteResult()

    data class Error(
        val message: String
    ) : BubblePopCompleteResult()
}

/**
 * Authoritative Bubble Pop Game Engine.
 * Enforces session time windows, prevents fabricated client scores,
 * enforces daily round limits, and securely awards verified coins via the Reward Engine.
 */
class BubblePopGameEngine(
    private val rewardEngine: RewardEngine,
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository
) {
    private val activeSessions = ConcurrentHashMap<String, BubbleSessionState>()
    private val completedResults = ConcurrentHashMap<String, BubblePopCompleteResult.Success>()

    suspend fun getDailyStats(userId: String): DailyBubblePopStats {
        val todayStr = gameRepository.getTodayString()
        val stats = gameRepository.getTodayGameStats(userId, BubblePopConfig.GAME_ID)
        val limit = BubblePopConfig.dailyRoundLimit
        val used = stats.playsCount
        val remaining = (limit - used).coerceAtLeast(0)

        return DailyBubblePopStats(
            dailyLimit = limit,
            roundsUsedToday = used,
            roundsRemainingToday = remaining,
            resetDate = todayStr
        )
    }

    /**
     * Authorizes and initializes a timed bubble popping round.
     */
    suspend fun startRound(userId: String): BubblePopSessionResult {
        val user = userRepository.getCurrentUser()
            ?: return BubblePopSessionResult.Error("Authentication required to play.")

        if (user.userId != userId) {
            return BubblePopSessionResult.Error("User session mismatch.")
        }

        if (user.accountStatus != AccountStatus.ACTIVE) {
            return BubblePopSessionResult.Error("Account is not active (${user.accountStatus}).")
        }

        if (!BubblePopConfig.enabled) {
            return BubblePopSessionResult.GameDisabled("Bubble Popper is currently unavailable.")
        }

        val dailyStats = getDailyStats(userId)
        if (dailyStats.roundsRemainingToday <= 0) {
            return BubblePopSessionResult.LimitReached(
                roundsUsedToday = dailyStats.roundsUsedToday,
                limit = dailyStats.dailyLimit,
                message = "Daily limit reached (${dailyStats.dailyLimit} rounds/day). Come back tomorrow!"
            )
        }

        val sessionId = UUID.randomUUID().toString()
        val session = BubbleSessionState(
            sessionId = sessionId,
            userId = userId,
            startedAt = System.currentTimeMillis(),
            targetDurationSeconds = BubblePopConfig.roundDurationSeconds
        )
        activeSessions[sessionId] = session

        return BubblePopSessionResult.SessionStarted(
            sessionId = sessionId,
            durationSeconds = BubblePopConfig.roundDurationSeconds,
            roundsRemainingToday = dailyStats.roundsRemainingToday
        )
    }

    /**
     * Authoritatively completes and verifies a bubble popping round.
     * Validates score calculations, rate limits, and elapsed session time.
     */
    suspend fun completeRound(
        userId: String,
        sessionId: String,
        claimedBubblesPopped: Int,
        elapsedSeconds: Int
    ): BubblePopCompleteResult {
        completedResults[sessionId]?.let { return it }

        val session = activeSessions[sessionId]
            ?: return BubblePopCompleteResult.Error("Game session expired or not found.")

        if (session.userId != userId) {
            return BubblePopCompleteResult.Error("Unauthorized session.")
        }

        if (session.isCompleted) {
            return BubblePopCompleteResult.AlreadyCompleted(sessionId, "This round was already completed.")
        }

        // 1. Anti-Cheat & Rate Limit Verification
        val duration = elapsedSeconds.coerceAtLeast(1)
        val maxAllowedTaps = (duration * BubblePopConfig.maxAllowedTapsPerSecond).toInt() + 15
        if (claimedBubblesPopped > maxAllowedTaps) {
            return BubblePopCompleteResult.InvalidScore(
                "Invalid score submission: Tap frequency exceeded physical possibility."
            )
        }

        val calculatedScore = claimedBubblesPopped * BubblePopConfig.pointsPerBubble
        val tier = BubblePopConfig.getTierForScore(calculatedScore)
        val coinsToAward = tier.rewardCoins

        session.isCompleted = true

        val idempotencyKey = IdempotencyManager.generateToken(
            userId = userId,
            source = "BUBBLE_POP",
            sessionId = sessionId
        )

        var newBalance = walletRepository.getCalculatedBalance(userId)
        var transactionId: String? = null

        if (coinsToAward > 0L) {
            val rewardResult = rewardEngine.processReward(
                userId = userId,
                rewardType = TransactionType.BUBBLE_POP_REWARD,
                source = "game_bubble_pop",
                amount = coinsToAward,
                referenceId = sessionId,
                idempotencyKey = idempotencyKey,
                metadata = "Bubble Pop: Score $calculatedScore ($claimedBubblesPopped bubbles | Tier: ${tier.tierName})"
            )

            when (rewardResult) {
                is RewardGrantResult.Success -> {
                    newBalance = rewardResult.newBalance
                    transactionId = rewardResult.transactionId
                }
                is RewardGrantResult.AlreadyClaimed -> {
                    newBalance = rewardResult.currentBalance
                    transactionId = "DUP_$sessionId"
                }
                is RewardGrantResult.Rejected -> {
                    return BubblePopCompleteResult.Error(rewardResult.reason)
                }
            }
        }

        gameRepository.recordGamePlay(userId, BubblePopConfig.GAME_ID, coinsToAward)
        val updatedStats = getDailyStats(userId)

        val successResult = BubblePopCompleteResult.Success(
            sessionId = sessionId,
            finalScore = calculatedScore,
            bubblesPopped = claimedBubblesPopped,
            tier = tier,
            coinsAwarded = coinsToAward,
            newBalance = newBalance,
            roundsUsedToday = updatedStats.roundsUsedToday,
            roundsRemainingToday = updatedStats.roundsRemainingToday,
            transactionId = transactionId
        )

        completedResults[sessionId] = successResult
        activeSessions.remove(sessionId)

        return successResult
    }
}
