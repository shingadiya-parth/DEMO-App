package com.example.domain.engine

import com.example.core.config.TicTacToeConfig
import com.example.core.config.TicTacToeMark
import com.example.core.config.TicTacToeOutcome
import com.example.core.security.IdempotencyManager
import com.example.data.model.AccountStatus
import com.example.data.model.TransactionType
import com.example.data.repository.GameRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class DailyTicTacToeStats(
    val dailyLimit: Int,
    val matchesUsedToday: Int,
    val matchesRemainingToday: Int,
    val resetDate: String
)

data class TicTacToeMatchState(
    val sessionId: String,
    val userId: String,
    val board: List<TicTacToeMark>, // Size 9
    val outcome: TicTacToeOutcome,
    val winningLine: List<Int>? = null,
    val aiMoveIndex: Int? = null,
    val coinsAwarded: Long = 0L,
    val newBalance: Long? = null,
    val matchesUsedToday: Int = 0,
    val matchesRemainingToday: Int = 0,
    val isCompleted: Boolean = false
)

sealed class TicTacToeResult {
    data class MatchCreated(
        val sessionId: String,
        val board: List<TicTacToeMark>,
        val matchesRemainingToday: Int
    ) : TicTacToeResult()

    data class MoveResult(
        val state: TicTacToeMatchState
    ) : TicTacToeResult()

    data class LimitReached(
        val matchesUsedToday: Int,
        val limit: Int,
        val message: String
    ) : TicTacToeResult()

    data class GameDisabled(
        val message: String
    ) : TicTacToeResult()

    data class Error(
        val message: String
    ) : TicTacToeResult()
}

/**
 * Authoritative Tic-Tac-Toe Game Engine.
 * Manages player vs smart AI match progression, win detection,
 * daily limits, and atomic reward crediting.
 */
