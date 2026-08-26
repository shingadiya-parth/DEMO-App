package com.example.core.database

import androidx.room.TypeConverter
import com.example.data.model.AccountStatus
import com.example.data.model.RedemptionStatus
import com.example.data.model.ReferralQualificationType
import com.example.data.model.ReferralRiskState
import com.example.data.model.ReferralStatus
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

    @TypeConverter
    fun fromReferralStatus(value: ReferralStatus?): String? = value?.name

    @TypeConverter
    fun toReferralStatus(value: String?): ReferralStatus? = value?.let { ReferralStatus.valueOf(it) }

    @TypeConverter
    fun fromReferralRiskState(value: ReferralRiskState?): String? = value?.name

    @TypeConverter
    fun toReferralRiskState(value: String?): ReferralRiskState? = value?.let { ReferralRiskState.valueOf(it) }

    @TypeConverter
    fun fromReferralQualificationType(value: ReferralQualificationType?): String? = value?.name

    @TypeConverter
    fun toReferralQualificationType(value: String?): ReferralQualificationType? = value?.let { ReferralQualificationType.valueOf(it) }

    @TypeConverter
    fun fromRewardCategory(value: com.example.data.model.RewardCategory?): String? = value?.name

    @TypeConverter
    fun toRewardCategory(value: String?): com.example.data.model.RewardCategory? = value?.let { com.example.data.model.RewardCategory.valueOf(it) }

    @TypeConverter
    fun fromRewardStockStatus(value: com.example.data.model.RewardStockStatus?): String? = value?.name

    @TypeConverter
    fun toRewardStockStatus(value: String?): com.example.data.model.RewardStockStatus? = value?.let { com.example.data.model.RewardStockStatus.valueOf(it) }

    @TypeConverter
    fun fromNotificationType(value: com.example.data.model.NotificationType?): String? = value?.name

    @TypeConverter
    fun toNotificationType(value: String?): com.example.data.model.NotificationType? = value?.let { com.example.data.model.NotificationType.valueOf(it) }

    @TypeConverter
    fun fromActivityType(value: com.example.data.model.ActivityType?): String? = value?.name

    @TypeConverter
    fun toActivityType(value: String?): com.example.data.model.ActivityType? = value?.let { com.example.data.model.ActivityType.valueOf(it) }

    @TypeConverter
    fun fromActivityCategory(value: com.example.data.model.ActivityCategory?): String? = value?.name

    @TypeConverter
    fun toActivityCategory(value: String?): com.example.data.model.ActivityCategory? = value?.let { com.example.data.model.ActivityCategory.valueOf(it) }
}
