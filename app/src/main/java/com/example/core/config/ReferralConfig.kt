package com.example.core.config

import com.example.data.model.ReferralQualificationType

/**
 * Centralized Referral Program Configuration.
 * 
 * All business rules, reward amounts, qualification thresholds, limits, and domain structures
 * are managed here and never hard-coded inside UI components.
 */
object ReferralConfig {
    // Master switches
    var referralEnabled: Boolean = true
    var rewardEnabled: Boolean = true

    // Reward amounts (NestCoins)
    var referrerReward: Long = 500L
    var referredUserReward: Long = 100L

    // Qualification Rules
    var qualificationRequirement: ReferralQualificationType = ReferralQualificationType.GAMES_COMPLETED
    var requiredGameSessionsCount: Int = 3
    var minimumCoinsEarnedForQualification: Long = 50L

    // Anti-Abuse & Rate Limits
    var maximumReferralsPerDay: Int = 10
    var maximumLifetimeReferrals: Int = 100
    var cooldownSecondsBetweenReferrals: Long = 10L
    var referralExpiryDays: Int = 30

    // Domain & Deep Link Architecture
    // Domain placeholder ready for production domain configuration
    var baseReferralDomain: String = "https://playrewards.app"
    var deepLinkScheme: String = "playrewards"
    var deepLinkHost: String = "invite"

    /**
     * Generates standard web / universal invite link.
     */
    fun generateInviteLink(referralCode: String): String {
        return "$baseReferralDomain/invite/$referralCode"
    }

    /**
     * Generates custom scheme deep link.
     */
    fun generateDeepLink(referralCode: String): String {
        return "$deepLinkScheme://$deepLinkHost/$referralCode"
    }

    /**
     * Generates standard user-friendly share message for Android share sheet.
     */
    fun generateShareMessage(referralCode: String): String {
        val link = generateInviteLink(referralCode)
        return "Join me on PlayRewards and earn NestCoins playing games! Use my referral code: $referralCode or visit: $link"
    }
}
