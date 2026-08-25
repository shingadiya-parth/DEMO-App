package com.example.data.repository

import com.example.data.local.GamePlayStatsDao
import com.example.data.model.GameCategory
import com.example.data.model.GameDefinition
import com.example.data.model.GameDifficulty
import com.example.data.model.GamePlayStats
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GameRepository(private val statsDao: GamePlayStatsDao) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun getTodayString(): String = dateFormat.format(Date())

    /**
     * Centralized Registry of all In-App Games.
     * Fully configurable with limits, difficulties, and reward values.
     */
    private val registeredGames = listOf(
        GameDefinition(
            gameId = "spin_win",
            gameName = "Spin & Win",
            description = "Spin the lucky wheel to win instant bonus coins every few hours.",
            category = GameCategory.CHANCE,
            difficulty = GameDifficulty.EASY,
            baseRewardCoins = 70L,
            maxDailyPlays = 15,
            maxDailyRewardCoins = 2100L,
            cooldownMinutes = 15,
            isRewardedAdAvailable = true,
            isEnabled = true,
            iconKey = "ic_spin"
        ),
        GameDefinition(
            gameId = "scratch_reveal",
            gameName = "Scratch & Reveal",
            description = "Scratch mystery cards to reveal hidden coin multipliers.",
            category = GameCategory.CHANCE,
            difficulty = GameDifficulty.EASY,
            baseRewardCoins = 50L,
            maxDailyPlays = 20,
            maxDailyRewardCoins = 1800L,
            cooldownMinutes = 10,
            isRewardedAdAvailable = true,
            isEnabled = true,
            iconKey = "ic_scratch"
        ),
        GameDefinition(
            gameId = "puzzles",
            gameName = "Number Slider Puzzle",
            description = "Slide tiles into chronological order against the clock.",
            category = GameCategory.PUZZLE,
            difficulty = GameDifficulty.MEDIUM,
            baseRewardCoins = 140L,
            maxDailyPlays = 12,
            maxDailyRewardCoins = 2800L,
            cooldownMinutes = 5,
            isRewardedAdAvailable = true,
            isEnabled = true,
            iconKey = "ic_puzzle"
        ),
        GameDefinition(
            gameId = "coin_toss",
            gameName = "Lucky Coin Toss",
            description = "Predict Heads or Tails and build winning streak multipliers.",
            category = GameCategory.CHANCE,
            difficulty = GameDifficulty.EASY,
            baseRewardCoins = 35L,
            maxDailyPlays = 25,
            maxDailyRewardCoins = 1500L,
            cooldownMinutes = 3,
            isRewardedAdAvailable = true,
            isEnabled = true,
            iconKey = "ic_coin_toss"
        ),
        GameDefinition(
            gameId = "tictactoe",
            gameName = "Tic-Tac-Toe AI",
            description = "Challenge our smart AI engine in a classic grid battle.",
            category = GameCategory.CLASSIC,
            difficulty = GameDifficulty.MEDIUM,
            baseRewardCoins = 100L,
            maxDailyPlays = 15,
            maxDailyRewardCoins = 2200L,
            cooldownMinutes = 5,
            isRewardedAdAvailable = true,
            isEnabled = true,
            iconKey = "ic_tictactoe"
        ),
        GameDefinition(
            gameId = "word_puzzle",
            gameName = "Word Scramble",
            description = "Unscramble anagrams and test your vocabulary skills.",
            category = GameCategory.PUZZLE,
            difficulty = GameDifficulty.MEDIUM,
            baseRewardCoins = 120L,
            maxDailyPlays = 15,
            maxDailyRewardCoins = 2500L,
            cooldownMinutes = 5,
            isRewardedAdAvailable = true,
            isEnabled = true,
            iconKey = "ic_word"
        ),
        GameDefinition(
            gameId = "bubble_pop",
            gameName = "Bubble Popper",
            description = "Pop falling colorful bubbles before time runs out.",
            category = GameCategory.ARCADE,
            difficulty = GameDifficulty.EASY,
            baseRewardCoins = 90L,
            maxDailyPlays = 20,
            maxDailyRewardCoins = 2600L,
            cooldownMinutes = 5,
            isRewardedAdAvailable = true,
            isEnabled = true,
            iconKey = "ic_bubble"
        ),
        GameDefinition(
            gameId = "daily_challenge",
            gameName = "Daily Mega Challenge",
            description = "Special daily mini-mission with maximum coin rewards.",
            category = GameCategory.PUZZLE,
            difficulty = GameDifficulty.HARD,
            baseRewardCoins = 350L,
            maxDailyPlays = 1,
            maxDailyRewardCoins = 700L,
            cooldownMinutes = 720,
            isRewardedAdAvailable = true,
            isEnabled = true,
            iconKey = "ic_daily_challenge"
        )
    )

    fun getAllGames(): List<GameDefinition> = registeredGames

    fun getGameById(gameId: String): GameDefinition? = registeredGames.find { it.gameId == gameId }

    fun observeTodayStats(userId: String): Flow<List<GamePlayStats>> {
        return statsDao.observeDailyStats(userId, getTodayString())
    }

    suspend fun getTodayGameStats(userId: String, gameId: String): GamePlayStats {
        val today = getTodayString()
        val statId = "${userId}_${gameId}_$today"
        return statsDao.getStats(statId) ?: GamePlayStats(
            statId = statId,
            userId = userId,
            gameId = gameId,
            dateString = today,
            playsCount = 0,
            coinsEarnedToday = 0L,
            lastPlayedTimestamp = 0L
        )
    }

    suspend fun recordGamePlay(userId: String, gameId: String, coinsEarned: Long) {
        val today = getTodayString()
        val currentStats = getTodayGameStats(userId, gameId)
        val updated = currentStats.copy(
            playsCount = currentStats.playsCount + 1,
            coinsEarnedToday = currentStats.coinsEarnedToday + coinsEarned,
            lastPlayedTimestamp = System.currentTimeMillis()
        )
        statsDao.upsertStats(updated)
    }
}
