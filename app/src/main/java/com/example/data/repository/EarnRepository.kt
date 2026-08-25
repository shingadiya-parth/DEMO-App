package com.example.data.repository

import com.example.core.config.DailyBonusConfig
import com.example.core.security.IdempotencyManager
import com.example.data.local.DailyStreakDao
import com.example.data.model.AccountStatus
import com.example.data.model.DailyStreak
import com.example.data.model.EarnActivity
import com.example.data.model.EarnActivityType
import com.example.data.model.TransactionType
import com.example.domain.engine.RewardEngine
import com.example.domain.engine.RewardGrantResult
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EarnRepository(
    private val walletRepository: WalletRepository,
    private val userRepository: UserRepository,
    private val rewardEngine: RewardEngine,
    private val dailyStreakDao: DailyStreakDao
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun getTodayDateString(): String = dateFormat.format(Date())

    fun getYesterdayDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(calendar.time)
    }

    fun observeDailyStreak(userId: String): Flow<DailyStreak?> {
        return dailyStreakDao.observeStreak(userId)
    }

    suspend fun getDailyStreak(userId: String): DailyStreak {
        return dailyStreakDao.getStreak(userId) ?: DailyStreak(userId = userId)
    }

    /**
     * Checks whether the user has already claimed their daily bonus for today's calendar day.
     * Validates both the persistent streak state and the wallet ledger.
     */
    suspend fun isDailyBonusClaimedToday(userId: String): Boolean {
        val todayStr = getTodayDateString()
        val streak = dailyStreakDao.getStreak(userId)
        if (streak != null && streak.lastClaimDate == todayStr) {
            return true
        }

        // Ledger check for duplicate daily bonus transaction on today's date
        val recentTransactions = walletRepository.getRecentTransactions(userId, 50)
        return recentTransactions.any { tx ->
            tx.type == TransactionType.DAILY_BONUS &&
                    ((tx.idempotencyKey?.contains(todayStr) == true) || (tx.metadata?.contains(todayStr) == true))
        }
    }

    /**
     * Authoritative Daily Bonus claim operation.
     * Evaluates daily eligibility, calculates streak, applies idempotency protection,
     * routes through Centralized Reward Engine, and updates database records atomically.
     */
    suspend fun claimDailyBonus(userId: String): RewardGrantResult {
        val user = userRepository.getCurrentUser()
            ?: return RewardGrantResult.Rejected("Authentication required")

        if (user.accountStatus != AccountStatus.ACTIVE) {
            return RewardGrantResult.Rejected("Account is not active")
        }

        val todayStr = getTodayDateString()
        val yesterdayStr = getYesterdayDateString()

        // 1. Check if already claimed today
        if (isDailyBonusClaimedToday(userId)) {
            val balance = walletRepository.getCalculatedBalance(userId)
            return RewardGrantResult.AlreadyClaimed(
                existingCoins = DailyBonusConfig.BONUS_AMOUNT_COINS,
                currentBalance = balance,
                message = "Today's bonus already claimed. Come back tomorrow."
            )
        }

        // 2. Determine streak day and reward amount from config
        val currentRecord = dailyStreakDao.getStreak(userId) ?: DailyStreak(userId = userId)
        val newStreak = when (currentRecord.lastClaimDate) {
            yesterdayStr -> currentRecord.currentStreak + 1
            todayStr -> currentRecord.currentStreak
            else -> 1 // Streak reset if a day was skipped
        }

        val rewardAmount = DailyBonusConfig.BONUS_AMOUNT_COINS
        val idempotencyKey = IdempotencyManager.generateDailyToken(userId, "DAILY_BONUS", todayStr)

        // 3. Process authoritative grant through Central Reward Engine
        val result = rewardEngine.processReward(
            userId = userId,
            rewardType = TransactionType.DAILY_BONUS,
            source = "daily_bonus",
            amount = rewardAmount,
            referenceId = todayStr,
            idempotencyKey = idempotencyKey,
            metadata = "Daily Bonus ($todayStr) | Streak: Day $newStreak"
        )

        // 4. On successful credit, update persistent streak record
        if (result is RewardGrantResult.Success) {
            val updatedStreak = DailyStreak(
                userId = userId,
                currentStreak = newStreak,
                longestStreak = maxOf(newStreak, currentRecord.longestStreak),
                lastClaimDate = todayStr,
                lastClaimTimestamp = System.currentTimeMillis(),
                totalClaimsCount = currentRecord.totalClaimsCount + 1
            )
            dailyStreakDao.insertOrUpdateStreak(updatedStreak)
        }

        return result
    }

    /**
     * Backward-compatible helper for streak claim
     */
    suspend fun claimDailyStreak(userId: String, currentStreakDay: Int): TransactionResult {
        val grant = claimDailyBonus(userId)
        return when (grant) {
            is RewardGrantResult.Success -> {
                val tx = walletRepository.getRecentTransactions(userId, 20).firstOrNull { it.transactionId == grant.transactionId }
                if (tx != null) {
                    TransactionResult.Success(tx, grant.newBalance)
                } else {
                    TransactionResult.Error("Reward recorded with balance: ${grant.newBalance}")
                }
            }
            is RewardGrantResult.AlreadyClaimed -> {
                TransactionResult.Duplicate(
                    existingTransaction = com.example.data.model.CoinTransaction(
                        transactionId = "DUP",
                        userId = userId,
                        type = TransactionType.DAILY_BONUS,
                        amount = grant.existingCoins,
                        source = "daily_bonus",
                        idempotencyKey = "DUP",
                        createdAt = System.currentTimeMillis()
                    ),
                    currentBalance = grant.currentBalance
                )
            }
            is RewardGrantResult.Rejected -> {
                TransactionResult.Error(grant.reason)
            }
        }
    }

    /**
     * In-app earning activities definition (100% internal, no partner offers, no external links).
     */
    fun getInAppEarnActivities(): List<EarnActivity> {
        return listOf(
            EarnActivity(
                id = "act_daily_bonus",
                title = DailyBonusConfig.BONUS_TITLE,
                description = DailyBonusConfig.BONUS_SUBTITLE,
                type = EarnActivityType.DAILY_CHECKIN,
                rewardCoins = DailyBonusConfig.BONUS_AMOUNT_COINS,
                actionLabel = "Claim Bonus"
            ),
            EarnActivity(
                id = "act_referral_bonus",
                title = "Refer & Earn",
                description = "Share your unique in-app invite code with friends to receive bonus coins.",
                type = EarnActivityType.REFERRAL,
                rewardCoins = 350L,
                actionLabel = "Invite"
            ),
            EarnActivity(
                id = "act_daily_giveaway",
                title = "Daily In-App Giveaway",
                description = "Participate in today's community coin pot giveaway.",
                type = EarnActivityType.GIVEAWAY,
                rewardCoins = 700L,
                actionLabel = "Enter"
            )
        )
    }

    /**
     * Claims referral bonus.
     */
    suspend fun claimReferralBonus(userId: String, friendReferralCode: String): TransactionResult {
        val idempotencyKey = "REF:$userId:${friendReferralCode.trim().uppercase()}"
        return walletRepository.recordTransaction(
            userId = userId,
            type = TransactionType.REFERRAL_REWARD,
            source = "referral_invite",
            amount = 350L,
            idempotencyKey = idempotencyKey,
            metadata = "Referral code applied: $friendReferralCode"
        )
    }
}
