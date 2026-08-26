package com.example.domain.engine

import com.example.core.config.ClientPuzzleQuestion
import com.example.core.config.PuzzleCategory
import com.example.core.config.PuzzleConfig
import com.example.core.config.PuzzleDefinition
import com.example.core.config.PuzzleDifficulty
import com.example.core.security.IdempotencyManager
import com.example.data.model.AccountStatus
import com.example.data.model.PuzzleSession
import com.example.data.model.PuzzleSessionStatus
import com.example.data.model.TransactionType
import com.example.data.repository.GameRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class DailyPuzzleStats(
    val dailyLimit: Int,
    val puzzlesCompletedToday: Int,
    val puzzlesRemainingToday: Int,
    val resetDate: String
)

sealed class PuzzleSessionResult {
    data class QuestionDelivered(
        val sessionId: String,
        val question: ClientPuzzleQuestion,
        val puzzlesCompletedToday: Int,
        val puzzlesRemainingToday: Int,
        val timeLimitSeconds: Int
    ) : PuzzleSessionResult()

    data class LimitReached(
        val puzzlesCompletedToday: Int,
        val limit: Int,
        val message: String
    ) : PuzzleSessionResult()

    data class GameDisabled(
        val message: String
    ) : PuzzleSessionResult()

    data class Error(
        val message: String
    ) : PuzzleSessionResult()
}

sealed class PuzzleSubmitResult {
    data class Correct(
        val coinsAwarded: Long,
        val newBalance: Long,
        val explanation: String,
        val correctAnswerIndex: Int,
        val puzzlesCompletedToday: Int,
        val puzzlesRemainingToday: Int,
        val transactionId: String
    ) : PuzzleSubmitResult()

    data class Incorrect(
        val explanation: String,
        val correctAnswerIndex: Int,
        val puzzlesCompletedToday: Int,
        val puzzlesRemainingToday: Int
    ) : PuzzleSubmitResult()

    data class Expired(
        val explanation: String,
        val correctAnswerIndex: Int,
        val message: String
    ) : PuzzleSubmitResult()

    data class AlreadySubmitted(
        val message: String,
        val coinsAwarded: Long
    ) : PuzzleSubmitResult()

    data class LimitReached(
        val message: String
    ) : PuzzleSubmitResult()

    data class Error(
        val message: String
    ) : PuzzleSubmitResult()
}

/**
 * Authoritative Puzzle Game Engine.
 * 
 * Responsibilities:
 * 1. Authoritative daily quota enforcement (5 puzzles/day).
 * 2. Secure puzzle question delivery with answer masking.
 * 3. Authoritative server-side answer and timer evaluation.
 * 4. Atomic transaction processing via central RewardEngine with SHA-256 idempotency protection.
 */
