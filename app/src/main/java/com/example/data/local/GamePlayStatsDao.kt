package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.GamePlayStats
import kotlinx.coroutines.flow.Flow

@Dao
interface GamePlayStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStats(stats: GamePlayStats)

    @Query("SELECT * FROM gameplay_stats WHERE stat_id = :statId LIMIT 1")
    suspend fun getStats(statId: String): GamePlayStats?

    @Query("SELECT * FROM gameplay_stats WHERE user_id = :userId AND date_string = :dateString")
    fun observeDailyStats(userId: String, dateString: String): Flow<List<GamePlayStats>>

    @Query("SELECT * FROM gameplay_stats WHERE user_id = :userId AND game_id = :gameId AND date_string = :dateString LIMIT 1")
    suspend fun getGameDailyStats(userId: String, gameId: String, dateString: String): GamePlayStats?

    @Query("DELETE FROM gameplay_stats WHERE user_id = :userId")
    suspend fun deleteStatsForUser(userId: String)
}
