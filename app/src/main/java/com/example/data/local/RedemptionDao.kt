package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RedemptionRequest
import com.example.data.model.RedemptionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RedemptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: RedemptionRequest)

    @Update
    suspend fun updateRequest(request: RedemptionRequest)

    @Query("SELECT * FROM redemption_requests WHERE redemption_id = :redemptionId LIMIT 1")
    suspend fun getRedemptionById(redemptionId: String): RedemptionRequest?

    @Query("SELECT * FROM redemption_requests WHERE idempotency_key = :idempotencyKey LIMIT 1")
    suspend fun getRedemptionByIdempotencyKey(idempotencyKey: String): RedemptionRequest?

    @Query("SELECT * FROM redemption_requests WHERE user_id = :userId ORDER BY created_at DESC")
    fun observeRedemptionRequests(userId: String): Flow<List<RedemptionRequest>>

    @Query("SELECT * FROM redemption_requests WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getRedemptionsForUser(userId: String): List<RedemptionRequest>

    @Query("SELECT * FROM redemption_requests WHERE user_id = :userId AND created_at >= :sinceTimestamp")
    suspend fun getRequestsSince(userId: String, sinceTimestamp: Long): List<RedemptionRequest>

    @Query("SELECT * FROM redemption_requests WHERE user_id = :userId AND reward_id = :rewardId AND created_at >= :sinceTimestamp")
    suspend fun getRequestsForRewardSince(userId: String, rewardId: String, sinceTimestamp: Long): List<RedemptionRequest>

    @Query("SELECT COUNT(*) FROM redemption_requests WHERE user_id = :userId AND reward_id = :rewardId AND status NOT IN ('REJECTED', 'CANCELLED', 'REFUNDED')")
    suspend fun getActiveRedemptionCountForReward(userId: String, rewardId: String): Int

    @Query("SELECT COUNT(*) FROM redemption_requests WHERE user_id = :userId AND created_at >= :sinceTimestamp AND status NOT IN ('REJECTED', 'CANCELLED', 'REFUNDED')")
    suspend fun getDailyActiveRedemptionCount(userId: String, sinceTimestamp: Long): Int

    @Query("DELETE FROM redemption_requests WHERE user_id = :userId")
    suspend fun deleteRedemptionsForUser(userId: String)
}
