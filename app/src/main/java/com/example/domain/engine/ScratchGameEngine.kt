package com.example.domain.engine

import com.example.core.config.ScratchGameConfig
import com.example.core.config.ScratchRewardTier
import com.example.core.security.IdempotencyManager
import com.example.data.model.AccountStatus
import com.example.data.model.ScratchSession
import com.example.data.model.ScratchSessionStatus
import com.example.data.model.TransactionType
import com.example.data.repository.GameRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class DailyScratchStats(
    val dailyLimit: Int,
    val scratchesUsedToday: Int,
    val scratchesRemainingToday: Int,
    val resetDate: String
)

sealed class ScratchCardResult {
    data class CardCreated(
        val session: ScratchSession,
        val tier: ScratchRewardTier,
        val scratchesUsedToday: Int,
        val scratchesRemainingToday: Int
    ) : ScratchCardResult()

    data class RewardGranted(
        val session: ScratchSession,
        val coinsAwarded: Long,
        val newBalance: Long,
        val scratchesUsedToday: Int,
        val scratchesRemainingToday: Int,
        val transactionId: String
    ) : ScratchCardResult()

    data class LimitReached(
        val scratchesUsedToday: Int,
        val limit: Int,
        val message: String
    ) : ScratchCardResult()

    data class AlreadyCompleted(
        val message: String,
        val coinsAwarded: Long
    ) : ScratchCardResult()

    data class GameDisabled(
        val message: String
    ) : ScratchCardResult()

    data class Error(
        val message: String
    ) : ScratchCardResult()
}

/**
 * Authoritative Scratch & Reveal Game Engine.
 * 
 * Flow:
 * 1. User requests scratch card -> Backend/Engine creates scratch session & selects authoritative reward tier.
 * 2. User scratches card locally on canvas -> tracks reveal percentage.
 * 3. User crosses threshold (e.g. 70%) or taps alternative reveal -> calls completeScratchSession.
 * 4. Engine validates session, enforces idempotency, prevents duplicate reward, logs SCRATCH_REWARD to wallet ledger.
 */
