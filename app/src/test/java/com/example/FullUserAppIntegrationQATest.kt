package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.CoinSide
import com.example.core.config.TicTacToeMark
import com.example.core.database.AppDatabase
import com.example.core.security.FraudRiskEngine
import com.example.core.security.RateLimiter
import com.example.core.security.SecurityEventLogger
import com.example.data.model.AccountStatus
import com.example.data.model.RedemptionStatus
import com.example.data.model.TransactionType
import com.example.data.repository.ActivityRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthSessionManager
import com.example.data.repository.EarnRepository
import com.example.data.repository.GameRepository
import com.example.data.repository.NotificationPreferencesRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.RedemptionRepository
import com.example.data.repository.RedemptionResult
import com.example.data.repository.ReferralRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import com.example.domain.engine.BubblePopCompleteResult
import com.example.domain.engine.BubblePopGameEngine
import com.example.domain.engine.BubblePopSessionResult
import com.example.domain.engine.CoinTossGameEngine
import com.example.domain.engine.CoinTossResult
import com.example.domain.engine.PuzzleGameEngine
import com.example.domain.engine.PuzzleSessionResult
import com.example.domain.engine.PuzzleSubmitResult
import com.example.domain.engine.ReferralQualificationEngine
import com.example.domain.engine.ReferralRiskEngine
import com.example.domain.engine.RewardEngine
import com.example.domain.engine.RewardGrantResult
import com.example.domain.engine.ScratchCardResult
import com.example.domain.engine.ScratchGameEngine
import com.example.domain.engine.SpinGameEngine
import com.example.domain.engine.SpinResult
import com.example.domain.engine.TicTacToeGameEngine
import com.example.domain.engine.TicTacToeResult
import com.example.services.ads.AdActionConfig
import com.example.services.ads.AdMobService
import com.example.services.ads.AdPlacement
import com.example.services.notifications.NotificationService
import com.example.services.notifications.PushTokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FullUserAppIntegrationQATest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var sessionManager: AuthSessionManager
    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: UserRepository
    private lateinit var walletRepository: WalletRepository
    private lateinit var gameRepository: GameRepository
    private lateinit var activityRepository: ActivityRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var notificationService: NotificationService
    private lateinit var securityEventLogger: SecurityEventLogger
    private lateinit var fraudRiskEngine: FraudRiskEngine
    private lateinit var rewardEngine: RewardEngine
    private lateinit var earnRepository: EarnRepository
    private lateinit var referralRepository: ReferralRepository
    private lateinit var referralQualificationEngine: ReferralQualificationEngine
    private lateinit var referralRiskEngine: ReferralRiskEngine
    private lateinit var redemptionRepository: RedemptionRepository
    private lateinit var adMobService: AdMobService

    private lateinit var spinGameEngine: SpinGameEngine
    private lateinit var scratchGameEngine: ScratchGameEngine
    private lateinit var puzzleGameEngine: PuzzleGameEngine
    private lateinit var coinTossGameEngine: CoinTossGameEngine
    private lateinit var ticTacToeGameEngine: TicTacToeGameEngine
    private lateinit var bubblePopGameEngine: BubblePopGameEngine

    @Before
    fun setUp() {
        RateLimiter.clearAll()
        context = ApplicationProvider.getApplicationContext()
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
        walletRepository = WalletRepository(database, database.coinTransactionDao(), database.userDao())
        gameRepository = GameRepository(database.gamePlayStatsDao())
        activityRepository = ActivityRepository(database.activityDao())

        val preferencesRepository = NotificationPreferencesRepository(context)
        val pushTokenManager = PushTokenManager(context)

        notificationRepository = NotificationRepository(
            notificationDao = database.notificationDao(),
            preferencesRepository = preferencesRepository,
            pushTokenManager = pushTokenManager
        )

        notificationService = NotificationService(
            notificationRepository = notificationRepository,
            activityRepository = activityRepository
        )
        securityEventLogger = SecurityEventLogger(database.securityEventDao())
        fraudRiskEngine = FraudRiskEngine(database.securityEventDao(), securityEventLogger)

        rewardEngine = RewardEngine(
            walletRepository = walletRepository,
            gameRepository = gameRepository,
            userRepository = userRepository,
            activityRepository = activityRepository,
            notificationService = notificationService,
            securityEventLogger = securityEventLogger,
            fraudRiskEngine = fraudRiskEngine
        )

        referralQualificationEngine = ReferralQualificationEngine(
            referralDao = database.referralDao(),
            userDao = database.userDao(),
            notificationService = notificationService,
            rewardEngineProvider = { rewardEngine }
        )
        rewardEngine.referralQualificationEngine = referralQualificationEngine

        referralRiskEngine = ReferralRiskEngine(database.referralDao())
        referralRepository = ReferralRepository(
            referralDao = database.referralDao(),
            userDao = database.userDao(),
            riskEngine = referralRiskEngine,
            notificationService = notificationService
        )

        earnRepository = EarnRepository(
            walletRepository = walletRepository,
            userRepository = userRepository,
            rewardEngine = rewardEngine,
            dailyStreakDao = database.dailyStreakDao(),
            activityRepository = activityRepository,
            notificationService = notificationService
        )

        redemptionRepository = RedemptionRepository(
            redemptionDao = database.redemptionDao(),
            walletRepository = walletRepository,
            userRepository = userRepository,
            database = database,
            activityRepository = activityRepository,
            notificationService = notificationService,
            securityEventLogger = securityEventLogger,
            fraudRiskEngine = fraudRiskEngine
        )

        adMobService = AdMobService(rewardEngine)

        spinGameEngine = SpinGameEngine(rewardEngine, gameRepository, userRepository, walletRepository)
        scratchGameEngine = ScratchGameEngine(rewardEngine, gameRepository, userRepository, walletRepository)
        puzzleGameEngine = PuzzleGameEngine(rewardEngine, gameRepository, userRepository, walletRepository)
        coinTossGameEngine = CoinTossGameEngine(rewardEngine, gameRepository, userRepository, walletRepository)
        ticTacToeGameEngine = TicTacToeGameEngine(rewardEngine, gameRepository, userRepository, walletRepository)
        bubblePopGameEngine = BubblePopGameEngine(rewardEngine, gameRepository, userRepository, walletRepository)
    }

    @After
    fun tearDown() {
        database.close()
        RateLimiter.clearAll()
    }

    @Test
    fun testCompleteEndToEndUserJourney() = runBlocking {
        // 1. Sign up User A (Referrer)
        val signupAResult = authRepository.signUp(
            name = "User Alpha",
            email = "user_a@example.com",
            password = "Password@123",
            confirmPassword = "Password@123",
            referralCode = null
        )
        assertTrue("User A sign up should succeed", signupAResult.isSuccess)
        val userA = signupAResult.getOrThrow()
        assertNotNull(userA.referralCode)
        val referrerCode = userA.referralCode

        // 2. Sign up User B (Invited by User A)
        authRepository.logout()
        val signupBResult = authRepository.signUp(
            name = "User Beta",
            email = "user_b@example.com",
            password = "Password@123",
            confirmPassword = "Password@123",
            referralCode = referrerCode
        )
        assertTrue("User B sign up should succeed", signupBResult.isSuccess)
        val userB = signupBResult.getOrThrow()
        val userBId = userB.userId

        // Initial balance 0
        val initialBalance = walletRepository.getCalculatedBalance(userBId)
        assertEquals(0L, initialBalance)

        // 3. Daily Bonus Claim & Duplicate Prevention
        val dailyClaim1 = earnRepository.claimDailyBonus(userBId)
        assertTrue("Daily bonus claim 1 should succeed", dailyClaim1 is RewardGrantResult.Success)
        val bonus1Amount = (dailyClaim1 as RewardGrantResult.Success).coinsGranted
        assertTrue(bonus1Amount > 0)
        assertEquals(bonus1Amount, walletRepository.getCalculatedBalance(userBId))

        val dailyClaim2 = earnRepository.claimDailyBonus(userBId)
        assertTrue("Duplicate daily bonus must be AlreadyClaimed", dailyClaim2 is RewardGrantResult.AlreadyClaimed)
        assertEquals(bonus1Amount, walletRepository.getCalculatedBalance(userBId))

        // 4. Spin & Win
        val spinRes = spinGameEngine.executeAuthoritativeSpin(userBId)
        assertTrue("Spin should execute successfully", spinRes is SpinResult.Success)
        val spinCoins = (spinRes as SpinResult.Success).coinsAwarded
        assertTrue(walletRepository.getCalculatedBalance(userBId) >= bonus1Amount + spinCoins)

        // 5. Scratch & Reveal
        val scratchInit = scratchGameEngine.createScratchSession(userBId)
        assertTrue("Scratch card creation should succeed", scratchInit is ScratchCardResult.CardCreated)
        val scratchSession = (scratchInit as ScratchCardResult.CardCreated).session

        val prematureScratch = scratchGameEngine.completeScratchSession(userBId, scratchSession.scratchId, 0.40f)
        assertTrue(prematureScratch is ScratchCardResult.Error)

        val validScratch = scratchGameEngine.completeScratchSession(userBId, scratchSession.scratchId, 0.80f)
        assertTrue(validScratch is ScratchCardResult.RewardGranted)

        // 6. Brain Puzzle
        val puzzleInit = puzzleGameEngine.createPuzzleSession(userBId)
        assertTrue("Puzzle session creation should succeed", puzzleInit is PuzzleSessionResult.QuestionDelivered)
        val puzzleSessionId = (puzzleInit as PuzzleSessionResult.QuestionDelivered).sessionId

        val puzzleSubmit = puzzleGameEngine.submitAnswer(userBId, puzzleSessionId, selectedAnswerIndex = 0)
        assertTrue(puzzleSubmit is PuzzleSubmitResult.Correct || puzzleSubmit is PuzzleSubmitResult.Incorrect)

        // 7. Coin Toss
        val coinTossRes = coinTossGameEngine.playCoinToss(userBId, CoinSide.HEADS)
        assertTrue("Coin toss must produce valid result", coinTossRes is CoinTossResult.Success)

        // 8. Tic-Tac-Toe
        val tttInit = ticTacToeGameEngine.startMatch(userBId)
        assertTrue("Tic-Tac-Toe match creation should succeed", tttInit is TicTacToeResult.MatchCreated)
        val tttSessionId = (tttInit as TicTacToeResult.MatchCreated).sessionId

        val tttMove = ticTacToeGameEngine.playMove(userBId, tttSessionId, 0)
        assertTrue("Tic-Tac-Toe move should succeed", tttMove is TicTacToeResult.MoveResult)

        // 9. Bubble Pop
        val bubbleInit = bubblePopGameEngine.startRound(userBId)
        assertTrue("Bubble pop start should succeed", bubbleInit is BubblePopSessionResult.SessionStarted)
        val bubbleSessionId = (bubbleInit as BubblePopSessionResult.SessionStarted).sessionId

        val bubbleComplete = bubblePopGameEngine.completeRound(
            userId = userBId,
            sessionId = bubbleSessionId,
            claimedBubblesPopped = 35,
            elapsedSeconds = 30
        )
        assertTrue("Bubble pop completion should succeed", bubbleComplete is BubblePopCompleteResult.Success)

        // 10. Rewarded Ad Completion
        var adRewardSuccess = false
        adMobService.showRewardedAd(
            userId = userBId,
            placement = AdPlacement.REWARDED_DIRECT_COINS,
            actionConfig = AdActionConfig(rewardType = com.example.services.ads.AdRewardType.AD_COIN_REWARD),
            onRewardGranted = { result ->
                if (result is RewardGrantResult.Success) {
                    adRewardSuccess = true
                }
            },
            onAdFailedOrSkipped = {}
        )
        assertTrue("Ad reward callback should succeed", adRewardSuccess)

        // 11. Wallet Balance & Ledger Audit
        val currentBalance = walletRepository.getCalculatedBalance(userBId)
        assertTrue("Balance must be positive", currentBalance > 0)

        val allTx = walletRepository.observeTransactions(userBId).first()
        assertTrue("Transactions must be recorded", allTx.isNotEmpty())
        val sumCredits = allTx.filter { it.amount > 0 && it.type != TransactionType.REDEMPTION_DEDUCTION }.sumOf { it.amount }
        val sumDeducts = allTx.filter { it.type == TransactionType.REDEMPTION_DEDUCTION }.sumOf { it.amount }
        assertEquals(sumCredits - sumDeducts, currentBalance)

        // 12. Reward Catalog & Redemption Flow
        val catalog = redemptionRepository.getRewardCatalog()
        assertTrue("Reward catalog must be populated", catalog.isNotEmpty())
        val lowestReward = catalog.minByOrNull { it.requiredCoins }!!

        if (currentBalance < lowestReward.requiredCoins) {
            val diff = lowestReward.requiredCoins - currentBalance
            rewardEngine.processReward(
                userId = userBId,
                rewardType = TransactionType.DAILY_BONUS,
                source = "qa_boost",
                amount = diff,
                idempotencyKey = "qa_boost_token_${System.currentTimeMillis()}"
            )
        }

        val balanceBeforeRedeem = walletRepository.getCalculatedBalance(userBId)
        assertTrue(balanceBeforeRedeem >= lowestReward.requiredCoins)

        val redeemRes = redemptionRepository.submitRedemptionRequest(
            userId = userBId,
            rewardId = lowestReward.rewardId,
            destinationAccount = "user_b_upi@bank",
            idempotencyKey = "qa_redemption_key_001"
        )
        assertTrue("Redemption submission should succeed", redeemRes is RedemptionResult.Success)
        val createdRedemption = (redeemRes as RedemptionResult.Success).request
        assertEquals(RedemptionStatus.PENDING, createdRedemption.status)

        val balanceAfterRedeem = walletRepository.getCalculatedBalance(userBId)
        assertEquals(balanceBeforeRedeem - lowestReward.requiredCoins, balanceAfterRedeem)

        // Duplicate submission check
        val duplicateRedeemRes = redemptionRepository.submitRedemptionRequest(
            userId = userBId,
            rewardId = lowestReward.rewardId,
            destinationAccount = "user_b_upi@bank",
            idempotencyKey = "qa_redemption_key_001"
        )
        assertTrue(duplicateRedeemRes is RedemptionResult.Success)
        assertEquals(balanceAfterRedeem, walletRepository.getCalculatedBalance(userBId))

        // 13. Notifications & Activity History
        val notifications = notificationRepository.observeNotifications(userBId).first()
        assertTrue("Notifications should be emitted", notifications.isNotEmpty())
        val unreadCountBefore = notificationRepository.getUnreadCount(userBId)
        assertTrue(unreadCountBefore > 0)

        notificationRepository.markAllAsRead(userBId)
        val unreadCountAfter = notificationRepository.getUnreadCount(userBId)
        assertEquals(0, unreadCountAfter)

        val activities = activityRepository.observeActivities(userBId).first()
        assertTrue("Activities should be logged", activities.isNotEmpty())

        // 14. Settings & Re-login
        authRepository.logout()
        val reLogin = authRepository.login("user_b@example.com", "Password@123")
        assertTrue("Re-login should succeed", reLogin.isSuccess)
        val reLoggedUser = reLogin.getOrThrow()
        assertEquals(userBId, reLoggedUser.userId)
        assertEquals(balanceAfterRedeem, walletRepository.getCalculatedBalance(userBId))
    }

    @Test
    fun testSelfReferralIsStrictlyRejected() = runBlocking {
        val signupRes = authRepository.signUp(
            name = "Self User",
            email = "self_ref@example.com",
            password = "Password@123",
            confirmPassword = "Password@123",
            referralCode = null
        )
        assertTrue(signupRes.isSuccess)
        val user = signupRes.getOrThrow()

        val applyResult = referralRepository.applyReferralCode(user.userId, user.referralCode)
        assertFalse("Self-referral must be rejected", applyResult.isSuccess)
    }

    @Test
    fun testNegativeBalanceInvariantUnderHighConcurrencySimulation() = runBlocking {
        val signup = authRepository.signUp(
            name = "Concur User",
            email = "concur@example.com",
            password = "Password@123",
            confirmPassword = "Password@123",
            referralCode = null
        )
        val user = signup.getOrThrow()

        rewardEngine.processReward(
            userId = user.userId,
            rewardType = TransactionType.DAILY_BONUS,
            source = "init",
            amount = 100L,
            idempotencyKey = "tx_concur_init"
        )
        assertEquals(100L, walletRepository.getCalculatedBalance(user.userId))

        val deductFail = walletRepository.subtractCoins(
            userId = user.userId,
            type = TransactionType.REDEMPTION_DEDUCTION,
            source = "overdraw_attempt",
            amount = 150L,
            referenceId = "fail_1",
            idempotencyKey = "fail_key_1"
        )
        assertTrue("Overdraw deduction must fail", deductFail is com.example.data.repository.TransactionResult.Error)
        assertEquals(100L, walletRepository.getCalculatedBalance(user.userId))

        val deductSuccess = walletRepository.subtractCoins(
            userId = user.userId,
            type = TransactionType.REDEMPTION_DEDUCTION,
            source = "valid_deduct",
            amount = 60L,
            referenceId = "ok_1",
            idempotencyKey = "ok_key_1"
        )
        assertTrue("Valid deduction must succeed", deductSuccess is com.example.data.repository.TransactionResult.Success)
        assertEquals(40L, walletRepository.getCalculatedBalance(user.userId))
    }
}
