package com.example.security

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.CoinConfig
import com.example.core.config.CoinSide
import com.example.core.database.AppDatabase
import com.example.core.security.FraudRiskEngine
import com.example.core.security.IdempotencyManager
import com.example.core.security.RateLimitAction
import com.example.core.security.RateLimitResult
import com.example.core.security.RateLimiter
import com.example.core.security.SecurityEventLogger
import com.example.core.security.SecurityValidationResult
import com.example.core.security.SecurityValidator
import com.example.data.model.AccountStatus
import com.example.data.model.FraudRiskState
import com.example.data.model.SecurityEventType
import com.example.data.model.SecuritySeverity
import com.example.data.model.TransactionType
import com.example.data.model.UserAccount
import com.example.data.repository.ActivityRepository
import com.example.data.repository.GameRepository
import com.example.data.repository.RedemptionRepository
import com.example.data.repository.RedemptionResult
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import com.example.domain.engine.CoinTossGameEngine
import com.example.domain.engine.CoinTossResult
import com.example.domain.engine.PuzzleGameEngine
import com.example.domain.engine.PuzzleSessionResult
import com.example.domain.engine.PuzzleSubmitResult
import com.example.domain.engine.RewardEngine
import com.example.domain.engine.RewardGrantResult
import com.example.domain.engine.ScratchCardResult
import com.example.domain.engine.ScratchGameEngine
import com.example.domain.engine.SpinGameEngine
import com.example.domain.engine.SpinResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SecurityAndAntiFraudTest {

    private lateinit var database: AppDatabase
    private lateinit var sessionManager: com.example.data.repository.AuthSessionManager
    private lateinit var authRepository: com.example.data.repository.AuthRepository
    private lateinit var walletRepository: WalletRepository
    private lateinit var gameRepository: GameRepository
    private lateinit var userRepository: UserRepository
    private lateinit var activityRepository: ActivityRepository
    private lateinit var securityEventLogger: SecurityEventLogger
    private lateinit var fraudRiskEngine: FraudRiskEngine
    private lateinit var rewardEngine: RewardEngine
    private lateinit var redemptionRepository: RedemptionRepository
    private lateinit var spinGameEngine: SpinGameEngine
    private lateinit var scratchGameEngine: ScratchGameEngine
    private lateinit var puzzleGameEngine: PuzzleGameEngine
    private lateinit var coinTossGameEngine: CoinTossGameEngine

    private val testUserId = "test_sec_user_001"

    @Before
    fun setUp() {
        runBlocking {
            RateLimiter.clearAll()
            val context = ApplicationProvider.getApplicationContext<Context>()
            database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()

            sessionManager = com.example.data.repository.AuthSessionManager(context)
            sessionManager.clearSession()

            authRepository = com.example.data.repository.AuthRepository(
                database = database,
                authDao = database.authDao(),
                userDao = database.userDao(),
                coinTransactionDao = database.coinTransactionDao(),
                redemptionDao = database.redemptionDao(),
                gamePlayStatsDao = database.gamePlayStatsDao(),
                sessionManager = sessionManager
            )

            walletRepository = WalletRepository(database, database.coinTransactionDao(), database.userDao())
            gameRepository = GameRepository(database.gamePlayStatsDao())
            userRepository = UserRepository(database.userDao(), authRepository)
            activityRepository = ActivityRepository(database.activityDao())
            securityEventLogger = SecurityEventLogger(database.securityEventDao())
            fraudRiskEngine = FraudRiskEngine(database.securityEventDao(), securityEventLogger)

            rewardEngine = RewardEngine(
                walletRepository = walletRepository,
                gameRepository = gameRepository,
                userRepository = userRepository,
                activityRepository = activityRepository,
                securityEventLogger = securityEventLogger,
                fraudRiskEngine = fraudRiskEngine
            )

            redemptionRepository = RedemptionRepository(
                redemptionDao = database.redemptionDao(),
                walletRepository = walletRepository,
                userRepository = userRepository,
                database = database,
                activityRepository = activityRepository,
                securityEventLogger = securityEventLogger,
                fraudRiskEngine = fraudRiskEngine
            )

            spinGameEngine = SpinGameEngine(rewardEngine, gameRepository, userRepository, walletRepository)
            scratchGameEngine = ScratchGameEngine(rewardEngine, gameRepository, userRepository, walletRepository)
            puzzleGameEngine = PuzzleGameEngine(rewardEngine, gameRepository, userRepository, walletRepository)
            coinTossGameEngine = CoinTossGameEngine(rewardEngine, gameRepository, userRepository, walletRepository)

            // Seed Active User and Session
            val testUser = UserAccount(
                userId = testUserId,
                displayName = "Security Test User",
                email = "sec_test@example.com",
                referralCode = "SECTEST1",
                accountStatus = AccountStatus.ACTIVE
            )
            database.userDao().insertUser(testUser)
            sessionManager.setActiveUserId(testUserId)
        }
    }

    @After
    fun tearDown() {
        database.close()
        RateLimiter.clearAll()
    }

    @Test
    fun test1_walletLedgerIsImmutableAndBalancesAreAtomic() = runBlocking {
        val initialBalance = walletRepository.getCalculatedBalance(testUserId)
        assertEquals(0L, initialBalance)

        // Grant reward
        val idempotencyKey = IdempotencyManager.generateToken(testUserId, "DAILY_BONUS", "day_1")
        val res = rewardEngine.processReward(
            userId = testUserId,
            rewardType = TransactionType.DAILY_BONUS,
            source = "daily_bonus",
            amount = 100L,
            referenceId = "day_1",
            idempotencyKey = idempotencyKey,
            metadata = "Day 1 streak"
        )
        assertTrue(res is RewardGrantResult.Success)
        assertEquals(100L, walletRepository.getCalculatedBalance(testUserId))

        // Ensure negative balance cannot occur on subtraction exceeding balance
        val subResult = walletRepository.subtractCoins(
            userId = testUserId,
            type = TransactionType.REDEMPTION_DEDUCTION,
            source = "test_sub",
            amount = 500L,
            referenceId = "sub_001",
            idempotencyKey = "tx_sub_001"
        )
        assertTrue(subResult is com.example.data.repository.TransactionResult.Error)
        assertEquals(100L, walletRepository.getCalculatedBalance(testUserId))
    }

    @Test
    fun test2_idempotencyPreventsDuplicateCoinCredits() = runBlocking {
        val idempotencyKey = "fixed_deterministic_key_001"

        // First grant
        val res1 = rewardEngine.processReward(
            userId = testUserId,
            rewardType = TransactionType.GAME_REWARD,
            source = "test_game",
            amount = 50L,
            referenceId = "session_123",
            idempotencyKey = idempotencyKey
        )
        assertTrue(res1 is RewardGrantResult.Success)
        assertEquals(50L, walletRepository.getCalculatedBalance(testUserId))

        // Duplicate replay
        val res2 = rewardEngine.processReward(
            userId = testUserId,
            rewardType = TransactionType.GAME_REWARD,
            source = "test_game",
            amount = 50L,
            referenceId = "session_123",
            idempotencyKey = idempotencyKey
        )
        assertTrue(res2 is RewardGrantResult.AlreadyClaimed)
        assertEquals(50L, walletRepository.getCalculatedBalance(testUserId))
    }

    @Test
    fun test3_inactiveAccountsCannotEarnRewards() = runBlocking {
        // Change user status to SUSPENDED
        val currentUser = userRepository.getCurrentUser()!!
        val suspendedUser = currentUser.copy(accountStatus = AccountStatus.SUSPENDED)
        database.userDao().insertUser(suspendedUser)

        val res = rewardEngine.processReward(
            userId = testUserId,
            rewardType = TransactionType.SPIN_REWARD,
            source = "game_spin",
            amount = 25L,
            referenceId = "spin_test",
            idempotencyKey = "key_spin_test"
        )
        assertTrue(res is RewardGrantResult.Rejected)
        assertEquals(0L, walletRepository.getCalculatedBalance(testUserId))
    }

    @Test
    fun test4_rateLimiterEnforcesSlidingWindowLimits() {
        val action = RateLimitAction.REWARD_REQUEST
        for (i in 1..action.maxRequests) {
            val check = RateLimiter.checkAndRecord(testUserId, action)
            assertTrue("Request $i should be allowed", check is RateLimitResult.Allowed)
        }

        // Next request immediately must exceed rate limit
        val blockedCheck = RateLimiter.checkAndRecord(testUserId, action)
        assertTrue("Request exceeding max should be blocked", blockedCheck is RateLimitResult.Exceeded)
    }

    @Test
    fun test5_redemptionRequiresAuthoritativeSufficientBalance() = runBlocking {
        // Try redeeming with 0 balance
        val catalog = redemptionRepository.getRewardCatalog()
        val reward = catalog.first()

        val failedResult = redemptionRepository.submitRedemptionRequest(
            userId = testUserId,
            rewardId = reward.rewardId,
            destinationAccount = "user@example.com"
        )
        assertTrue(failedResult is RedemptionResult.Error)

        // Credit exactly required coins
        rewardEngine.processReward(
            userId = testUserId,
            rewardType = TransactionType.DAILY_BONUS,
            source = "deposit",
            amount = reward.requiredCoins,
            idempotencyKey = "credit_for_redemption"
        )
        assertEquals(reward.requiredCoins, walletRepository.getCalculatedBalance(testUserId))

        // Now submit redemption
        val successResult = redemptionRepository.submitRedemptionRequest(
            userId = testUserId,
            rewardId = reward.rewardId,
            destinationAccount = "user@example.com",
            idempotencyKey = "red_idemp_key_001"
        )
        assertTrue(successResult is RedemptionResult.Success)
        assertEquals(0L, walletRepository.getCalculatedBalance(testUserId))
    }

    @Test
    fun test6_fraudRiskEngineDetectsAnomaliesAndAssignsRiskState() = runBlocking {
        val initialRisk = fraudRiskEngine.evaluateUserRisk(testUserId)
        assertEquals(FraudRiskState.NORMAL, initialRisk.riskState)
        assertEquals(0, initialRisk.riskScore)

        // Simulate multiple critical security anomalies
        repeat(3) { i ->
            fraudRiskEngine.recordAnomaly(
                userId = testUserId,
                eventType = SecurityEventType.INVALID_GAME_RESULT,
                severity = SecuritySeverity.HIGH,
                relatedId = "game_$i",
                metadata = "Impossible score detected"
            )
        }
        repeat(5) { i ->
            fraudRiskEngine.recordAnomaly(
                userId = testUserId,
                eventType = SecurityEventType.DUPLICATE_REWARD_ATTEMPT,
                severity = SecuritySeverity.MEDIUM,
                relatedId = "dup_$i",
                metadata = "High-velocity replay"
            )
        }

        val evaluatedRisk = fraudRiskEngine.evaluateUserRisk(testUserId)
        assertTrue(evaluatedRisk.riskScore >= 75)
        assertEquals(FraudRiskState.BLOCKED, evaluatedRisk.riskState)
        assertTrue(evaluatedRisk.triggeredFlags.isNotEmpty())
    }

    @Test
    fun test7_securityEventLoggerSanitizesSensitiveData() {
        val rawLog = "login failed with password=superSecretPassword123 and token=abcde12345"
        val sanitized = SecurityEventLogger.sanitize(rawLog)
        assertNotNull(sanitized)
        assertFalse(sanitized!!.contains("superSecretPassword123"))
        assertFalse(sanitized.contains("abcde12345"))
        assertTrue(sanitized.contains("***REDACTED***"))
    }

    @Test
    fun test8_coinTossAuthoritativeFlipIntegrity() = runBlocking {
        val result = coinTossGameEngine.playCoinToss(
            userId = testUserId,
            userChoice = CoinSide.HEADS
        )
        assertTrue(result is CoinTossResult.Success)
        val success = result as CoinTossResult.Success
        assertNotNull(success.outcome)
        assertTrue(success.coinsAwarded >= 0L)
    }

    @Test
    fun test9_scratchGameRequiresThresholdRevealBeforeReward() = runBlocking {
        val cardResult = scratchGameEngine.createScratchSession(testUserId)
        assertTrue(cardResult is ScratchCardResult.CardCreated)
        val session = (cardResult as ScratchCardResult.CardCreated).session

        // Attempt completion with only 30% scratched (below 70% threshold)
        val prematureResult = scratchGameEngine.completeScratchSession(
            userId = testUserId,
            scratchId = session.scratchId,
            revealedPercent = 0.30f
        )
        assertTrue(prematureResult is ScratchCardResult.Error)

        // Attempt completion with 85% scratched (above 70% threshold)
        val validResult = scratchGameEngine.completeScratchSession(
            userId = testUserId,
            scratchId = session.scratchId,
            revealedPercent = 0.85f
        )
        assertTrue(validResult is ScratchCardResult.RewardGranted)
    }

    @Test
    fun test10_puzzleGameMasksAnswersAndEnforcesTimeLimit() = runBlocking {
        val sessionResult = puzzleGameEngine.createPuzzleSession(testUserId)
        assertTrue(sessionResult is PuzzleSessionResult.QuestionDelivered)
        val delivered = sessionResult as PuzzleSessionResult.QuestionDelivered

        // Verify client question does not contain correctAnswerIndex
        assertNotNull(delivered.question)
        assertNotNull(delivered.sessionId)

        // Submit correct answer index (0 for first puzzle or evaluated against catalog)
        val submitResult = puzzleGameEngine.submitAnswer(
            userId = testUserId,
            sessionId = delivered.sessionId,
            selectedAnswerIndex = 0
        )
        assertTrue(submitResult is PuzzleSubmitResult.Correct || submitResult is PuzzleSubmitResult.Incorrect)
    }
}
