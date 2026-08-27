package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SecurityEventType {
    DUPLICATE_REWARD_ATTEMPT,
    INVALID_GAME_RESULT,
    RATE_LIMIT_TRIGGERED,
    SUSPICIOUS_REFERRAL,
    SUSPICIOUS_AD_REWARD,
    REDEMPTION_ABUSE,
    AUTH_SECURITY_EVENT,
    UNAUTHORIZED_ACCESS_ATTEMPT,
    ACCOUNT_TAMPER_ATTEMPT
}

enum class SecuritySeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class FraudRiskState {
    NORMAL,
    REVIEW,
    BLOCKED
}

@Entity(
    tableName = "security_events",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["eventType"]),
        Index(value = ["severity"]),
        Index(value = ["timestamp"])
    ]
)
data class SecurityEvent(
    @PrimaryKey
    val eventId: String,
    val userId: String,
    val eventType: SecurityEventType,
    val severity: SecuritySeverity,
    val timestamp: Long,
    val relatedId: String? = null,
    val metadata: String? = null
)

data class RiskEvaluationResult(
    val riskScore: Int, // 0 to 100
    val riskState: FraudRiskState,
    val triggeredFlags: List<String>,
    val recommendation: String
)