class PuzzleGameEngine(
    private val rewardEngine: RewardEngine,
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository
) {
    // In-memory active session store mapped by puzzleSessionId
    private val activeSessions = ConcurrentHashMap<String, PuzzleSession>()

    /**
     * Retrieves daily puzzle attempts and remaining limits from the authoritative ledger/repository.
     */
    suspend fun getDailyPuzzleStats(userId: String): DailyPuzzleStats {
        val todayStr = gameRepository.getTodayString()
        val stats = gameRepository.getTodayGameStats(userId, PuzzleConfig.GAME_ID)
        val limit = PuzzleConfig.dailyPuzzleLimit
        val completed = stats.playsCount
        val remaining = (limit - completed).coerceAtLeast(0)

        return DailyPuzzleStats(
            dailyLimit = limit,
            puzzlesCompletedToday = completed,
            puzzlesRemainingToday = remaining,
            resetDate = todayStr
        )
    }

    /**
     * Initiates a new authoritative puzzle session for the active user.
     * Selects a puzzle definition and strips answer metadata before returning to the UI.
     */
    suspend fun createPuzzleSession(
        userId: String,
        customSessionId: String? = null
    ): PuzzleSessionResult {
        // 1. User authentication verification
        val user = userRepository.getCurrentUser()
            ?: return PuzzleSessionResult.Error("Authentication required to play.")

        if (user.userId != userId) {
            return PuzzleSessionResult.Error("User session mismatch.")
        }

        if (user.accountStatus != AccountStatus.ACTIVE) {
            return PuzzleSessionResult.Error("Account is not active (${user.accountStatus}).")
        }

        // 2. Check game status
        if (!PuzzleConfig.puzzleGameEnabled) {
            return PuzzleSessionResult.GameDisabled("Brain Puzzle game is currently disabled.")
        }

        // 3. Authoritative daily quota check
        val dailyStats = getDailyPuzzleStats(userId)
        if (dailyStats.puzzlesRemainingToday <= 0) {
            return PuzzleSessionResult.LimitReached(
                puzzlesCompletedToday = dailyStats.puzzlesCompletedToday,
                limit = dailyStats.dailyLimit,
                message = "Today's puzzles are complete! Come back tomorrow for new challenges."
            )
        }

        // 4. Select Puzzle Definition
        val activeCatalog = PuzzleConfig.getActivePuzzles()
        if (activeCatalog.isEmpty()) {
            return PuzzleSessionResult.Error("No active puzzles available in catalog.")
        }

        // Deterministic or pseudo-random selection based on attempt count
        val puzzleIndex = (dailyStats.puzzlesCompletedToday + System.currentTimeMillis().toInt()).mod(activeCatalog.size)
        val selectedPuzzle = activeCatalog[puzzleIndex]

        // 5. Generate Session & Deterministic Idempotency Key
        val sessionId = customSessionId ?: UUID.randomUUID().toString()
        val idempotencyKey = IdempotencyManager.generateToken(
            userId = userId,
            source = "PUZZLE_REWARD",
            sessionId = sessionId
        )

        val session = PuzzleSession(
            puzzleSessionId = sessionId,
            userId = userId,
            puzzleId = selectedPuzzle.puzzleId,
            startedAt = System.currentTimeMillis(),
            status = PuzzleSessionStatus.STARTED,
            idempotencyKey = idempotencyKey
        )

        activeSessions[sessionId] = session

        // 6. Deliver masked client question
        val clientQuestion = ClientPuzzleQuestion(
            puzzleId = selectedPuzzle.puzzleId,
            question = selectedPuzzle.question,
            options = selectedPuzzle.options,
            difficulty = selectedPuzzle.difficulty,
            category = selectedPuzzle.category,
            rewardAmount = selectedPuzzle.rewardAmount,
            timeLimitSeconds = selectedPuzzle.timeLimitSeconds
        )

        return PuzzleSessionResult.QuestionDelivered(
            sessionId = sessionId,
            question = clientQuestion,
            puzzlesCompletedToday = dailyStats.puzzlesCompletedToday,
            puzzlesRemainingToday = dailyStats.puzzlesRemainingToday,
            timeLimitSeconds = selectedPuzzle.timeLimitSeconds
        )
    }

    /**
     * Evaluates the submitted answer authoritatively against the puzzle session.
     */
    suspend fun submitAnswer(
        userId: String,
        sessionId: String,
        selectedAnswerIndex: Int
    ): PuzzleSubmitResult {
        val user = userRepository.getCurrentUser()
            ?: return PuzzleSubmitResult.Error("Authentication required.")

        if (user.userId != userId) {
            return PuzzleSubmitResult.Error("User session mismatch.")
        }

        val session = activeSessions[sessionId]
            ?: return PuzzleSubmitResult.Error("Invalid or expired puzzle session.")

        if (session.userId != userId) {
            return PuzzleSubmitResult.Error("Session does not belong to the active user.")
        }

        // Check if already completed
        if (session.status == PuzzleSessionStatus.REWARDED ||
            session.status == PuzzleSessionStatus.CORRECT ||
            session.status == PuzzleSessionStatus.INCORRECT ||
            session.status == PuzzleSessionStatus.EXPIRED
        ) {
            return PuzzleSubmitResult.AlreadySubmitted(
                message = "This puzzle attempt has already been submitted.",
                coinsAwarded = if (session.status == PuzzleSessionStatus.REWARDED) 25L else 0L
            )
        }

        // Fetch puzzle definition from catalog
        val puzzleDef = PuzzleConfig.puzzleCatalog.find { it.puzzleId == session.puzzleId }
            ?: return PuzzleSubmitResult.Error("Puzzle definition not found.")

        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - session.startedAt) / 1000

        // Check authoritative timer expiration
        if (puzzleDef.timeLimitSeconds > 0) {
            val maxAllowedSeconds = puzzleDef.timeLimitSeconds + PuzzleConfig.timerToleranceBufferSeconds
            if (elapsedSeconds > maxAllowedSeconds) {
                val expiredSession = session.copy(
                    submittedAt = now,
                    status = PuzzleSessionStatus.EXPIRED,
                    selectedAnswerIndex = selectedAnswerIndex,
                    result = "EXPIRED"
                )
                activeSessions[sessionId] = expiredSession

                // Record attempt in database
                gameRepository.recordGamePlay(userId, PuzzleConfig.GAME_ID, 0L)

                return PuzzleSubmitResult.Expired(
                    explanation = puzzleDef.explanation,
                    correctAnswerIndex = puzzleDef.correctAnswerIndex,
                    message = "Time's up! Answer was not submitted within ${puzzleDef.timeLimitSeconds} seconds."
                )
            }
        }

        // Authoritative correctness evaluation
        val isCorrect = selectedAnswerIndex == puzzleDef.correctAnswerIndex

        if (isCorrect) {
            // Process reward through central engine
            val rewardResult = rewardEngine.processReward(
                userId = userId,
                rewardType = TransactionType.PUZZLE_REWARD,
                source = "game_puzzle",
                amount = puzzleDef.rewardAmount,
                referenceId = session.puzzleSessionId,
                idempotencyKey = session.idempotencyKey,
                metadata = "Puzzle: ${puzzleDef.category.displayName} (${puzzleDef.difficulty.displayName})"
            )

            return when (rewardResult) {
                is RewardGrantResult.Success -> {
                    val updatedSession = session.copy(
                        submittedAt = now,
                        status = PuzzleSessionStatus.REWARDED,
                        selectedAnswerIndex = selectedAnswerIndex,
                        result = "CORRECT",
                        rewardTransactionId = rewardResult.transactionId
                    )
                    activeSessions[sessionId] = updatedSession

                    gameRepository.recordGamePlay(userId, PuzzleConfig.GAME_ID, puzzleDef.rewardAmount)
                    val updatedStats = getDailyPuzzleStats(userId)

                    PuzzleSubmitResult.Correct(
                        coinsAwarded = puzzleDef.rewardAmount,
                        newBalance = rewardResult.newBalance,
                        explanation = puzzleDef.explanation,
                        correctAnswerIndex = puzzleDef.correctAnswerIndex,
                        puzzlesCompletedToday = updatedStats.puzzlesCompletedToday,
                        puzzlesRemainingToday = updatedStats.puzzlesRemainingToday,
                        transactionId = rewardResult.transactionId
                    )
                }
                is RewardGrantResult.AlreadyClaimed -> {
                    val updatedSession = session.copy(
                        submittedAt = now,
                        status = PuzzleSessionStatus.REWARDED,
                        selectedAnswerIndex = selectedAnswerIndex,
                        result = "CORRECT"
                    )
                    activeSessions[sessionId] = updatedSession

                    val currentBalance = walletRepository.getCalculatedBalance(userId)
                    val updatedStats = getDailyPuzzleStats(userId)

                    PuzzleSubmitResult.Correct(
                        coinsAwarded = rewardResult.existingCoins,
                        newBalance = currentBalance,
                        explanation = puzzleDef.explanation,
                        correctAnswerIndex = puzzleDef.correctAnswerIndex,
                        puzzlesCompletedToday = updatedStats.puzzlesCompletedToday,
                        puzzlesRemainingToday = updatedStats.puzzlesRemainingToday,
                        transactionId = "DUP_$sessionId"
                    )
                }
                is RewardGrantResult.Rejected -> {
                    PuzzleSubmitResult.Error(rewardResult.reason)
                }
            }
        } else {
            // Incorrect answer
            val incorrectSession = session.copy(
                submittedAt = now,
                status = PuzzleSessionStatus.INCORRECT,
                selectedAnswerIndex = selectedAnswerIndex,
                result = "INCORRECT"
            )
            activeSessions[sessionId] = incorrectSession

            gameRepository.recordGamePlay(userId, PuzzleConfig.GAME_ID, 0L)
            val updatedStats = getDailyPuzzleStats(userId)

            return PuzzleSubmitResult.Incorrect(
                explanation = puzzleDef.explanation,
                correctAnswerIndex = puzzleDef.correctAnswerIndex,
                puzzlesCompletedToday = updatedStats.puzzlesCompletedToday,
                puzzlesRemainingToday = updatedStats.puzzlesRemainingToday
            )
        }
    }

    fun getSession(sessionId: String): PuzzleSession? = activeSessions[sessionId]
}
