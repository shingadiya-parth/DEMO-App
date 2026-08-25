package com.example.data.repository

import androidx.room.withTransaction
import com.example.core.config.CoinConfig
import com.example.core.database.AppDatabase
import com.example.data.local.CoinTransactionDao
import com.example.data.local.UserDao
import com.example.data.local.WalletDao
import com.example.data.model.CoinTransaction
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.model.Wallet
import com.example.data.model.WalletStatus
import com.example.domain.engine.CoinConversionHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

sealed class TransactionResult {
    data class Success(val transaction: CoinTransaction, val newBalance: Long) : TransactionResult()
    data class Duplicate(val existingTransaction: CoinTransaction, val currentBalance: Long) : TransactionResult()
    data class Error(val message: String) : TransactionResult()
}

enum class TransactionFilter {
    ALL,
    EARNED,
    SPENT
}

data class EarningsSummary(
    val balance: Long,
    val lifetimeEarned: Long,
    val lifetimeSpent: Long,
    val currencyEstimate: String,
    val rateExplanation: String
)

/**
 * Centralized Wallet & Ledger Repository.
 * 
 * Guarantees that EVERY coin balance mutation occurs through an immutable, auditable transaction record.
 * The ledger is append-only and acts as the single source of truth for all virtual coins.
 */
