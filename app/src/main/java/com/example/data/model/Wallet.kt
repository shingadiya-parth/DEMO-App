package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Status of a user wallet.
 */
enum class WalletStatus {
    ACTIVE,
    FROZEN,
    SUSPENDED
}

/**
 * Authoritative user Wallet entity.
 * 
 * Associated with exactly one authenticated user.
 * Initial balance is 0 coins.
 * All balance values use strictly 64-bit integer values (Long).
 */
@Entity(
    tableName = "wallets",
    indices = [
        Index(value = ["user_id"], unique = true)
    ]
)
data class Wallet(
    @PrimaryKey
    @ColumnInfo(name = "wallet_id")
    val walletId: String = "wal_${UUID.randomUUID()}",

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "balance")
    val balance: Long = 0L,

    @ColumnInfo(name = "lifetime_earned")
    val lifetimeEarned: Long = 0L,

    @ColumnInfo(name = "lifetime_spent")
    val lifetimeSpent: Long = 0L,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "wallet_status")
    val walletStatus: WalletStatus = WalletStatus.ACTIVE
)
