package com.example.domain.engine

import com.example.core.config.ReferralConfig
import com.example.core.security.IdempotencyManager
import com.example.data.model.AccountStatus
import com.example.data.model.ReferralRecord
import com.example.data.model.TransactionType
import com.example.data.repository.GameRepository
import com.example.data.repository.TransactionResult
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import com.example.services.ads.AdActionConfig
import com.example.services.ads.AdAnalytics
import com.example.services.ads.AdAnalyticsEvent
import com.example.services.ads.AdMobConfig

/**
 * Result of a Reward Engine processing operation.
 */
sealed class RewardGrantResult {
    data class Success(
        val coinsGranted: Long,
        val newBalance: Long,
        val transactionId: String,
        val message: String
    ) : RewardGrantResult()

    data class AlreadyClaimed(
        val existingCoins: Long,
        val currentBalance: Long,
        val message: String
    ) : RewardGrantResult()

    data class Rejected(
        val reason: String
    ) : RewardGrantResult()
}

/**
 * Result of a Referral Reward grant operation.
 */
sealed class ReferralRewardGrantResult {
    data class Success(
        val referrerTransactionId: String,
        val referredUserTransactionId: String?,
        val referrerCoinsGranted: Long,
        val referredUserCoinsGranted: Long
    ) : ReferralRewardGrantResult()

    data class Skipped(val reason: String) : ReferralRewardGrantResult()
    data class Failed(val error: String) : ReferralRewardGrantResult()
}

/**
 * Centralized Reward Engine.
 * 
 * All rewards MUST pass through this engine.
 * No individual game, ad service, or UI activity directly modifies the wallet balance.
 */
