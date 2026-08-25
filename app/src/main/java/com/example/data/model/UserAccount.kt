package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User Account status flags.
 */
enum class AccountStatus {
    ACTIVE,
    PENDING_VERIFICATION,
    SUSPENDED,
    DEACTIVATED
}

/**
 * Authoritative User Account entity.
 * Note: Protected fields (coin balance, total earned, total spent, referral code, account status)
 * cannot be directly modified through client-side profile editing and must only be modified
 * via verified database ledger updates.
 */
@Entity(tableName = "user_account")
data class UserAccount(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "avatar")
    val avatar: String = "avatar_1",

    @ColumnInfo(name = "country")
    val country: String = "IN",

    @ColumnInfo(name = "date_of_birth")
    val dateOfBirth: String? = null,

    // Cached balance mirror - authoritative source is the ledger
    @ColumnInfo(name = "coin_balance")
    val coinBalance: Long = 0L,

    @ColumnInfo(name = "total_coins_earned")
    val totalCoinsEarned: Long = 0L,

    @ColumnInfo(name = "total_coins_spent")
    val totalCoinsSpent: Long = 0L,

    @ColumnInfo(name = "account_created_date")
    val accountCreationDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_login_at")
    val lastLoginAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_activity")
    val lastActivity: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "referral_code")
    val referralCode: String,

    @ColumnInfo(name = "referred_by")
    val referredBy: String? = null,

    @ColumnInfo(name = "account_status")
    val accountStatus: AccountStatus = AccountStatus.ACTIVE
)
