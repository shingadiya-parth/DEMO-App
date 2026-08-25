package com.example.domain.engine

import com.example.data.model.GameDefinition
import java.util.UUID

/**
 * Game Session state representing an active or completed round in any of the 8 supported games.
 */
data class GameSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val gameId: String,
    val userId: String,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val rawScore: Int = 0,
    val movesCount: Int = 0,
    val durationSeconds: Long = 0L,
    val isCompleted: Boolean = false,
    val isVerified: Boolean = false
)

/**
 * Core Game Engine.
 * Provides session lifecycle, score to coin calculation, and gameplay validation.
 */
class GameEngine(
    private val rewardEngine: RewardEngine
) {

    /**
     * Initializes a new secure game session token for a specific game.
     */
    fun startSession(userId: String, gameId: String): GameSession {
        return GameSession(
            sessionId = UUID.randomUUID().toString(),
            gameId = gameId,
            userId = userId,
            startedAt = System.currentTimeMillis()
        )
    }

    /**
     * Calculates base coin reward based on game rules and score achieved.
     */
    fun calculateBaseCoins(game: GameDefinition, score: Int, isVictory: Boolean): Long {
        if (!isVictory && score <= 0) return 0L
        val scoreMultiplier = when {
            score >= 1000 -> 1.5
            score >= 500 -> 1.2
            else -> 1.0
        }
        return (game.baseRewardCoins * scoreMultiplier).toLong().coerceAtLeast(5L)
    }

    /**
     * Completes game round and delegates reward processing to Centralized Reward Engine.
     */
    suspend fun completeGameRound(
        session: GameSession,
        game: GameDefinition,
        score: Int,
        isVictory: Boolean,
        adMultiplier: Double = 1.0
    ): RewardGrantResult {
        val baseCoins = calculateBaseCoins(game, score, isVictory)
        return rewardEngine.processGameReward(
            userId = session.userId,
            gameId = session.gameId,
            calculatedScore = score,
            rawCoinsProposed = baseCoins,
            multiplier = adMultiplier,
            sessionId = session.sessionId
        )
    }
}
