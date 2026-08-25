package com.example.wallet

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.CoinConfig
import com.example.core.database.AppDatabase
import com.example.data.model.AccountStatus
import com.example.data.model.GameDefinition
import com.example.data.model.GameDifficulty
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.model.UserAccount
import com.example.data.model.WalletStatus
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthSessionManager
import com.example.data.repository.GameRepository
import com.example.data.repository.TransactionFilter
import com.example.data.repository.TransactionResult
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import com.example.domain.engine.CoinConversionHelper
import com.example.domain.engine.GameRewardConfigManager
import com.example.domain.engine.RewardEngine
import com.example.domain.engine.RewardGrantResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WalletAndTransactionEngineTest {

    private lateinit var database: AppDatabase
    private lateinit var sessionManager: AuthSessionManager
    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: UserRepository
    private lateinit var walletRepository: WalletRepository
    private lateinit var gameRepository: GameRepository
    private lateinit var rewardEngine: RewardEngine

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionManager = AuthSessionManager(context)
        sessionManager.clearSession()

        authRepository = AuthRepository(
            database = database,
            authDao = database.authDao(),
            userDao = database.userDao(),
            coinTransactionDao = database.coinTransactionDao(),
            redemptionDao = database.redemptionDao(),
            gamePlayStatsDao = database.gamePlayStatsDao(),
            sessionManager = sessionManager
        )
        userRepository = UserRepository(database.userDao(), authRepository)
        walletRepository = WalletRepository(
            database = database,
            transactionDao = database.coinTransactionDao(),
            userDao = database.userDao(),
            walletDao = database.walletDao()
        )
        gameRepository = GameRepository(database.gamePlayStatsDao())
        rewardEngine = RewardEngine(
            walletRepository = walletRepository,
            gameRepository = gameRepository,
            userRepository = userRepository
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testInitialWalletBalanceIsZero() = runTest {
        val user = authRepository.signUp("PlayerOne", "player1@game.com", "Secret123", "Secret123").getOrThrow()

        val balance = walletRepository.getCalculatedBalance(user.userId)
        assertEquals(0L, balance)

        val wallet = database.walletDao().getWalletByUserId(user.userId)
        assertNotNull(wallet)
        assertEquals(0L, wallet?.balance)
        assertEquals(0L, wallet?.lifetimeEarned)
        assertEquals(0L, wallet?.lifetimeSpent)
        assertEquals(WalletStatus.ACTIVE, wallet?.walletStatus)
    }

    @Test
    fun testAddAndSpendCoinsStepSequence() = runTest {
        val user = authRepository.signUp("CoinTester", "tester@game.com", "Secret123", "Secret123").getOrThrow()

        // 1. Initial balance is 0
        assertEquals(0L, walletRepository.getCalculatedBalance(user.userId))

        // 2. Add 100 coins
        val add1Result = walletRepository.addCoins(
            userId = user.userId,
            type = TransactionType.GAME_REWARD,
            source = "spin_win",
            amount = 100L,
            idempotencyKey = "key_step_1"
        )
        assertTrue(add1Result is TransactionResult.Success)
        assertEquals(100L, (add1Result as TransactionResult.Success).newBalance)
        assertEquals(100L, walletRepository.getCalculatedBalance(user.userId))

        // 3. Add another 50 coins -> balance becomes 150
        val add2Result = walletRepository.addCoins(
            userId = user.userId,
            type = TransactionType.DAILY_BONUS,
            source = "daily_checkin",
            amount = 50L,
            idempotencyKey = "key_step_2"
        )
        assertTrue(add2Result is TransactionResult.Success)
        assertEquals(150L, (add2Result as TransactionResult.Success).newBalance)
        assertEquals(150L, walletRepository.getCalculatedBalance(user.userId))

        // 4. Spend 100 coins -> balance becomes 50
        val spend1Result = walletRepository.subtractCoins(
            userId = user.userId,
            type = TransactionType.REDEMPTION_DEDUCTION,
            source = "voucher_redemption",
            amount = 100L,
            idempotencyKey = "key_step_3"
        )
        assertTrue(spend1Result is TransactionResult.Success)
        assertEquals(50L, (spend1Result as TransactionResult.Success).newBalance)
        assertEquals(50L, walletRepository.getCalculatedBalance(user.userId))

        // 5. Attempt to spend 100 coins with 50 balance -> must fail
        val spendFailResult = walletRepository.subtractCoins(
            userId = user.userId,
            type = TransactionType.REDEMPTION_DEDUCTION,
            source = "voucher_redemption_2",
            amount = 100L,
            idempotencyKey = "key_step_4"
        )
        assertTrue(spendFailResult is TransactionResult.Error)

        // 6. Balance must remain exactly 50
        assertEquals(50L, walletRepository.getCalculatedBalance(user.userId))
    }

    @Test
    fun testIdempotencyPreventsDuplicateRewards() = runTest {
        val user = authRepository.signUp("IdemUser", "idem@game.com", "Secret123", "Secret123").getOrThrow()
        val idempotencyToken = "UNIQUE_SPIN_TOKEN_12345"

        // First reward claim
        val firstResult = rewardEngine.processReward(
            userId = user.userId,
            rewardType = TransactionType.SPIN_REWARD,
            source = "spin_win",
            amount = 75L,
            idempotencyKey = idempotencyToken
        )
        assertTrue(firstResult is RewardGrantResult.Success)
        assertEquals(75L, (firstResult as RewardGrantResult.Success).newBalance)

        // Duplicate claim attempt with identical key
        val duplicateResult = rewardEngine.processReward(
            userId = user.userId,
            rewardType = TransactionType.SPIN_REWARD,
            source = "spin_win",
            amount = 75L,
            idempotencyKey = idempotencyToken
        )
        assertTrue(duplicateResult is RewardGrantResult.AlreadyClaimed)
        assertEquals(75L, (duplicateResult as RewardGrantResult.AlreadyClaimed).currentBalance)

        // Balance strictly unchanged
        assertEquals(75L, walletRepository.getCalculatedBalance(user.userId))
    }

    @Test
    fun testTwoUsersCannotAccessOrModifyEachOthersWallets() = runTest {
        val userA = authRepository.signUp("UserA", "usera@game.com", "Pass123", "Pass123").getOrThrow()
        val userB = authRepository.signUp("UserB", "userb@game.com", "Pass123", "Pass123").getOrThrow()

        // Credit User A
        walletRepository.addCoins(userA.userId, TransactionType.GAME_REWARD, "game_1", 500L, idempotencyKey = "ua_1")

        // Credit User B
        walletRepository.addCoins(userB.userId, TransactionType.GAME_REWARD, "game_2", 200L, idempotencyKey = "ub_1")

        assertEquals(500L, walletRepository.getCalculatedBalance(userA.userId))
        assertEquals(200L, walletRepository.getCalculatedBalance(userB.userId))

        // User A spends 100 -> does NOT affect User B
        walletRepository.subtractCoins(userA.userId, TransactionType.REDEMPTION_DEDUCTION, "item", 100L, idempotencyKey = "ua_2")

        assertEquals(400L, walletRepository.getCalculatedBalance(userA.userId))
        assertEquals(200L, walletRepository.getCalculatedBalance(userB.userId))

        val userATransactions = database.coinTransactionDao().observeTransactions(userA.userId).first()
        val userBTransactions = database.coinTransactionDao().observeTransactions(userB.userId).first()

        assertEquals(2, userATransactions.size)
        assertEquals(1, userBTransactions.size)
        assertTrue(userATransactions.all { it.userId == userA.userId })
        assertTrue(userBTransactions.all { it.userId == userB.userId })
    }

    @Test
    fun testNegativeBalanceIsImpossible() = runTest {
        val user = authRepository.signUp("NegativeTester", "neg@game.com", "Pass123", "Pass123").getOrThrow()

        // Balance is 0, attempt to deduct 1 coin
        val failResult = walletRepository.subtractCoins(
            userId = user.userId,
            type = TransactionType.REDEMPTION_DEDUCTION,
            source = "test",
            amount = 1L,
            idempotencyKey = "neg_try_1"
        )
        assertTrue(failResult is TransactionResult.Error)
        assertEquals(0L, walletRepository.getCalculatedBalance(user.userId))
    }

    @Test
    fun testLifetimeEarnedAndSpentCalculations() = runTest {
        val user = authRepository.signUp("LifetimeTester", "life@game.com", "Pass123", "Pass123").getOrThrow()

        walletRepository.addCoins(user.userId, TransactionType.GAME_REWARD, "game", 300L, idempotencyKey = "k1")
        walletRepository.addCoins(user.userId, TransactionType.DAILY_BONUS, "daily", 100L, idempotencyKey = "k2")
        walletRepository.subtractCoins(user.userId, TransactionType.REDEMPTION_DEDUCTION, "red", 150L, idempotencyKey = "k3")

        val summary = walletRepository.getEarningsSummary(user.userId)
        assertEquals(250L, summary.balance) // 300 + 100 - 150 = 250
        assertEquals(400L, summary.lifetimeEarned) // 300 + 100 = 400
        assertEquals(150L, summary.lifetimeSpent) // 150
    }

    @Test
    fun testCoinConversionRateRule() = runTest {
        // Centralized rule: 700 coins = ₹1.00
        assertEquals("₹1.00", CoinConversionHelper.getCurrencyEstimate(700L))
        assertEquals("₹10.00", CoinConversionHelper.getCurrencyEstimate(7000L))
        assertEquals("₹0.00", CoinConversionHelper.getCurrencyEstimate(0L))
        assertEquals("700 Coins = ₹1.00 INR", CoinConversionHelper.getRateExplanation())
    }

    @Test
    fun testTransactionHistoryFilter() = runTest {
        val user = authRepository.signUp("FilterUser", "filter@game.com", "Pass123", "Pass123").getOrThrow()

        walletRepository.addCoins(user.userId, TransactionType.GAME_REWARD, "g1", 100L, idempotencyKey = "f1")
        walletRepository.addCoins(user.userId, TransactionType.DAILY_BONUS, "g2", 50L, idempotencyKey = "f2")
        walletRepository.subtractCoins(user.userId, TransactionType.REDEMPTION_DEDUCTION, "r1", 40L, idempotencyKey = "f3")

        val allTx = walletRepository.observeFilteredTransactions(user.userId, TransactionFilter.ALL).first()
        val earnedTx = walletRepository.observeFilteredTransactions(user.userId, TransactionFilter.EARNED).first()
        val spentTx = walletRepository.observeFilteredTransactions(user.userId, TransactionFilter.SPENT).first()

        assertEquals(3, allTx.size)
        assertEquals(2, earnedTx.size)
        assertEquals(1, spentTx.size)
        assertTrue(earnedTx.all { it.amount > 0 })
        assertTrue(spentTx.all { it.amount < 0 })
    }

    @Test
    fun testAdminAdjustmentCreatesProperLedgerEntry() = runTest {
        val user = authRepository.signUp("AdminAdjUser", "adminadj@game.com", "Pass123", "Pass123").getOrThrow()

        val adminResult = walletRepository.executeAdminAdjustment(
            targetUserId = user.userId,
            amount = 1000L,
            reason = "Tournament Winner Bonus",
            adminIdentifier = "ADMIN_SUPERVISOR_99"
        )
        assertTrue(adminResult is TransactionResult.Success)
        assertEquals(1000L, (adminResult as TransactionResult.Success).newBalance)

        val tx = database.coinTransactionDao().observeTransactions(user.userId).first().first()
        assertEquals(TransactionType.ADMIN_ADJUSTMENT, tx.type)
        assertEquals("admin_adjustment", tx.source)
        assertTrue(tx.metadata?.contains("Tournament Winner Bonus") == true)
        assertEquals("ADMIN_SUPERVISOR_99", tx.referenceId)
    }

    @Test
    fun testGameRewardConfigValidation() = runTest {
        val config = GameRewardConfigManager.getConfig("spin_win")
        assertNotNull(config)
        assertEquals("Spin & Win", config?.title)
        assertEquals(50L, config?.rewardAmount)
        assertEquals(10, config?.dailyLimit)
        assertTrue(config?.enabled == true)
    }
}
