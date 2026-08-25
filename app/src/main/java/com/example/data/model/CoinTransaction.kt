package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Immutable Coin Transaction entity representing an append-only entry in the centralized ledger.
 * 
 * Every coin change in the app MUST create a permanent transaction record.
 * Direct modification of the wallet balance without an atomic ledger entry is prohibited.
 */
@Entity(
    tableName = "coin_transactions",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["wallet_id"]),
        Index(value = ["idempotency_key"], unique = false),
        Index(value = ["created_at"])
    ]
)
data class CoinTransaction(
    @PrimaryKey
    @ColumnInfo(name = "transaction_id")
    val transactionId: String = "tx_${UUID.randomUUID()}",

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "wallet_id")
    val walletId: String = "",

    @ColumnInfo(name = "type")
    val type: TransactionType,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "amount")
    val amount: Long, // Positive for credits, negative for deductions

    @ColumnInfo(name = "balance_before")
    val balanceBefore: Long = 0L,

    @ColumnInfo(name = "balance_after")
    val balanceAfter: Long = 0L,

    @ColumnInfo(name = "status")
    val status: TransactionStatus = TransactionStatus.COMPLETED,

    @ColumnInfo(name = "reference_id")
    val referenceId: String? = null,

    @ColumnInfo(name = "idempotency_key")
    val idempotencyKey: String? = null,

    @ColumnInfo(name = "metadata")
    val metadata: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