class WalletRepository(
    private val database: AppDatabase,
    private val transactionDao: CoinTransactionDao,
    private val userDao: UserDao,
    private val walletDao: WalletDao? = null
) {

    private val safeWalletDao: WalletDao by lazy {
        walletDao ?: database.walletDao()
    }

    /**
     * Observes live transactions for a specific user.
     */
    fun observeTransactions(userId: String): Flow<List<CoinTransaction>> {
        return transactionDao.observeTransactions(userId)
    }

    /**
     * Observes filtered transactions (All, Earned, Spent).
     */
    fun observeFilteredTransactions(userId: String, filter: TransactionFilter): Flow<List<CoinTransaction>> {
        return when (filter) {
            TransactionFilter.ALL -> transactionDao.observeTransactions(userId)
            TransactionFilter.EARNED -> transactionDao.observeEarnedTransactions(userId)
            TransactionFilter.SPENT -> transactionDao.observeSpentTransactions(userId)
        }
    }

    /**
     * Observes calculated authoritative balance derived from ledger.
     */
    fun observeCalculatedBalance(userId: String): Flow<Long> {
        return transactionDao.observeCalculatedBalance(userId)
    }

    /**
     * Observes lifetime earned coins.
     */
    fun observeLifetimeEarned(userId: String): Flow<Long> {
        return transactionDao.observeLifetimeEarnedCoins(userId)
    }

    /**
     * Observes lifetime spent coins.
     */
    fun observeLifetimeSpent(userId: String): Flow<Long> {
        return transactionDao.observeLifetimeSpentCoins(userId)
    }

    /**
     * Observes live earnings summary.
     */
    fun observeEarningsSummary(userId: String): Flow<EarningsSummary> {
        return transactionDao.observeCalculatedBalance(userId).map { balance ->
            val earned = transactionDao.getLifetimeEarnedCoins(userId)
            val spent = transactionDao.getLifetimeSpentCoins(userId)
            EarningsSummary(
                balance = balance,
                lifetimeEarned = earned,
                lifetimeSpent = spent,
                currencyEstimate = CoinConversionHelper.getCurrencyEstimate(balance),
                rateExplanation = CoinConversionHelper.getRateExplanation()
            )
        }
    }

    suspend fun getRecentTransactions(userId: String, limit: Int = 20): List<CoinTransaction> {
        return transactionDao.getRecentTransactions(userId, limit)
    }

    suspend fun getTransactionsPaged(
        userId: String,
        filter: TransactionFilter,
        limit: Int = 20,
        offset: Int = 0
    ): List<CoinTransaction> {
        return when (filter) {
            TransactionFilter.ALL -> transactionDao.getTransactionsPaged(userId, limit, offset)
            TransactionFilter.EARNED -> transactionDao.getEarnedTransactionsPaged(userId, limit, offset)
            TransactionFilter.SPENT -> transactionDao.getSpentTransactionsPaged(userId, limit, offset)
        }
    }

    suspend fun getCalculatedBalance(userId: String): Long {
        return transactionDao.getCalculatedBalance(userId)
    }

    suspend fun getLifetimeEarned(userId: String): Long {
        return transactionDao.getLifetimeEarnedCoins(userId)
    }

    suspend fun getLifetimeSpent(userId: String): Long {
        return transactionDao.getLifetimeSpentCoins(userId)
    }

    suspend fun getEarningsSummary(userId: String): EarningsSummary {
        val balance = getCalculatedBalance(userId)
        val earned = getLifetimeEarned(userId)
        val spent = getLifetimeSpent(userId)
        return EarningsSummary(
            balance = balance,
            lifetimeEarned = earned,
            lifetimeSpent = spent,
            currencyEstimate = CoinConversionHelper.getCurrencyEstimate(balance),
            rateExplanation = CoinConversionHelper.getRateExplanation()
        )
    }

    /**
     * Ensures a wallet entity exists for the given user.
     */
    suspend fun getOrCreateWallet(userId: String): Wallet {
        val existing = safeWalletDao.getWalletByUserId(userId)
        if (existing != null) return existing

        val newWallet = Wallet(
            walletId = "wal_${UUID.randomUUID()}",
            userId = userId,
            balance = 0L,
            lifetimeEarned = 0L,
            lifetimeSpent = 0L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            walletStatus = WalletStatus.ACTIVE
        )
        safeWalletDao.insertWallet(newWallet)
        return newWallet
    }

    /**
     * Executes atomic coin credit (ADD COINS).
     * 
     * 1. Verify authenticated user.
     * 2. Validate reward request (positive amount).
     * 3. Check idempotency.
     * 4. Read current wallet.
     * 5. Create transaction.
     * 6. Update balance atomically.
     * 7. Update lifetimeEarned.
     * 8. Return the updated balance.
     */
    suspend fun addCoins(
        userId: String,
        type: TransactionType,
        source: String,
        amount: Long,
        referenceId: String? = null,
        idempotencyKey: String? = null,
        metadata: String? = null
    ): TransactionResult {
        if (userId.isBlank()) {
            return TransactionResult.Error("Unauthorized: Invalid user ID")
        }
        if (amount <= 0L) {
            return TransactionResult.Error("Reward amount must be greater than zero")
        }

        // Idempotency check before mutation
        if (!idempotencyKey.isNullOrBlank()) {
            val existing = transactionDao.getTransactionByIdempotencyKey(idempotencyKey)
            if (existing != null) {
                val currentBalance = transactionDao.getCalculatedBalance(userId)
                return TransactionResult.Duplicate(existing, currentBalance)
            }
        }

        return try {
            database.withTransaction {
                // Secondary check inside transaction for concurrency
                if (!idempotencyKey.isNullOrBlank()) {
                    val existing = transactionDao.getTransactionByIdempotencyKey(idempotencyKey)
                    if (existing != null) {
                        val currentBalance = transactionDao.getCalculatedBalance(userId)
                        return@withTransaction TransactionResult.Duplicate(existing, currentBalance)
                    }
                }

                val wallet = getOrCreateWallet(userId)
                if (wallet.walletStatus != WalletStatus.ACTIVE) {
                    return@withTransaction TransactionResult.Error("Wallet is not active (${wallet.walletStatus})")
                }

                val balanceBefore = transactionDao.getCalculatedBalance(userId)
                val balanceAfter = balanceBefore + amount

                val transaction = CoinTransaction(
                    transactionId = "tx_${UUID.randomUUID()}",
                    userId = userId,
                    walletId = wallet.walletId,
                    type = type,
                    source = source,
                    amount = amount,
                    balanceBefore = balanceBefore,
                    balanceAfter = balanceAfter,
                    status = TransactionStatus.COMPLETED,
                    referenceId = referenceId,
                    idempotencyKey = idempotencyKey,
                    metadata = metadata,
                    createdAt = System.currentTimeMillis()
                )

                transactionDao.insertTransaction(transaction)

                val newEarned = transactionDao.getLifetimeEarnedCoins(userId)
                val newSpent = transactionDao.getLifetimeSpentCoins(userId)

                safeWalletDao.updateWalletBalances(
                    userId = userId,
                    newBalance = balanceAfter,
                    lifetimeEarned = newEarned,
                    lifetimeSpent = newSpent
                )

                userDao.updateWalletTotals(userId, newEarned, newSpent, balanceAfter)

                TransactionResult.Success(transaction, balanceAfter)
            }
        } catch (e: Exception) {
            TransactionResult.Error(e.message ?: "Failed to add coins to ledger")
        }
    }

    /**
     * Executes atomic coin deduction (SUBTRACT COINS).
     * 
     * 1. Verify authenticated user.
     * 2. Check sufficient balance.
     * 3. Check idempotency.
     * 4. Create transaction.
     * 5. Update balance atomically.
     * 6. Update lifetimeSpent.
     * Never allow balance to become negative.
     */
    suspend fun subtractCoins(
        userId: String,
        type: TransactionType,
        source: String,
        amount: Long, // Positive amount to deduct
        referenceId: String? = null,
        idempotencyKey: String? = null,
        metadata: String? = null
    ): TransactionResult {
        if (userId.isBlank()) {
            return TransactionResult.Error("Unauthorized: Invalid user ID")
        }
        if (amount <= 0L) {
            return TransactionResult.Error("Deduction amount must be greater than zero")
        }

        // Idempotency check before mutation
        if (!idempotencyKey.isNullOrBlank()) {
            val existing = transactionDao.getTransactionByIdempotencyKey(idempotencyKey)
            if (existing != null) {
                val currentBalance = transactionDao.getCalculatedBalance(userId)
                return TransactionResult.Duplicate(existing, currentBalance)
            }
        }

        return try {
            database.withTransaction {
                if (!idempotencyKey.isNullOrBlank()) {
                    val existing = transactionDao.getTransactionByIdempotencyKey(idempotencyKey)
                    if (existing != null) {
                        val currentBalance = transactionDao.getCalculatedBalance(userId)
                        return@withTransaction TransactionResult.Duplicate(existing, currentBalance)
                    }
                }

                val wallet = getOrCreateWallet(userId)
                if (wallet.walletStatus != WalletStatus.ACTIVE) {
                    return@withTransaction TransactionResult.Error("Wallet is not active (${wallet.walletStatus})")
                }

                val currentBalance = transactionDao.getCalculatedBalance(userId)
                if (currentBalance < amount) {
                    return@withTransaction TransactionResult.Error(
                        "Insufficient balance: available $currentBalance coins, required $amount coins"
                    )
                }

                val balanceBefore = currentBalance
                val balanceAfter = currentBalance - amount
                val deductionAmount = -amount

                val transaction = CoinTransaction(
                    transactionId = "tx_${UUID.randomUUID()}",
                    userId = userId,
                    walletId = wallet.walletId,
                    type = type,
                    source = source,
                    amount = deductionAmount,
                    balanceBefore = balanceBefore,
                    balanceAfter = balanceAfter,
                    status = TransactionStatus.COMPLETED,
                    referenceId = referenceId,
                    idempotencyKey = idempotencyKey,
                    metadata = metadata,
                    createdAt = System.currentTimeMillis()
                )

                transactionDao.insertTransaction(transaction)

                val newEarned = transactionDao.getLifetimeEarnedCoins(userId)
                val newSpent = transactionDao.getLifetimeSpentCoins(userId)

                safeWalletDao.updateWalletBalances(
                    userId = userId,
                    newBalance = balanceAfter,
                    lifetimeEarned = newEarned,
                    lifetimeSpent = newSpent
                )

                userDao.updateWalletTotals(userId, newEarned, newSpent, balanceAfter)

                TransactionResult.Success(transaction, balanceAfter)
            }
        } catch (e: Exception) {
            TransactionResult.Error(e.message ?: "Failed to deduct coins from ledger")
        }
    }

    /**
     * Backward-compatible helper method for recordTransaction.
     */
    suspend fun recordTransaction(
        userId: String,
        type: TransactionType,
        source: String,
        amount: Long,
        referenceId: String? = null,
        idempotencyKey: String? = null,
        metadata: String? = null
    ): TransactionResult {
        return if (amount >= 0) {
            addCoins(
                userId = userId,
                type = type,
                source = source,
                amount = amount,
                referenceId = referenceId,
                idempotencyKey = idempotencyKey,
                metadata = metadata
            )
        } else {
            subtractCoins(
                userId = userId,
                type = type,
                source = source,
                amount = -amount,
                referenceId = referenceId,
                idempotencyKey = idempotencyKey,
                metadata = metadata
            )
        }
    }

    /**
     * Protected administrative adjustment operation.
     */
    suspend fun executeAdminAdjustment(
        targetUserId: String,
        amount: Long,
        reason: String,
        adminIdentifier: String = "SYSTEM_ADMIN",
        idempotencyKey: String? = null
    ): TransactionResult {
        val key = idempotencyKey ?: "ADMIN_${targetUserId}_${System.currentTimeMillis()}"
        return if (amount >= 0) {
            addCoins(
                userId = targetUserId,
                type = TransactionType.ADMIN_ADJUSTMENT,
                source = "admin_adjustment",
                amount = amount,
                referenceId = adminIdentifier,
                idempotencyKey = key,
                metadata = "Admin Adjustment: $reason (by $adminIdentifier)"
            )
        } else {
            subtractCoins(
                userId = targetUserId,
                type = TransactionType.ADMIN_ADJUSTMENT,
                source = "admin_adjustment",
                amount = -amount,
                referenceId = adminIdentifier,
                idempotencyKey = key,
                metadata = "Admin Deduction: $reason (by $adminIdentifier)"
            )
        }
    }
}