class ScratchGameEngine(
    private val rewardEngine: RewardEngine,
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository
) {
    // Active in-memory session cache mapped by scratchId
    private val activeSessions = ConcurrentHashMap<String, ScratchSession>()

    /**
     * Returns the authoritative remaining scratches for the user today.
     */
    suspend fun getDailyScratchStats(userId: String): DailyScratchStats {
        val todayStr = gameRepository.getTodayString()
        val stats = gameRepository.getTodayGameStats(userId, ScratchGameConfig.GAME_ID)
        val limit = ScratchGameConfig.dailyScratchLimit
        val used = stats.playsCount
        val remaining = (limit - used).coerceAtLeast(0)

        return DailyScratchStats(
            dailyLimit = limit,
            scratchesUsedToday = used,
            scratchesRemainingToday = remaining,
            resetDate = todayStr
        )
    }

    /**
     * 1. Create a secure scratch card session.
     * Selects winning tier authoritatively server-side via weighted probability.
     */
    suspend fun createScratchSession(
        userId: String,
        customScratchId: String? = null
    ): ScratchCardResult {
        // 1. Verify User
        val user = userRepository.getCurrentUser()
            ?: return ScratchCardResult.Error("Authentication required to play.")

        if (user.userId != userId) {
            return ScratchCardResult.Error("User session mismatch.")
        }

        if (user.accountStatus != AccountStatus.ACTIVE) {
            return ScratchCardResult.Error("Account is not active (${user.accountStatus}).")
        }

        // 2. Check Game Enabled
        if (!ScratchGameConfig.scratchGameEnabled) {
            return ScratchCardResult.GameDisabled("Scratch & Reveal is currently unavailable.")
        }

        // 3. Verify Daily Limit
        val todayStats = getDailyScratchStats(userId)
        if (todayStats.scratchesRemainingToday <= 0) {
            return ScratchCardResult.LimitReached(
                scratchesUsedToday = todayStats.scratchesUsedToday,
                limit = todayStats.dailyLimit,
                message = "Daily limit reached. Come back tomorrow for more free scratch cards!"
            )
        }

        // 4. Server-Authoritative Reward Selection
        val activeTiers = ScratchGameConfig.getActiveTiers()
        if (activeTiers.isEmpty()) {
            return ScratchCardResult.Error("No active reward tiers configured.")
        }

        val totalWeight = activeTiers.sumOf { it.weight }
        val randomPoint = Random.nextInt(totalWeight)

        var accumulatedWeight = 0
        var winningTier = activeTiers.first()

        for (tier in activeTiers) {
            accumulatedWeight += tier.weight
            if (randomPoint < accumulatedWeight) {
                winningTier = tier
                break
            }
        }

        // 5. Generate Session & Deterministic Idempotency Key
        val scratchId = customScratchId ?: UUID.randomUUID().toString()
        val idempotencyKey = IdempotencyManager.generateToken(
            userId = userId,
            source = "SCRATCH_REVEAL",
            sessionId = scratchId
        )

        val session = ScratchSession(
            scratchId = scratchId,
            userId = userId,
            rewardId = winningTier.rewardId,
            rewardAmount = winningTier.rewardAmount,
            status = ScratchSessionStatus.CREATED,
            createdAt = System.currentTimeMillis(),
            idempotencyKey = idempotencyKey
        )

        activeSessions[scratchId] = session

        return ScratchCardResult.CardCreated(
            session = session,
            tier = winningTier,
            scratchesUsedToday = todayStats.scratchesUsedToday,
            scratchesRemainingToday = todayStats.scratchesRemainingToday
        )
    }

    /**
     * 2. Complete and claim the scratch card reward.
     * Validates session, enforces scratch percentage threshold,
     * awards coins via central Reward Engine with unique idempotencyKey.
     */
    suspend fun completeScratchSession(
        userId: String,
        scratchId: String,
        revealedPercent: Float
    ): ScratchCardResult {
        val user = userRepository.getCurrentUser()
            ?: return ScratchCardResult.Error("Authentication required.")

        if (user.userId != userId) {
            return ScratchCardResult.Error("User session mismatch.")
        }

        val session = activeSessions[scratchId]
            ?: return ScratchCardResult.Error("Invalid or expired scratch session.")

        if (session.userId != userId) {
            return ScratchCardResult.Error("Session does not belong to the current user.")
        }

        // Check already rewarded
        if (session.status == ScratchSessionStatus.REWARDED) {
            return ScratchCardResult.AlreadyCompleted(
                message = "This scratch card has already been revealed and rewarded.",
                coinsAwarded = session.rewardAmount
            )
        }

        // Verify threshold
        if (revealedPercent < ScratchGameConfig.revealThresholdPercent) {
            return ScratchCardResult.Error("Card not fully scratched yet (${(revealedPercent * 100).toInt()}%).")
        }

        // Verify daily limit at claim time as well
        val todayStats = getDailyScratchStats(userId)
        if (todayStats.scratchesRemainingToday <= 0 && session.status == ScratchSessionStatus.CREATED) {
            return ScratchCardResult.LimitReached(
                scratchesUsedToday = todayStats.scratchesUsedToday,
                limit = todayStats.dailyLimit,
                message = "Daily scratch limit reached."
            )
        }

        // Process reward through central Reward Engine
        val rewardResult = rewardEngine.processReward(
            userId = userId,
            rewardType = TransactionType.SCRATCH_REWARD,
            source = "game_scratch_card",
            amount = session.rewardAmount,
            referenceId = session.scratchId,
            idempotencyKey = session.idempotencyKey,
            metadata = "Scratch & Reveal: ${session.rewardAmount} Coins (Tier ${session.rewardId})"
        )

        return when (rewardResult) {
            is RewardGrantResult.Success -> {
                val completedSession = session.copy(
                    status = ScratchSessionStatus.REWARDED,
                    revealedAt = System.currentTimeMillis(),
                    completedAt = System.currentTimeMillis()
                )
                activeSessions[scratchId] = completedSession

                // Record gameplay stats in database
                gameRepository.recordGamePlay(userId, ScratchGameConfig.GAME_ID, session.rewardAmount)
                val updatedStats = getDailyScratchStats(userId)

                ScratchCardResult.RewardGranted(
                    session = completedSession,
                    coinsAwarded = session.rewardAmount,
                    newBalance = rewardResult.newBalance,
                    scratchesUsedToday = updatedStats.scratchesUsedToday,
                    scratchesRemainingToday = updatedStats.scratchesRemainingToday,
                    transactionId = rewardResult.transactionId
                )
            }
            is RewardGrantResult.AlreadyClaimed -> {
                val completedSession = session.copy(
                    status = ScratchSessionStatus.REWARDED,
                    completedAt = System.currentTimeMillis()
                )
                activeSessions[scratchId] = completedSession

                val currentBalance = walletRepository.getCalculatedBalance(userId)
                val currentStats = getDailyScratchStats(userId)

                ScratchCardResult.RewardGranted(
                    session = completedSession,
                    coinsAwarded = rewardResult.existingCoins,
                    newBalance = currentBalance,
                    scratchesUsedToday = currentStats.scratchesUsedToday,
                    scratchesRemainingToday = currentStats.scratchesRemainingToday,
                    transactionId = "DUP_$scratchId"
                )
            }
            is RewardGrantResult.Rejected -> {
                ScratchCardResult.Error(rewardResult.reason)
            }
        }
    }

    fun getSession(scratchId: String): ScratchSession? = activeSessions[scratchId]
}
