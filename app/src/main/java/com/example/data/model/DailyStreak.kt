package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Authoritative Daily Streak and Daily Bonus persistence entity.
 * Prepares data structure for current streak, longest streak, last claim date, and timestamp.
 */
@Entity(tableName = "daily_streak")
data class DailyStreak(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "current_streak")
    val currentStreak: Int = 0,

    @ColumnInfo(name = "longest_streak")
    val longestStreak: Int = 0,

    @ColumnInfo(name = "last_claim_date")
    val lastClaimDate: String? = null, // Formatted as "yyyy-MM-dd"

    @ColumnInfo(name = "last_claim_timestamp")
    val lastClaimTimestamp: Long = 0L,

    @ColumnInfo(name = "total_claims_count")
    val totalClaimsCount: Int = 0
)
