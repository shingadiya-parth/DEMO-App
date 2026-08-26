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
import com.example.domain.engine.BubblePopGameEngine
import com.example.domain.engine.CoinTossGameEngine
import com.example.domain.engine.GameEngine
import com.example.domain.engine.PuzzleGameEngine
import com.example.domain.engine.RewardEngine
import com.example.domain.engine.ScratchGameEngine
import com.example.domain.engine.SpinGameEngine
import com.example.domain.engine.TicTacToeGameEngine
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
    val referralDao by lazy { database.referralDao() }

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
        RedemptionRepository(redemptionDao, walletRepository, userRepository, database)
    }

    val notificationService: NotificationService by lazy {
        NotificationService()
    }

    val referralRiskEngine: com.example.domain.engine.ReferralRiskEngine by lazy {
        com.example.domain.engine.ReferralRiskEngine(referralDao)
    }

    val referralRepository: com.example.data.repository.ReferralRepository by lazy {
        com.example.data.repository.ReferralRepository(
            referralDao = referralDao,
            userDao = userDao,
            riskEngine = referralRiskEngine,
            notificationService = notificationService
        )
    }

    val referralQualificationEngine: com.example.domain.engine.ReferralQualificationEngine by lazy {
        com.example.domain.engine.ReferralQualificationEngine(
            referralDao = referralDao,
            userDao = userDao,
            notificationService = notificationService,
            rewardEngineProvider = { rewardEngine }
        )
    }

    val rewardEngine: RewardEngine by lazy {
        RewardEngine(walletRepository, gameRepository, userRepository).apply {
            this.referralQualificationEngine = this@AppContainer.referralQualificationEngine
        }
    }

    val earnRepository: EarnRepository by lazy {
        EarnRepository(walletRepository, userRepository, rewardEngine, dailyStreakDao)
    }

    val gameEngine: GameEngine by lazy {
        GameEngine(rewardEngine)
    }

    val spinGameEngine: SpinGameEngine by lazy {
        SpinGameEngine(rewardEngine, gameRepository, userRepository, walletRepository)
    }

    val scratchGameEngine: ScratchGameEngine by lazy {
        ScratchGameEngine(rewardEngine, gameRepository, userRepository, walletRepository)
    }

    val puzzleGameEngine: PuzzleGameEngine by lazy {
        PuzzleGameEngine(rewardEngine, gameRepository, userRepository, walletRepository)
    }

    val coinTossGameEngine: CoinTossGameEngine by lazy {
        CoinTossGameEngine(rewardEngine, gameRepository, userRepository, walletRepository)
    }

    val ticTacToeGameEngine: TicTacToeGameEngine by lazy {
        TicTacToeGameEngine(rewardEngine, gameRepository, userRepository, walletRepository)
    }

    val bubblePopGameEngine: BubblePopGameEngine by lazy {
        BubblePopGameEngine(rewardEngine, gameRepository, userRepository, walletRepository)
    }

    val adMobService: AdMobService by lazy {
        AdMobService(rewardEngine)
    }
}
