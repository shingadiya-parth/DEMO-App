package com.example.data.model

/**
 * Game Difficulty levels.
 */
enum class GameDifficulty(val label: String, val multiplier: Double) {
    EASY("Easy", 1.0),
    MEDIUM("Medium", 1.5),
    HARD("Hard", 2.0)
}

/**
 * Game Categories.
 */
enum class GameCategory(val title: String) {
    CHANCE("Luck & Chance"),
    PUZZLE("Mind & Puzzle"),
    ARCADE("Casual Arcade"),
    CLASSIC("Classic Board")
}

/**
 * Configurable Game Definition model.
 */
data class GameDefinition(
    val gameId: String,
    val gameName: String,
    val description: String,
    val category: GameCategory,
    val difficulty: GameDifficulty,
    val baseRewardCoins: Long,
    val maxDailyPlays: Int,
    val maxDailyRewardCoins: Long,
    val cooldownMinutes: Int,
    val isRewardedAdAvailable: Boolean = true,
    val isEnabled: Boolean = true,
    val iconKey: String
)
