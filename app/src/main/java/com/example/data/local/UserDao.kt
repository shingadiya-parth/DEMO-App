package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount)

    @Update
    suspend fun updateUser(user: UserAccount)

    @Query("SELECT * FROM user_account WHERE user_id = :userId LIMIT 1")
    fun observeUser(userId: String): Flow<UserAccount?>

    @Query("SELECT * FROM user_account WHERE user_id = :userId LIMIT 1")
    suspend fun getUser(userId: String): UserAccount?

    @Query("SELECT * FROM user_account WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccount?

    @Query("SELECT * FROM user_account WHERE referral_code = :referralCode LIMIT 1")
    suspend fun getUserByReferralCode(referralCode: String): UserAccount?

    @Query("UPDATE user_account SET coin_balance = :newBalance, last_activity = :timestamp WHERE user_id = :userId")
    suspend fun updateCoinBalance(userId: String, newBalance: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_account SET total_coins_earned = :earned, total_coins_spent = :spent, coin_balance = :balance WHERE user_id = :userId")
    suspend fun updateWalletTotals(userId: String, earned: Long, spent: Long, balance: Long)

    @Query("UPDATE user_account SET last_activity = :timestamp WHERE user_id = :userId")
    suspend fun updateLastActivity(userId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_account SET last_login_at = :timestamp, last_activity = :timestamp WHERE user_id = :userId")
    suspend fun updateLastLogin(userId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_account WHERE user_id = :userId")
    suspend fun deleteUser(userId: String)
}
