package com.example.core.di

import android.content.Context
import com.example.core.database.AppDatabase
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthSessionManager
import com.example.data.repository.EarnRepository
import com.example.data.repository.GameRepository
import com.example.data.repository.RedemptionRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import com.example.domain.engine.GameEngine
import com.example.domain.engine.RewardEngine
import com.example.services.ads.AdMobService
import com.example.services.notifications.NotificationService

/**
 * Central Dependency Injection Container.
 */
class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val userDao by lazy { database.userDao() }
    val authDao by lazy { database.authDao() }
    val coinTransactionDao by lazy { database.coinTransactionDao() }
    val redemptionDao by lazy { database.redemptionDao() }
    val gamePlayStatsDao by lazy { database.gamePlayStatsDao() }
    val dailyStreakDao by lazy { database.dailyStreakDao() }

    val sessionManager by lazy {
        AuthSessionManager(context.applicationContext)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            database = database,
            authDao = authDao,
            userDao = userDao,
            coinTransactionDao = coinTransactionDao,
            redemptionDao = redemptionDao,
            gamePlayStatsDao = gamePlayStatsDao,
            sessionManager = sessionManager
        )
    }

    val userRepository: UserRepository by lazy {
        UserRepository(userDao, authRepository)
    }

    val walletRepository: WalletRepository by lazy {
        WalletRepository(database, coinTransactionDao, userDao)
    }

    val gameRepository: GameRepository by lazy {
        GameRepository(gamePlayStatsDao)
    }

    val redemptionRepository: RedemptionRepository by lazy {
        RedemptionRepository(redemptionDao, walletRepository, userRepository)
    }

    val rewardEngine: RewardEngine by lazy {
        RewardEngine(walletRepository, gameRepository, userRepository)
    }

    val earnRepository: EarnRepository by lazy {
        EarnRepository(walletRepository, userRepository, rewardEngine, dailyStreakDao)
    }

    val gameEngine: GameEngine by lazy {
        GameEngine(rewardEngine)
    }

    val adMobService: AdMobService by lazy {
        AdMobService(rewardEngine)
    }

    val notificationService: NotificationService by lazy {
        NotificationService()
    }
}
