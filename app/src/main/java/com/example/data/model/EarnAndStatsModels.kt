package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Type of in-app earning activities.
 */
enum class EarnActivityType(val title: String) {
    DAILY_CHECKIN("Daily Streak Check-in"),
    REFERRAL("Refer Friends"),
    GIVEAWAY("Daily Coin Giveaway"),
    MILESTONE("Gameplay Milestone")
}

/**
 * In-app earning activity item definition.
 */
data class EarnActivity(
    val id: String,
    val title: String,
    val description: String,
    val type: EarnActivityType,
    val rewardCoins: Long,
    val cooldownHours: Int = 24,
    val isCompleted: Boolean = false,
    val progress: Float = 0f,
    val actionLabel: String = "Claim"
)

/**
 * Tracks daily game play stats for enforcing play limits, cooldowns, and anti-fraud rules.
 */
@Entity(tableName = "gameplay_stats")
data class GamePlayStats(
    @PrimaryKey
    @ColumnInfo(name = "stat_id")
    val statId: String, // e.g. "${userId}_${gameId}_${dateString}"

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "game_id")
    val gameId: String,

    @ColumnInfo(name = "date_string")
    val dateString: String, // YYYY-MM-DD

    @ColumnInfo(name = "plays_count")
    val playsCount: Int = 0,

    @ColumnInfo(name = "coins_earned_today")
    val coinsEarnedToday: Long = 0L,

    @ColumnInfo(name = "last_played_timestamp")
    val lastPlayedTimestamp: Long = 0L
)
