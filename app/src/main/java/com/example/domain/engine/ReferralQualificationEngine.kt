package com.example.domain.engine

import com.example.core.config.ReferralConfig
import com.example.data.local.ReferralDao
import com.example.data.local.UserDao
import com.example.data.model.AccountStatus
import com.example.data.model.ReferralRecord
import com.example.data.model.ReferralRiskState
import com.example.data.model.ReferralStatus
import com.example.services.notifications.NotificationService
import java.util.concurrent.TimeUnit

sealed class QualificationEvaluationResult {
    data object NoReferralFound : QualificationEvaluationResult()
    data class ProgressUpdated(val currentProgress: Int, val target: Int) : QualificationEvaluationResult()
    data class QualifiedAndRewarded(
        val referralRecord: ReferralRecord,
        val referrerCoins: Long,
        val referredUserCoins: Long
    ) : QualificationEvaluationResult()
    data class Expired(val referralId: String) : QualificationEvaluationResult()
    data class Rejected(val reason: String) : QualificationEvaluationResult()
}

/**
 * Authoritative Referral Qualification Engine.
 * 
 * Guarantees that rewards are NOT granted upon mere signup.
 * Evaluates real user activity against centralized rules and orchestrates reward settlement.
 */
class ReferralQualificationEngine(
    private val referralDao: ReferralDao,
    private val userDao: UserDao,
    private val notificationService: NotificationService,
    private val rewardEngineProvider: () -> RewardEngine
) {

    private val rewardEngine: RewardEngine get() = rewardEngineProvider()

    /**
     * Called whenever a user completes an eligible activity (e.g. game session played or coins earned).
     */
    suspend fun evaluateUserProgress(
        referredUserId: String,
        additionalProgress: Int = 1
    ): QualificationEvaluationResult {
        // 1. Fetch active pending/qualifying referral
        val record = referralDao.getPendingOrQualifyingReferral(referredUserId)
            ?: return QualificationEvaluationResult.NoReferralFound

        // 2. Check if expired
        val maxAgeMillis = TimeUnit.DAYS.toMillis(ReferralConfig.referralExpiryDays.toLong())
        if (System.currentTimeMillis() - record.createdAt > maxAgeMillis) {
            val expiredRecord = record.copy(
                status = ReferralStatus.EXPIRED,
                notes = "Referral window expired after ${ReferralConfig.referralExpiryDays} days."
            )
            referralDao.updateReferral(expiredRecord)
            return QualificationEvaluationResult.Expired(record.referralId)
        }

        // 3. Check anti-fraud / risk status
        if (record.riskState == ReferralRiskState.BLOCKED) {
            val rejectedRecord = record.copy(
                status = ReferralStatus.REJECTED,
                notes = "Referral rejected due to risk policy enforcement."
            )
            referralDao.updateReferral(rejectedRecord)
            return QualificationEvaluationResult.Rejected("Risk policy violation.")
        }

        // 4. Verify referred user account status
        val user = userDao.getUser(referredUserId)
        if (user == null || user.accountStatus != AccountStatus.ACTIVE) {
            return QualificationEvaluationResult.Rejected("Referred user account is not active.")
        }

        // 5. Update progress
        val newProgress = record.qualificationProgress + additionalProgress
        val target = record.qualificationTarget

        if (newProgress >= target) {
            // Target satisfied! Transition to QUALIFIED
            val qualifiedRecord = record.copy(
                qualificationProgress = newProgress,
                status = ReferralStatus.QUALIFIED,
                qualifiedAt = System.currentTimeMillis()
            )
            referralDao.updateReferral(qualifiedRecord)

            notificationService.emitReferralQualified(
                userId = record.referrerUserId,
                rewardCoins = qualifiedRecord.referrerRewardAmount
            )

            // Trigger authoritative reward payout via RewardEngine
            val rewardResult = rewardEngine.processReferralReward(qualifiedRecord)

            val finalRecord = when (rewardResult) {
                is ReferralRewardGrantResult.Success -> {
                    notificationService.emitReferralRewardCredited(
                        userId = record.referrerUserId,
                        coins = rewardResult.referrerCoinsGranted,
                        isInviter = true
                    )
                    if (rewardResult.referredUserCoinsGranted > 0L) {
                        notificationService.emitReferralRewardCredited(
                            userId = record.referredUserId,
                            coins = rewardResult.referredUserCoinsGranted,
                            isInviter = false
                        )
                    }

                    qualifiedRecord.copy(
                        status = ReferralStatus.REWARDED,
                        referrerRewardTransactionId = rewardResult.referrerTransactionId,
                        referredUserRewardTransactionId = rewardResult.referredUserTransactionId
                    )
                }
                is ReferralRewardGrantResult.Skipped -> {
                    qualifiedRecord.copy(notes = "Reward skipped: ${rewardResult.reason}")
                }
                is ReferralRewardGrantResult.Failed -> {
                    qualifiedRecord.copy(notes = "Reward failed: ${rewardResult.error}")
                }
            }

            referralDao.updateReferral(finalRecord)
            return QualificationEvaluationResult.QualifiedAndRewarded(
                referralRecord = finalRecord,
                referrerCoins = record.referrerRewardAmount,
                referredUserCoins = record.referredUserRewardAmount
            )
        } else {
            // Still qualifying
            val qualifyingRecord = record.copy(
                qualificationProgress = newProgress,
                status = ReferralStatus.QUALIFYING
            )
            referralDao.updateReferral(qualifyingRecord)
            notificationService.emitReferralQualifying(
                userId = record.referrerUserId,
                progress = newProgress,
                target = target
            )
            return QualificationEvaluationResult.ProgressUpdated(newProgress, target)
        }
    }
}
