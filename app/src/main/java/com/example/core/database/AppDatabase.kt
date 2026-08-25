package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.AuthDao
import com.example.data.local.CoinTransactionDao
import com.example.data.local.DailyStreakDao
import com.example.data.local.GamePlayStatsDao
import com.example.data.local.RedemptionDao
import com.example.data.local.UserDao
import com.example.data.local.WalletDao
import com.example.data.model.AuthCredentials
import com.example.data.model.CoinTransaction
import com.example.data.model.DailyStreak
import com.example.data.model.GamePlayStats
import com.example.data.model.RedemptionRequest
import com.example.data.model.UserAccount
import com.example.data.model.Wallet

@Database(
    entities = [
        Wallet::class,
        CoinTransaction::class,
        UserAccount::class,
        AuthCredentials::class,
        RedemptionRequest::class,
        GamePlayStats::class,
        DailyStreak::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun walletDao(): WalletDao
    abstract fun coinTransactionDao(): CoinTransactionDao
    abstract fun userDao(): UserDao
    abstract fun authDao(): AuthDao
    abstract fun redemptionDao(): RedemptionDao
    abstract fun gamePlayStatsDao(): GamePlayStatsDao
    abstract fun dailyStreakDao(): DailyStreakDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "play_rewards_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
