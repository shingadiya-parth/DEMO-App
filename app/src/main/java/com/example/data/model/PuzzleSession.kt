package com.example.data.model

enum class PuzzleSessionStatus {
    STARTED,
    SUBMITTED,
    CORRECT,
    INCORRECT,
    EXPIRED,
    REWARDED
}

/**
 * Authoritative Puzzle gameplay session model.
 */
data class PuzzleSession(
    val puzzleSessionId: String,
    val userId: String,
    val puzzleId: String,
    val startedAt: Long = System.currentTimeMillis(),
    val submittedAt: Long? = null,
    val status: PuzzleSessionStatus = PuzzleSessionStatus.STARTED,
    val selectedAnswerIndex: Int? = null,
    val result: String? = null,
    val rewardTransactionId: String? = null,
    val idempotencyKey: String
)
