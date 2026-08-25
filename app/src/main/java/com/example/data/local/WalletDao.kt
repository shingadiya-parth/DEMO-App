package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Wallet
import com.example.data.model.WalletStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: Wallet)

    @Update
    suspend fun updateWallet(wallet: Wallet)

    @Query("SELECT * FROM wallets WHERE user_id = :userId LIMIT 1")
    suspend fun getWalletByUserId(userId: String): Wallet?

    @Query("SELECT * FROM wallets WHERE wallet_id = :walletId LIMIT 1")
    suspend fun getWalletById(walletId: String): Wallet?

    @Query("SELECT * FROM wallets WHERE user_id = :userId LIMIT 1")
    fun observeWallet(userId: String): Flow<Wallet?>

    @Query("SELECT balance FROM wallets WHERE user_id = :userId LIMIT 1")
    fun observeBalance(userId: String): Flow<Long?>

    @Query("""
        UPDATE wallets 
        SET balance = :newBalance, 
            lifetime_earned = :lifetimeEarned, 
            lifetime_spent = :lifetimeSpent, 
            updated_at = :updatedAt 
        WHERE user_id = :userId
    """)
    suspend fun updateWalletBalances(
        userId: String,
        newBalance: Long,
        lifetimeEarned: Long,
        lifetimeSpent: Long,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE wallets SET wallet_status = :status, updated_at = :updatedAt WHERE user_id = :userId")
    suspend fun updateWalletStatus(
        userId: String,
        status: WalletStatus,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM wallets WHERE user_id = :userId")
    suspend fun deleteWalletForUser(userId: String)
}