class TicTacToeGameEngine(
    private val rewardEngine: RewardEngine,
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository
) {
    private val activeMatches = ConcurrentHashMap<String, TicTacToeMatchState>()

    private val winningCombinations = listOf(
        listOf(0, 1, 2),
        listOf(3, 4, 5),
        listOf(6, 7, 8),
        listOf(0, 3, 6),
        listOf(1, 4, 7),
        listOf(2, 5, 8),
        listOf(0, 4, 8),
        listOf(2, 4, 6)
    )

    suspend fun getDailyStats(userId: String): DailyTicTacToeStats {
        val todayStr = gameRepository.getTodayString()
        val stats = gameRepository.getTodayGameStats(userId, TicTacToeConfig.GAME_ID)
        val limit = TicTacToeConfig.dailyMatchLimit
        val used = stats.playsCount
        val remaining = (limit - used).coerceAtLeast(0)

        return DailyTicTacToeStats(
            dailyLimit = limit,
            matchesUsedToday = used,
            matchesRemainingToday = remaining,
            resetDate = todayStr
        )
    }

    /**
     * Initializes a new authorized Tic-Tac-Toe match session.
     */
    suspend fun startMatch(userId: String): TicTacToeResult {
        val user = userRepository.getCurrentUser()
            ?: return TicTacToeResult.Error("Authentication required to play.")

        if (user.userId != userId) {
            return TicTacToeResult.Error("User session mismatch.")
        }

        if (user.accountStatus != AccountStatus.ACTIVE) {
            return TicTacToeResult.Error("Account is not active (${user.accountStatus}).")
        }

        if (!TicTacToeConfig.enabled) {
            return TicTacToeResult.GameDisabled("Tic-Tac-Toe is currently unavailable.")
        }

        val dailyStats = getDailyStats(userId)
        if (dailyStats.matchesRemainingToday <= 0) {
            return TicTacToeResult.LimitReached(
                matchesUsedToday = dailyStats.matchesUsedToday,
                limit = dailyStats.dailyLimit,
                message = "Daily limit reached (${dailyStats.dailyLimit} matches/day). Come back tomorrow!"
            )
        }

        val sessionId = UUID.randomUUID().toString()
        val initialBoard = List(9) { TicTacToeMark.EMPTY }
        val initialState = TicTacToeMatchState(
            sessionId = sessionId,
            userId = userId,
            board = initialBoard,
            outcome = TicTacToeOutcome.IN_PROGRESS,
            matchesUsedToday = dailyStats.matchesUsedToday,
            matchesRemainingToday = dailyStats.matchesRemainingToday,
            isCompleted = false
        )
        activeMatches[sessionId] = initialState

        return TicTacToeResult.MatchCreated(
            sessionId = sessionId,
            board = initialBoard,
            matchesRemainingToday = dailyStats.matchesRemainingToday
        )
    }

    /**
     * Executes a player move (X), evaluates game state, executes AI response (O),
     * and processes rewards on match completion.
     */
    suspend fun playMove(userId: String, sessionId: String, cellIndex: Int): TicTacToeResult {
        val match = activeMatches[sessionId]
            ?: return TicTacToeResult.Error("Match session expired or not found.")

        if (match.userId != userId) {
            return TicTacToeResult.Error("Unauthorized match session.")
        }

        if (match.isCompleted) {
            return TicTacToeResult.MoveResult(match)
        }

        if (cellIndex !in 0..8 || match.board[cellIndex] != TicTacToeMark.EMPTY) {
            return TicTacToeResult.Error("Invalid move position.")
        }

        // 1. Apply Player move (X)
        val boardAfterPlayer = match.board.toMutableList().apply {
            set(cellIndex, TicTacToeMark.X)
        }

        // Check if player won
        val playerWinLine = checkWinningLine(boardAfterPlayer, TicTacToeMark.X)
        if (playerWinLine != null) {
            return completeMatch(
                userId = userId,
                sessionId = sessionId,
                board = boardAfterPlayer,
                outcome = TicTacToeOutcome.WIN,
                winningLine = playerWinLine,
                aiMoveIndex = null
            )
        }

        // Check if board full -> Draw
        if (boardAfterPlayer.none { it == TicTacToeMark.EMPTY }) {
            return completeMatch(
                userId = userId,
                sessionId = sessionId,
                board = boardAfterPlayer,
                outcome = TicTacToeOutcome.DRAW,
                winningLine = null,
                aiMoveIndex = null
            )
        }

        // 2. Compute AI move (O)
        val aiMove = findBestAiMove(boardAfterPlayer)
        val boardAfterAi = boardAfterPlayer.toMutableList().apply {
            set(aiMove, TicTacToeMark.O)
        }

        // Check if AI won
        val aiWinLine = checkWinningLine(boardAfterAi, TicTacToeMark.O)
        if (aiWinLine != null) {
            return completeMatch(
                userId = userId,
                sessionId = sessionId,
                board = boardAfterAi,
                outcome = TicTacToeOutcome.LOSS,
                winningLine = aiWinLine,
                aiMoveIndex = aiMove
            )
        }

        // Check if board full after AI move -> Draw
        if (boardAfterAi.none { it == TicTacToeMark.EMPTY }) {
            return completeMatch(
                userId = userId,
                sessionId = sessionId,
                board = boardAfterAi,
                outcome = TicTacToeOutcome.DRAW,
                winningLine = null,
                aiMoveIndex = aiMove
            )
        }

        // 3. Match continues
        val updatedState = match.copy(
            board = boardAfterAi,
            outcome = TicTacToeOutcome.IN_PROGRESS,
            aiMoveIndex = aiMove
        )
        activeMatches[sessionId] = updatedState
        return TicTacToeResult.MoveResult(updatedState)
    }

    /**
     * Smart AI decision engine:
     * 1. Detect winning moves for O.
     * 2. Block winning moves for X.
     * 3. Prefer center cell (4).
     * 4. Prefer corners (0, 2, 6, 8).
     * 5. Pick remaining valid moves.
     */
    private fun findBestAiMove(board: List<TicTacToeMark>): Int {
        // 1. Can AI (O) win right now?
        for (combo in winningCombinations) {
            val oCount = combo.count { board[it] == TicTacToeMark.O }
            val emptyCount = combo.count { board[it] == TicTacToeMark.EMPTY }
            if (oCount == 2 && emptyCount == 1) {
                return combo.first { board[it] == TicTacToeMark.EMPTY }
            }
        }

        // 2. Can Player (X) win on next turn? Block it!
        for (combo in winningCombinations) {
            val xCount = combo.count { board[it] == TicTacToeMark.X }
            val emptyCount = combo.count { board[it] == TicTacToeMark.EMPTY }
            if (xCount == 2 && emptyCount == 1) {
                return combo.first { board[it] == TicTacToeMark.EMPTY }
            }
        }

        // 3. Prefer Center cell (4)
        if (board[4] == TicTacToeMark.EMPTY) {
            return 4
        }

        // 4. Prefer Corners
        val corners = listOf(0, 2, 6, 8).filter { board[it] == TicTacToeMark.EMPTY }
        if (corners.isNotEmpty()) {
            return corners[Random.nextInt(corners.size)]
        }

        // 5. Remaining valid cells
        val remaining = board.indices.filter { board[it] == TicTacToeMark.EMPTY }
        return remaining[Random.nextInt(remaining.size)]
    }

    private fun checkWinningLine(board: List<TicTacToeMark>, mark: TicTacToeMark): List<Int>? {
        return winningCombinations.firstOrNull { combo ->
            combo.all { board[it] == mark }
        }
    }

    private suspend fun completeMatch(
        userId: String,
        sessionId: String,
        board: List<TicTacToeMark>,
        outcome: TicTacToeOutcome,
        winningLine: List<Int>?,
        aiMoveIndex: Int?
    ): TicTacToeResult {
        val coinsToAward = when (outcome) {
            TicTacToeOutcome.WIN -> TicTacToeConfig.winReward
            TicTacToeOutcome.DRAW -> TicTacToeConfig.drawReward
            TicTacToeOutcome.LOSS -> TicTacToeConfig.lossReward
            else -> 0L
        }

        val idempotencyKey = IdempotencyManager.generateToken(
            userId = userId,
            source = "TIC_TAC_TOE",
            sessionId = sessionId
        )

        var newBalance = walletRepository.getCalculatedBalance(userId)

        if (coinsToAward > 0L) {
            val rewardResult = rewardEngine.processReward(
                userId = userId,
                rewardType = TransactionType.TIC_TAC_TOE_REWARD,
                source = "game_tictactoe",
                amount = coinsToAward,
                referenceId = sessionId,
                idempotencyKey = idempotencyKey,
                metadata = "Tic-Tac-Toe: Outcome = ${outcome.label}"
            )

            when (rewardResult) {
                is RewardGrantResult.Success -> {
                    newBalance = rewardResult.newBalance
                }
                is RewardGrantResult.AlreadyClaimed -> {
                    newBalance = rewardResult.currentBalance
                }
                is RewardGrantResult.Rejected -> {
                    return TicTacToeResult.Error(rewardResult.reason)
                }
            }
        }

        gameRepository.recordGamePlay(userId, TicTacToeConfig.GAME_ID, coinsToAward)
        val updatedStats = getDailyStats(userId)

        val completedState = TicTacToeMatchState(
            sessionId = sessionId,
            userId = userId,
            board = board,
            outcome = outcome,
            winningLine = winningLine,
            aiMoveIndex = aiMoveIndex,
            coinsAwarded = coinsToAward,
            newBalance = newBalance,
            matchesUsedToday = updatedStats.matchesUsedToday,
            matchesRemainingToday = updatedStats.matchesRemainingToday,
            isCompleted = true
        )

        activeMatches[sessionId] = completedState
        return TicTacToeResult.MoveResult(completedState)
    }
}
