package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RedemptionRequest
import kotlinx.coroutines.flow.Flow

@Dao
interface RedemptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: RedemptionRequest)

    @Update
    suspend fun updateRequest(request: RedemptionRequest)

    @Query("SELECT * FROM redemption_requests WHERE user_id = :userId ORDER BY requested_at DESC")
    fun observeRedemptionRequests(userId: String): Flow<List<RedemptionRequest>>

    @Query("SELECT * FROM redemption_requests WHERE user_id = :userId AND reward_id = :rewardId AND requested_at >= :sinceTimestamp")
    suspend fun getRequestsForRewardSince(userId: String, rewardId: String, sinceTimestamp: Long): List<RedemptionRequest>

    @Query("DELETE FROM redemption_requests WHERE user_id = :userId")
    suspend fun deleteRedemptionsForUser(userId: String)
}
