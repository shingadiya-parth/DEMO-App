package com.example.data.repository

import com.example.core.config.ReferralConfig
import com.example.data.local.ReferralDao
import com.example.data.local.UserDao
import com.example.data.model.ReferralRecord
import com.example.data.model.ReferralRiskState
import com.example.data.model.ReferralStatus
import com.example.data.model.ReferralSummary
import com.example.data.model.SafeReferralDisplayItem
import com.example.data.model.UserAccount
import com.example.domain.engine.ReferralRiskEngine
import com.example.domain.engine.ReferralRiskEvaluation
import com.example.services.notifications.NotificationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Authoritative Repository managing referral relationships, validation, and analytics.
 */
class ReferralRepository(
    private val referralDao: ReferralDao,
    private val userDao: UserDao,
    private val riskEngine: ReferralRiskEngine,
    private val notificationService: NotificationService
) {

    /**
     * Observes live comprehensive referral dashboard statistics for a given user.
     */
    fun observeReferralSummary(userId: String): Flow<ReferralSummary> {
        val userFlow = userDao.observeUser(userId)
        val referralsFlow = referralDao.observeReferralsForReferrer(userId)

        return combine(userFlow, referralsFlow) { user, referrals ->
            val code = user?.referralCode ?: "NEEDS_CODE"
            val totalFriends = referrals.size
            val qualified = referrals.count { it.status == ReferralStatus.QUALIFIED || it.status == ReferralStatus.REWARDED }
            val pending = referrals.count { it.status == ReferralStatus.PENDING || it.status == ReferralStatus.QUALIFYING }
            val rejected = referrals.count { it.status == ReferralStatus.REJECTED || it.status == ReferralStatus.EXPIRED }
            val totalEarned = referrals
                .filter { it.status == ReferralStatus.REWARDED }
                .sumOf { it.referrerRewardAmount }

            val safeItems = referrals.mapIndexed { index, record ->
                SafeReferralDisplayItem(
                    referralId = record.referralId,
                    friendLabel = "Friend #${totalFriends - index}",
                    status = record.status,
                    rewardCoins = record.referrerRewardAmount,
                    progress = record.qualificationProgress,
                    target = record.qualificationTarget,
                    createdAt = record.createdAt,
                    qualifiedAt = record.qualifiedAt
                )
            }

            ReferralSummary(
                referralCode = code,
                shareableLink = ReferralConfig.generateInviteLink(code),
                totalFriendsReferred = totalFriends,
                qualifiedReferrals = qualified,
                pendingReferrals = pending,
                rejectedReferrals = rejected,
                totalCoinsEarned = totalEarned,
                hasAppliedReferralCode = user?.referredBy != null,
                referredByCode = null,
                recentReferrals = safeItems
            )
        }
    }

    /**
     * Observes safe privacy-preserving friend referral list.
     */
    fun observeSafeReferrals(userId: String): Flow<List<SafeReferralDisplayItem>> {
        return referralDao.observeReferralsForReferrer(userId).map { referrals ->
            referrals.mapIndexed { index, record ->
                SafeReferralDisplayItem(
                    referralId = record.referralId,
                    friendLabel = "Friend #${referrals.size - index}",
                    status = record.status,
                    rewardCoins = record.referrerRewardAmount,
                    progress = record.qualificationProgress,
                    target = record.qualificationTarget,
                    createdAt = record.createdAt,
                    qualifiedAt = record.qualifiedAt
                )
            }
        }
    }

    /**
     * Validates and applies a referral code entered by the user.
     * Enforces single referrer, self-referral prevention, and anti-abuse policies.
     */
    suspend fun applyReferralCode(currentUserId: String, rawCode: String): Result<ReferralRecord> {
        val cleanCode = rawCode.trim().uppercase()
        if (cleanCode.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter a referral code."))
        }

        val currentUser = userDao.getUser(currentUserId)
            ?: return Result.failure(IllegalStateException("User profile not found."))

        // 1. Check if user already applied a referral code
        if (currentUser.referredBy != null) {
            return Result.failure(IllegalStateException("You have already applied a referral code. Only one referrer is allowed."))
        }

        val existingReferral = referralDao.getReferralByReferredUserId(currentUserId)
        if (existingReferral != null) {
            return Result.failure(IllegalStateException("A referral record already exists for this account."))
        }

        // 2. Lookup inviter
        val referrer = userDao.getUserByReferralCode(cleanCode)
            ?: return Result.failure(IllegalArgumentException("Invalid referral code. Please check and try again."))

        // 3. Self-referral protection
        if (referrer.userId == currentUser.userId) {
            return Result.failure(IllegalArgumentException("You cannot use your own referral code."))
        }

        // 4. Evaluate risk
        val riskEval = riskEngine.evaluateNewReferral(referrer, currentUser)
        val riskState = when (riskEval) {
            is ReferralRiskEvaluation.Approved -> ReferralRiskState.NORMAL
            is ReferralRiskEvaluation.FlaggedForReview -> ReferralRiskState.REVIEW
            is ReferralRiskEvaluation.Blocked -> return Result.failure(IllegalStateException(riskEval.reason))
        }

        // 5. Create authoritative referral record
        val record = ReferralRecord(
            referralId = "ref_${UUID.randomUUID().toString().take(12)}",
            referrerUserId = referrer.userId,
            referredUserId = currentUser.userId,
            referralCode = referrer.referralCode,
            createdAt = System.currentTimeMillis(),
            status = ReferralStatus.PENDING,
            qualificationProgress = 0,
            qualificationTarget = ReferralConfig.requiredGameSessionsCount,
            referrerRewardAmount = ReferralConfig.referrerReward,
            referredUserRewardAmount = ReferralConfig.referredUserReward,
            idempotencyKey = "ref_bind_${referrer.userId}_${currentUser.userId}",
            riskState = riskState,
            notes = if (riskState == ReferralRiskState.REVIEW) "Flagged for velocity review" else null
        )

        referralDao.insertReferral(record)

        // 6. Update user's referredBy binding
        val updatedUser = currentUser.copy(
            referredBy = referrer.userId,
            lastActivity = System.currentTimeMillis()
        )
        userDao.updateUser(updatedUser)

        // 7. Emit in-app notification
        notificationService.emitFriendJoined(referrer.referralCode)

        return Result.success(record)
    }

    /**
     * Called during account registration if a referral code was supplied at signup.
     */
    suspend fun registerReferralOnSignUp(referrerCode: String, newUserId: String): Result<ReferralRecord> {
        return applyReferralCode(newUserId, referrerCode)
    }

    /**
     * Checks if a referral code exists and returns the referrer's public display name.
     */
    suspend fun validateReferralCode(code: String): Result<String> {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) return Result.failure(IllegalArgumentException("Code is blank"))

        val referrer = userDao.getUserByReferralCode(cleanCode)
            ?: return Result.failure(IllegalArgumentException("Referral code not found"))

        return Result.success(referrer.displayName)
    }
}