class RewardEngine(
    private val walletRepository: WalletRepository,
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository
) {

    var referralQualificationEngine: ReferralQualificationEngine? = null

    /**
     * Universal Reward Processor accepting all standard reward requests.
     */
    suspend fun processReward(
        userId: String,
        rewardType: TransactionType,
        source: String,
        amount: Long,
        referenceId: String? = null,
        idempotencyKey: String,
        metadata: String? = null
    ): RewardGrantResult {
        // 1. Verify authenticated user
        if (userId.isBlank()) {
            return RewardGrantResult.Rejected("Authentication required")
        }

        val user = userRepository.getCurrentUser()
        if (user == null || user.userId != userId) {
            return RewardGrantResult.Rejected("User account session mismatch")
        }

        if (user.accountStatus != AccountStatus.ACTIVE) {
            return RewardGrantResult.Rejected("Account status is ${user.accountStatus}")
        }

        // 2. Validate amount
        if (amount <= 0L) {
            return RewardGrantResult.Rejected("Reward amount must be greater than 0")
        }

        // 3. Atomically credit wallet via central ledger
        val txResult = walletRepository.addCoins(
            userId = userId,
            type = rewardType,
            source = source,
            amount = amount,
            referenceId = referenceId,
            idempotencyKey = idempotencyKey,
            metadata = metadata
        )

        return when (txResult) {
            is TransactionResult.Success -> {
                RewardGrantResult.Success(
                    coinsGranted = amount,
                    newBalance = txResult.newBalance,
                    transactionId = txResult.transaction.transactionId,
                    message = "+$amount coins awarded!"
                )
            }
            is TransactionResult.Duplicate -> {
                RewardGrantResult.AlreadyClaimed(
                    existingCoins = txResult.existingTransaction.amount,
                    currentBalance = txResult.currentBalance,
                    message = "Reward for this activity was already claimed."
                )
            }
            is TransactionResult.Error -> {
                RewardGrantResult.Rejected(txResult.message)
            }
        }
    }

    /**
     * Validates and processes a game completion reward.
     */
    suspend fun processGameReward(
        userId: String,
        gameId: String,
        calculatedScore: Int,
        rawCoinsProposed: Long,
        multiplier: Double = 1.0,
        sessionId: String
    ): RewardGrantResult {
        // 1. Validate Game Configuration
        val config = GameRewardConfigManager.getConfig(gameId)
        if (config != null && !config.enabled) {
            return RewardGrantResult.Rejected("This game is currently disabled.")
        }

        // 2. Validate daily limit & cooldown
        val stats = gameRepository.getTodayGameStats(userId, gameId)
        if (config != null) {
            if (stats.playsCount >= config.dailyLimit) {
                return RewardGrantResult.Rejected("Daily limit reached (${config.dailyLimit} plays/day). Come back tomorrow!")
            }
            val cooldownMillis = config.cooldownSeconds * 1000L
            val elapsed = System.currentTimeMillis() - stats.lastPlayedTimestamp
            if (stats.lastPlayedTimestamp > 0 && elapsed < cooldownMillis) {
                val remainingSec = (cooldownMillis - elapsed) / 1000L
                return RewardGrantResult.Rejected("Cooldown active. Please wait ${remainingSec}s before playing again.")
            }
        }

        val game = gameRepository.getGameById(gameId)
        val gameMultiplier = game?.difficulty?.multiplier ?: 1.0
        val baseReward = config?.rewardAmount ?: rawCoinsProposed
        val finalCoins = (baseReward * gameMultiplier * multiplier)
            .toLong()
            .coerceAtLeast(1L)

        // Enforce max daily reward if configured
        if (config != null && (stats.coinsEarnedToday + finalCoins) > config.maxDailyReward) {
            val allowedRemaining = (config.maxDailyReward - stats.coinsEarnedToday).coerceAtLeast(0L)
            if (allowedRemaining == 0L) {
                return RewardGrantResult.Rejected("Max daily reward limit of ${config.maxDailyReward} coins reached for this game.")
            }
        }

        val idempotencyKey = IdempotencyManager.generateToken(
            userId = userId,
            source = "GAME_$gameId",
            sessionId = sessionId
        )

        val result = processReward(
            userId = userId,
            rewardType = when (gameId) {
                "spin_win" -> TransactionType.SPIN_REWARD
                "scratch_card", "scratch_reveal" -> TransactionType.SCRATCH_REWARD
                "tile_puzzle", "puzzles", "puzzle", "word_guess" -> TransactionType.PUZZLE_REWARD
                "coin_toss" -> TransactionType.COIN_TOSS_REWARD
                "tictactoe" -> TransactionType.TIC_TAC_TOE_REWARD
                "bubble_pop" -> TransactionType.BUBBLE_POP_REWARD
                else -> TransactionType.GAME_REWARD
            },
            source = "game_$gameId",
            amount = finalCoins,
            referenceId = sessionId,
            idempotencyKey = idempotencyKey,
            metadata = "Game: ${game?.gameName ?: gameId} | Score: $calculatedScore | Multiplier: ${multiplier}x"
        )

        if (result is RewardGrantResult.Success) {
            gameRepository.recordGamePlay(userId, gameId, finalCoins)
            referralQualificationEngine?.evaluateUserProgress(userId, 1)
        }

        return result
    }

    /**
     * Authoritative Rewarded Ad Reward processing.
     * Enforces unique rewardRequestId and cryptographic idempotencyKey.
     */
    suspend fun processRewardedAdAction(
        userId: String,
        actionConfig: AdActionConfig,
        rewardRequestId: String,
        verificationToken: String,
        adPlacement: String
    ): RewardGrantResult {
        // Build cryptographic idempotency key
        val idempotencyKey = IdempotencyManager.generateToken(
            userId = userId,
            source = "REWARDED_AD_${actionConfig.actionKey}",
            sessionId = rewardRequestId
        )

        val result = processReward(
            userId = userId,
            rewardType = actionConfig.transactionType,
            source = "admob_${actionConfig.actionKey}",
            amount = actionConfig.rewardAmount,
            referenceId = rewardRequestId,
            idempotencyKey = idempotencyKey,
            metadata = "Ad Placement: $adPlacement | Action: ${actionConfig.actionKey} | Token: $verificationToken"
        )

        when (result) {
            is RewardGrantResult.Success -> {
                AdAnalytics.logEvent(
                    AdAnalyticsEvent.REWARDED_REWARD_GRANTED,
                    mapOf(
                        "userId" to userId,
                        "action" to actionConfig.actionKey,
                        "amount" to actionConfig.rewardAmount,
                        "transactionId" to result.transactionId
                    )
                )
            }
            is RewardGrantResult.AlreadyClaimed -> {
                AdAnalytics.logEvent(
                    AdAnalyticsEvent.REWARDED_REWARD_REJECTED,
                    mapOf(
                        "userId" to userId,
                        "action" to actionConfig.actionKey,
                        "reason" to "duplicate_request"
                    )
                )
            }
            is RewardGrantResult.Rejected -> {
                AdAnalytics.logEvent(
                    AdAnalyticsEvent.REWARDED_REWARD_REJECTED,
                    mapOf(
                        "userId" to userId,
                        "action" to actionConfig.actionKey,
                        "reason" to result.reason
                    )
                )
            }
        }

        return result
    }

    /**
     * Interface and architecture preparation for rewarded ad completions with custom unit ID.
     */
    suspend fun processRewardedAdReward(
        userId: String,
        adUnitId: String,
        adPlacement: String,
        rewardAmount: Long = 25L,
        adSessionToken: String
    ): RewardGrantResult {
        val idempotencyKey = IdempotencyManager.generateToken(
            userId = userId,
            source = "REWARDED_AD_$adPlacement",
            sessionId = adSessionToken
        )

        return processReward(
            userId = userId,
            rewardType = TransactionType.AD_REWARD,
            source = "admob_rewarded_$adPlacement",
            amount = rewardAmount,
            referenceId = adUnitId,
            idempotencyKey = idempotencyKey,
            metadata = "Ad Placement: $adPlacement | Unit: $adUnitId"
        )
    }

    /**
     * Processes activity rewards (Daily Bonus, Referral, Giveaway, etc.).
     */
    suspend fun processActivityReward(
        userId: String,
        activityType: TransactionType,
        sourceTag: String,
        coins: Long,
        actionToken: String,
        metadata: String? = null
    ): RewardGrantResult {
        val idempotencyKey = IdempotencyManager.generateToken(
            userId = userId,
            source = activityType.name,
            sessionId = actionToken
        )

        return processReward(
            userId = userId,
            rewardType = activityType,
            source = sourceTag,
            amount = coins,
            referenceId = actionToken,
            idempotencyKey = idempotencyKey,
            metadata = metadata
        )
    }

    /**
     * Authoritative referral reward settlement.
     * Credits the referrer and (if configured) the referred user via atomic immutable ledger transactions.
     */
    suspend fun processReferralReward(
        referralRecord: ReferralRecord
    ): ReferralRewardGrantResult {
        if (!ReferralConfig.rewardEnabled) {
            return ReferralRewardGrantResult.Skipped("Referral rewards are currently disabled in configuration.")
        }

        val referrerId = referralRecord.referrerUserId
        val referredId = referralRecord.referredUserId
        val referrerReward = referralRecord.referrerRewardAmount
        val referredUserReward = referralRecord.referredUserRewardAmount

        // 1. Process Referrer Reward
        val referrerIdempotencyKey = "ref_${referralRecord.referralId}_referrer"
        val referrerTxResult = walletRepository.addCoins(
            userId = referrerId,
            type = TransactionType.REFERRAL_REWARD,
            source = "referral_program",
            amount = referrerReward,
            referenceId = referralRecord.referralId,
            idempotencyKey = referrerIdempotencyKey,
            metadata = "Referral Reward for inviting friend (Ref ID: ${referralRecord.referralId})"
        )

        val referrerTxId = when (referrerTxResult) {
            is TransactionResult.Success -> referrerTxResult.transaction.transactionId
            is TransactionResult.Duplicate -> referrerTxResult.existingTransaction.transactionId
            is TransactionResult.Error -> return ReferralRewardGrantResult.Failed("Failed to credit referrer: ${referrerTxResult.message}")
        }

        // 2. Process Referred User Welcome Reward if configured
        var referredUserTxId: String? = null
        if (referredUserReward > 0L) {
            val referredIdempotencyKey = "ref_${referralRecord.referralId}_referred"
            val referredTxResult = walletRepository.addCoins(
                userId = referredId,
                type = TransactionType.REFERRAL_REWARD,
                source = "referral_welcome_bonus",
                amount = referredUserReward,
                referenceId = referralRecord.referralId,
                idempotencyKey = referredIdempotencyKey,
                metadata = "Welcome Bonus for joining via referral code ${referralRecord.referralCode}"
            )
            referredUserTxId = when (referredTxResult) {
                is TransactionResult.Success -> referredTxResult.transaction.transactionId
                is TransactionResult.Duplicate -> referredTxResult.existingTransaction.transactionId
                is TransactionResult.Error -> null
            }
        }

        return ReferralRewardGrantResult.Success(
            referrerTransactionId = referrerTxId,
            referredUserTransactionId = referredUserTxId,
            referrerCoinsGranted = referrerReward,
            referredUserCoinsGranted = referredUserReward
        )
    }
}
