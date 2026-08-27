package com.example.core.security

import com.example.data.local.SecurityEventDao
import com.example.data.model.FraudRiskState
import com.example.data.model.RiskEvaluationResult
import com.example.data.model.SecurityEventType
import com.example.data.model.SecuritySeverity

/**
 * Lightweight, configurable Fraud Risk Engine.
 * Evaluates behavioral signals and assigns a risk state without requiring invasive tracking.
 * 
 * Risk States:
 * - NORMAL: Standard active user behavior.
 * - REVIEW: Anomalies detected, flagged for backend/admin review (no automatic balance confiscation).
 * - BLOCKED: Critical security violations (e.g. repeated active tampering, extreme rate limit bypass).
 */
class FraudRiskEngine(
    private val securityEventDao: SecurityEventDao? = null,
    private val securityEventLogger: SecurityEventLogger? = null
) {

    /**
     * Evaluates the risk posture of a user based on recent security events and behavioral metrics.
     */
    suspend fun evaluateUserRisk(userId: String): RiskEvaluationResult {
        if (userId.isBlank()) {
            return RiskEvaluationResult(
                riskScore = 100,
                riskState = FraudRiskState.BLOCKED,
                triggeredFlags = listOf("Anonymous/Empty User ID"),
                recommendation = "Reject all operations for unauthenticated requests."
            )
        }

        if (securityEventDao == null) {
            return RiskEvaluationResult(
                riskScore = 0,
                riskState = FraudRiskState.NORMAL,
                triggeredFlags = emptyList(),
                recommendation = "Standard user operations permitted."
            )
        }

        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600_000L
        val tenMinutesAgo = now - 600_000L

        val recentEvents = securityEventDao.getRecentEventsForUser(userId, oneHourAgo)
        val flags = mutableListOf<String>()
        var score = 0

        // 1. Check duplicate replay attempts
        val duplicateAttempts = recentEvents.count { it.eventType == SecurityEventType.DUPLICATE_REWARD_ATTEMPT }
        if (duplicateAttempts >= 5) {
            score += 30
            flags.add("High duplicate reward attempts ($duplicateAttempts in last hour)")
        } else if (duplicateAttempts in 2..4) {
            score += 15
            flags.add("Moderate duplicate reward attempts ($duplicateAttempts in last hour)")
        }

        // 2. Check rate limit triggers
        val rateLimitEvents = recentEvents.count { it.eventType == SecurityEventType.RATE_LIMIT_TRIGGERED }
        if (rateLimitEvents >= 4) {
            score += 25
            flags.add("Repeated rate limit violations ($rateLimitEvents events)")
        } else if (rateLimitEvents in 2..3) {
            score += 10
            flags.add("Occasional rate limit triggers ($rateLimitEvents events)")
        }

        // 3. Check invalid game results (impossible scores/states)
        val invalidGameResults = recentEvents.count { it.eventType == SecurityEventType.INVALID_GAME_RESULT }
        if (invalidGameResults >= 3) {
            score += 40
            flags.add("Multiple impossible game submissions ($invalidGameResults)")
        } else if (invalidGameResults in 1..2) {
            score += 20
            flags.add("Suspicious gameplay submission detected ($invalidGameResults)")
        }

        // 4. Check suspicious ad completions
        val suspiciousAdEvents = recentEvents.count { it.eventType == SecurityEventType.SUSPICIOUS_AD_REWARD }
        if (suspiciousAdEvents >= 3) {
            score += 30
            flags.add("Abnormal ad reward velocity or verification failures ($suspiciousAdEvents)")
        }

        // 5. Check redemption abuse
        val redemptionAbuseEvents = recentEvents.count { it.eventType == SecurityEventType.REDEMPTION_ABUSE }
        if (redemptionAbuseEvents >= 2) {
            score += 35
            flags.add("Multiple redemption anomalies or balance violation attempts")
        }

        // 6. Check high-severity events in last 10 minutes
        val criticalRecent = recentEvents.count { 
            it.timestamp >= tenMinutesAgo && (it.severity == SecuritySeverity.HIGH || it.severity == SecuritySeverity.CRITICAL) 
        }
        if (criticalRecent >= 2) {
            score += 20
            flags.add("High-velocity critical security anomalies in last 10 minutes")
        }

        val clampedScore = score.coerceIn(0, 100)
        val riskState = when {
            clampedScore >= 75 -> FraudRiskState.BLOCKED
            clampedScore >= 35 -> FraudRiskState.REVIEW
            else -> FraudRiskState.NORMAL
        }

        val recommendation = when (riskState) {
            FraudRiskState.BLOCKED -> "Temporarily restrict high-value operations. Flagged for administrative verification."
            FraudRiskState.REVIEW -> "Permit standard gameplay with enhanced audit logging. Hold pending redemptions for review."
            FraudRiskState.NORMAL -> "Standard operations permitted without restrictions."
        }

        return RiskEvaluationResult(
            riskScore = clampedScore,
            riskState = riskState,
            triggeredFlags = flags,
            recommendation = recommendation
        )
    }

    /**
     * Records an anomaly and updates user audit state.
     */
    suspend fun recordAnomaly(
        userId: String,
        eventType: SecurityEventType,
        severity: SecuritySeverity,
        relatedId: String? = null,
        metadata: String? = null
    ) {
        if (securityEventLogger != null) {
            securityEventLogger.logEventSync(
                userId = userId,
                eventType = eventType,
                severity = severity,
                relatedId = relatedId,
                metadata = metadata
            )
        }
    }
}
