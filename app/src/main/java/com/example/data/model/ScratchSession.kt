package com.example.data.model

enum class ScratchSessionStatus {
    CREATED,
    SCRATCHING,
    REVEALED,
    REWARDED,
    EXPIRED,
    CANCELLED
}

/**
 * Authoritative Scratch & Reveal game session.
 */
data class ScratchSession(
    val scratchId: String,
    val userId: String,
    val rewardId: String,
    val rewardAmount: Long,
    val status: ScratchSessionStatus,
    val createdAt: Long = System.currentTimeMillis(),
    val revealedAt: Long? = null,
    val completedAt: Long? = null,
    val idempotencyKey: String
)
