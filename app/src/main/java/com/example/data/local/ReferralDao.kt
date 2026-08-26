package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ReferralRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ReferralDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferral(referral: ReferralRecord)

    @Update
    suspend fun updateReferral(referral: ReferralRecord)

    @Query("SELECT * FROM referral_record WHERE referral_id = :referralId LIMIT 1")
    suspend fun getReferralById(referralId: String): ReferralRecord?

    @Query("SELECT * FROM referral_record WHERE referred_user_id = :referredUserId LIMIT 1")
    suspend fun getReferralByReferredUserId(referredUserId: String): ReferralRecord?

    @Query("SELECT * FROM referral_record WHERE referrer_user_id = :referrerUserId ORDER BY created_at DESC")
    fun observeReferralsForReferrer(referrerUserId: String): Flow<List<ReferralRecord>>

    @Query("SELECT * FROM referral_record WHERE referrer_user_id = :referrerUserId ORDER BY created_at DESC")
    suspend fun getReferralsForReferrer(referrerUserId: String): List<ReferralRecord>

    @Query("SELECT COUNT(*) FROM referral_record WHERE referrer_user_id = :referrerUserId AND created_at >= :startOfDay")
    suspend fun countReferralsForReferrerToday(referrerUserId: String, startOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM referral_record WHERE referrer_user_id = :referrerUserId")
    suspend fun countLifetimeReferralsForReferrer(referrerUserId: String): Int

    @Query("SELECT COUNT(*) FROM referral_record WHERE referrer_user_id = :referrerUserId AND status IN ('QUALIFIED', 'REWARDED')")
    suspend fun countQualifiedReferralsForReferrer(referrerUserId: String): Int

    @Query("SELECT COUNT(*) FROM referral_record WHERE referrer_user_id = :referrerUserId AND status IN ('PENDING', 'QUALIFYING')")
    suspend fun countPendingReferralsForReferrer(referrerUserId: String): Int

    @Query("SELECT COUNT(*) FROM referral_record WHERE referrer_user_id = :referrerUserId AND status = 'REJECTED'")
    suspend fun countRejectedReferralsForReferrer(referrerUserId: String): Int

    @Query("SELECT SUM(referrer_reward_amount) FROM referral_record WHERE referrer_user_id = :referrerUserId AND status = 'REWARDED'")
    suspend fun getTotalCoinsEarnedFromReferrals(referrerUserId: String): Long?

    @Query("SELECT * FROM referral_record WHERE referred_user_id = :referredUserId AND status IN ('PENDING', 'QUALIFYING') LIMIT 1")
    suspend fun getPendingOrQualifyingReferral(referredUserId: String): ReferralRecord?

    @Query("DELETE FROM referral_record WHERE referrer_user_id = :userId OR referred_user_id = :userId")
    suspend fun deleteReferralsForUser(userId: String)
}
