package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DailyStreak
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Daily Streak and Daily Bonus records.
 */
@Dao
interface DailyStreakDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStreak(streak: DailyStreak)

    @Query("SELECT * FROM daily_streak WHERE user_id = :userId LIMIT 1")
    suspend fun getStreak(userId: String): DailyStreak?

    @Query("SELECT * FROM daily_streak WHERE user_id = :userId LIMIT 1")
    fun observeStreak(userId: String): Flow<DailyStreak?>

    @Query("DELETE FROM daily_streak WHERE user_id = :userId")
    suspend fun deleteStreak(userId: String)
}
