package com.example.domain.engine

import com.example.core.config.CoinSide
import com.example.core.config.CoinTossConfig
import com.example.core.security.IdempotencyManager
import com.example.data.model.AccountStatus
import com.example.data.model.TransactionType
import com.example.data.repository.GameRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class DailyCoinTossStats(
    val dailyLimit: Int,
    val attemptsUsedToday: Int,
    val attemptsRemainingToday: Int,
    val resetDate: String
)

sealed class CoinTossResult {
    data class Success(
        val sessionId: String,
        val userChoice: CoinSide,
        val outcome: CoinSide,
        val isWin: Boolean,
        val coinsAwarded: Long,
        val newBalance: Long,
        val attemptsUsedToday: Int,
        val attemptsRemainingToday: Int,
        val transactionId: String?
    ) : CoinTossResult()

    data class LimitReached(
        val attemptsUsedToday: Int,
        val limit: Int,
        val message: String
    ) : CoinTossResult()

    data class GameDisabled(
        val message: String
    ) : CoinTossResult()

    data class AlreadySubmitted(
        val sessionId: String,
        val message: String
    ) : CoinTossResult()

    data class Error(
        val message: String
    ) : CoinTossResult()
}

/**
 * Authoritative Lucky Coin Toss Game Engine.
 * Manages daily limits, server-side coin flipping, atomic rewards, and anti-tamper idempotency.
 */
class CoinTossGameEngine(
    private val rewardEngine: RewardEngine,
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository
) {
    private val completedSessions = ConcurrentHashMap<String, CoinTossResult.Success>()

    suspend fun getDailyStats(userId: String): DailyCoinTossStats {
        val todayStr = gameRepository.getTodayString()
        val stats = gameRepository.getTodayGameStats(userId, CoinTossConfig.GAME_ID)
        val limit = CoinTossConfig.dailyLimit
        val used = stats.playsCount
        val remaining = (limit - used).coerceAtLeast(0)

        return DailyCoinTossStats(
            dailyLimit = limit,
            attemptsUsedToday = used,
            attemptsRemainingToday = remaining,
            resetDate = todayStr
        )
    }

    /**
     * Authoritatively creates and executes a coin flip session.
     */
    suspend fun playCoinToss(
        userId: String,
        userChoice: CoinSide,
        customSessionId: String? = null
    ): CoinTossResult {
        // 1. Verify User & Account
        val user = userRepository.getCurrentUser()
            ?: return CoinTossResult.Error("Authentication required to play.")

        if (user.userId != userId) {
            return CoinTossResult.Error("User session mismatch.")
        }

        if (user.accountStatus != AccountStatus.ACTIVE) {
            return CoinTossResult.Error("Account is not active (${user.accountStatus}).")
        }

        // 2. Check Enabled
        if (!CoinTossConfig.enabled) {
            return CoinTossResult.GameDisabled("Coin Toss is currently disabled.")
        }

        // 3. Verify Daily Limit
        val todayStats = getDailyStats(userId)
        if (todayStats.attemptsRemainingToday <= 0) {
            return CoinTossResult.LimitReached(
                attemptsUsedToday = todayStats.attemptsUsedToday,
                limit = todayStats.dailyLimit,
                message = "Daily limit reached (${todayStats.dailyLimit} tosses/day). Come back tomorrow!"
            )
        }

        // 4. Session & Idempotency Key
        val sessionId = customSessionId ?: UUID.randomUUID().toString()
        completedSessions[sessionId]?.let { return it }

        // 5. Authoritative Fair Flip Outcome
        val outcome = if (Random.nextBoolean()) CoinSide.HEADS else CoinSide.TAILS
        val isWin = (userChoice == outcome)
        val coinsToAward = if (isWin) CoinTossConfig.winningReward else CoinTossConfig.losingReward

        val idempotencyKey = IdempotencyManager.generateToken(
            userId = userId,
            source = "COIN_TOSS",
            sessionId = sessionId
        )

        var newBalance = walletRepository.getCalculatedBalance(userId)
        var transactionId: String? = null

        if (coinsToAward > 0L) {
            val rewardResult = rewardEngine.processReward(
                userId = userId,
                rewardType = TransactionType.COIN_TOSS_REWARD,
                source = "game_coin_toss",
                amount = coinsToAward,
                referenceId = sessionId,
                idempotencyKey = idempotencyKey,
                metadata = "Coin Toss: User chose ${userChoice.displayName}, Outcome ${outcome.displayName} (Win: $isWin)"
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
                    return CoinTossResult.Error(rewardResult.reason)
                }
            }
        }

        // Record gameplay stats
        gameRepository.recordGamePlay(userId, CoinTossConfig.GAME_ID, coinsToAward)
        val updatedStats = getDailyStats(userId)

        val successResult = CoinTossResult.Success(
            sessionId = sessionId,
            userChoice = userChoice,
            outcome = outcome,
            isWin = isWin,
            coinsAwarded = coinsToAward,
            newBalance = newBalance,
            attemptsUsedToday = updatedStats.attemptsUsedToday,
            attemptsRemainingToday = updatedStats.attemptsRemainingToday,
            transactionId = transactionId
        )

        completedSessions[sessionId] = successResult
        return successResult
    }
}
