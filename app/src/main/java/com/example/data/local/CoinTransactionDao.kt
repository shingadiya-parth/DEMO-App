package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CoinTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinTransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: CoinTransaction)

    @Query("SELECT * FROM coin_transactions WHERE user_id = :userId ORDER BY created_at DESC")
    fun observeTransactions(userId: String): Flow<List<CoinTransaction>>

    @Query("SELECT * FROM coin_transactions WHERE user_id = :userId AND amount > 0 ORDER BY created_at DESC")
    fun observeEarnedTransactions(userId: String): Flow<List<CoinTransaction>>

    @Query("SELECT * FROM coin_transactions WHERE user_id = :userId AND amount < 0 ORDER BY created_at DESC")
    fun observeSpentTransactions(userId: String): Flow<List<CoinTransaction>>

    @Query("SELECT * FROM coin_transactions WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsPaged(userId: String, limit: Int, offset: Int): List<CoinTransaction>

    @Query("SELECT * FROM coin_transactions WHERE user_id = :userId AND amount > 0 ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getEarnedTransactionsPaged(userId: String, limit: Int, offset: Int): List<CoinTransaction>

    @Query("SELECT * FROM coin_transactions WHERE user_id = :userId AND amount < 0 ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getSpentTransactionsPaged(userId: String, limit: Int, offset: Int): List<CoinTransaction>

    @Query("SELECT * FROM coin_transactions WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentTransactions(userId: String, limit: Int = 20): List<CoinTransaction>

    @Query("SELECT * FROM coin_transactions WHERE transaction_id = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: String): CoinTransaction?

    @Query("SELECT * FROM coin_transactions WHERE idempotency_key = :idempotencyKey LIMIT 1")
    suspend fun getTransactionByIdempotencyKey(idempotencyKey: String): CoinTransaction?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM coin_transactions WHERE user_id = :userId AND status = 'COMPLETED'")
    fun observeCalculatedBalance(userId: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM coin_transactions WHERE user_id = :userId AND status = 'COMPLETED'")
    suspend fun getCalculatedBalance(userId: String): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM coin_transactions WHERE user_id = :userId AND amount > 0 AND status = 'COMPLETED'")
    fun observeLifetimeEarnedCoins(userId: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM coin_transactions WHERE user_id = :userId AND amount > 0 AND status = 'COMPLETED'")
    suspend fun getLifetimeEarnedCoins(userId: String): Long

    @Query("SELECT COALESCE(ABS(SUM(amount)), 0) FROM coin_transactions WHERE user_id = :userId AND amount < 0 AND status = 'COMPLETED'")
    fun observeLifetimeSpentCoins(userId: String): Flow<Long>

    @Query("SELECT COALESCE(ABS(SUM(amount)), 0) FROM coin_transactions WHERE user_id = :userId AND amount < 0 AND status = 'COMPLETED'")
    suspend fun getLifetimeSpentCoins(userId: String): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM coin_transactions WHERE user_id = :userId AND amount > 0 AND created_at >= :sinceTimestamp AND status = 'COMPLETED'")
    suspend fun getEarnedCoinsSince(userId: String, sinceTimestamp: Long): Long

    @Query("SELECT COUNT(*) FROM coin_transactions WHERE user_id = :userId")
    suspend fun getTransactionCount(userId: String): Int

    @Query("DELETE FROM coin_transactions WHERE user_id = :userId")
    suspend fun deleteTransactionsForUser(userId: String)
}
