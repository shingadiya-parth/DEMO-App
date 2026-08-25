package com.example.core.database

import androidx.room.TypeConverter
import com.example.data.model.AccountStatus
import com.example.data.model.RedemptionStatus
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.model.WalletStatus

class Converters {

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? = value?.let { TransactionType.valueOf(it) }

    @TypeConverter
    fun fromTransactionStatus(value: TransactionStatus?): String? = value?.name

    @TypeConverter
    fun toTransactionStatus(value: String?): TransactionStatus? = value?.let { TransactionStatus.valueOf(it) }

    @TypeConverter
    fun fromAccountStatus(value: AccountStatus?): String? = value?.name

    @TypeConverter
    fun toAccountStatus(value: String?): AccountStatus? = value?.let { AccountStatus.valueOf(it) }

    @TypeConverter
    fun fromRedemptionStatus(value: RedemptionStatus?): String? = value?.name

    @TypeConverter
    fun toRedemptionStatus(value: String?): RedemptionStatus? = value?.let { RedemptionStatus.valueOf(it) }

    @TypeConverter
    fun fromWalletStatus(value: WalletStatus?): String? = value?.name

    @TypeConverter
    fun toWalletStatus(value: String?): WalletStatus? = value?.let { WalletStatus.valueOf(it) }
}
