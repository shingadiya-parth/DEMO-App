package com.example.core.config

/**
 * Tic-Tac-Toe Player Marks.
 */
enum class TicTacToeMark {
    EMPTY,
    X, // User
    O  // AI
}

/**
 * Tic-Tac-Toe Game Outcomes.
 */
enum class TicTacToeOutcome(val label: String) {
    WIN("You Won!"),
    LOSS("Computer Won"),
    DRAW("It's a Draw!"),
    IN_PROGRESS("In Progress")
}

/**
 * Centralized configuration for Tic-Tac-Toe AI match.
 */
object TicTacToeConfig {
    const val GAME_ID = "tictactoe"
    const val GAME_TITLE = "Tic-Tac-Toe AI"

    var enabled: Boolean = true
    var dailyMatchLimit: Int = 5
    var winReward: Long = 50L
    var drawReward: Long = 10L
    var lossReward: Long = 0L
    var cooldownSeconds: Long = 0L
    var adExtraAttemptEnabled: Boolean = false
    var maximumAdExtraMatchesPerDay: Int = 3
}
