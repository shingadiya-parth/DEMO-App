package com.example.data.model

import java.util.UUID

/**
 * Lifecycle states for unified game sessions.
 */
enum class GameSessionStatus {
    CREATED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    EXPIRED,
    REWARDED
}

/**
 * Common Game Session model for authoritative state, verification, and audit trail.
 */
data class CommonGameSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val userId: String,
    val gameId: String,
    val gameType: String = gameId,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val status: GameSessionStatus = GameSessionStatus.CREATED,
    val result: String? = null,
    val score: Int = 0,
    val rewardAmount: Long = 0L,
    val idempotencyKey: String,
    val metadata: String? = null
)
