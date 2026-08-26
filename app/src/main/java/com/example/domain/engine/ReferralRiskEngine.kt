package com.example.domain.engine

import com.example.core.config.ReferralConfig
import com.example.data.local.ReferralDao
import com.example.data.model.AccountStatus
import com.example.data.model.ReferralRiskState
import com.example.data.model.UserAccount

sealed class ReferralRiskEvaluation {
    data object Approved : ReferralRiskEvaluation()
    data class FlaggedForReview(val reason: String) : ReferralRiskEvaluation()
    data class Blocked(val reason: String) : ReferralRiskEvaluation()
}

/**
 * Anti-Abuse and Fraud Detection Engine for Refer & Earn System.
 * Evaluates risk signals without relying solely on client-side state.
 */
class ReferralRiskEngine(
    private val referralDao: ReferralDao
) {

    /**
     * Evaluates whether a new referral relationship is legitimate.
     */
    suspend fun evaluateNewReferral(
        referrer: UserAccount,
        referredUser: UserAccount
    ): ReferralRiskEvaluation {
        // 1. Self-referral protection: strict identity check
        if (referrer.userId == referredUser.userId) {
            return ReferralRiskEvaluation.Blocked("Self-referrals are strictly prohibited.")
        }

        // 2. Cross-referral / email match check
        if (referrer.email.equals(referredUser.email, ignoreCase = true)) {
            return ReferralRiskEvaluation.Blocked("Cannot refer an account sharing the same email.")
        }

        // 3. User account status check
        if (referrer.accountStatus != AccountStatus.ACTIVE) {
            return ReferralRiskEvaluation.Blocked("Referrer account is not in active standing.")
        }

        if (referredUser.accountStatus != AccountStatus.ACTIVE) {
            return ReferralRiskEvaluation.Blocked("Referred account is not in active standing.")
        }

        // 4. Rate limit check: Daily limit
        val startOfDay = getStartOfDayTimestamp()
        val referralsToday = referralDao.countReferralsForReferrerToday(referrer.userId, startOfDay)
        if (referralsToday >= ReferralConfig.maximumReferralsPerDay) {
            return ReferralRiskEvaluation.Blocked("Referrer has reached the daily limit of ${ReferralConfig.maximumReferralsPerDay} referrals.")
        }

        // 5. Rate limit check: Lifetime limit
        val lifetimeReferrals = referralDao.countLifetimeReferralsForReferrer(referrer.userId)
        if (lifetimeReferrals >= ReferralConfig.maximumLifetimeReferrals) {
            return ReferralRiskEvaluation.Blocked("Referrer has reached the maximum lifetime referral limit of ${ReferralConfig.maximumLifetimeReferrals}.")
        }

        // 6. Velocity / pattern monitoring: if rapid creation within same session, mark for review
        if (referralsToday >= (ReferralConfig.maximumReferralsPerDay / 2)) {
            return ReferralRiskEvaluation.FlaggedForReview("High referral velocity detected today.")
        }

        return ReferralRiskEvaluation.Approved
    }

    private fun getStartOfDayTimestamp(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
