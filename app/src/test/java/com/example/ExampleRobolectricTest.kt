package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.CoinConfig
import com.example.core.config.DailyBonusConfig
import com.example.core.config.PuzzleCategory
import com.example.core.config.PuzzleConfig
import com.example.core.config.PuzzleDifficulty
import com.example.core.config.ScratchGameConfig
import com.example.core.config.SpinGameConfig
import com.example.core.di.AppContainer
import com.example.data.model.TransactionType
import com.example.domain.engine.CoinConversionHelper
import com.example.domain.engine.PuzzleSessionResult
import com.example.domain.engine.PuzzleSubmitResult
import com.example.domain.engine.RewardGrantResult
import com.example.domain.engine.ScratchCardResult
import com.example.domain.engine.SpinResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PlayRewards", appName)
    }

    @Test
    fun `verify bottom navigation items are initialized and not null`() {
        val items = com.example.ui.navigation.Screen.bottomNavItems
        assertEquals(5, items.size)
        items.forEach { screen ->
            assertNotNull(screen)
            assertTrue(screen.route.isNotBlank())
            assertTrue(screen.title.isNotBlank())
        }
    }

    @Test
    fun `verify coin to rupee conversion rate is exactly 700 coins per 1 rupee`() {
        assertEquals(700.0, CoinConfig.coinsPerCurrencyUnit, 0.001)
        assertEquals(1.0, CoinConfig.coinsToCurrencyValue(700L), 0.001)
        assertEquals(10.0, CoinConfig.coinsToCurrencyValue(7000L), 0.001)
        assertEquals("₹1.00", CoinConversionHelper.getCurrencyEstimate(700L))
        assertEquals("₹10.00", CoinConversionHelper.getCurrencyEstimate(7000L))
        assertEquals("₹1.79", CoinConversionHelper.getCurrencyEstimate(1250L))
    }

    @Test
    fun `verify time greeting produces valid output for all 24 hours`() {
        val morningCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 8) }
        val afternoonCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 14) }
        val eveningCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 19) }
        val nightCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 23) }

        val homeVmGreeting = { cal: Calendar ->
            when (cal.get(Calendar.HOUR_OF_DAY)) {
                in 5..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                in 17..21 -> "Good evening"
                else -> "Good night"
            }
        }

        assertEquals("Good morning", homeVmGreeting(morningCal))
        assertEquals("Good afternoon", homeVmGreeting(afternoonCal))
        assertEquals("Good evening", homeVmGreeting(eveningCal))
        assertEquals("Good night", homeVmGreeting(nightCal))
    }

    @Test
    fun `authoritative daily bonus grants configured coins and prevents duplicate claim`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AppContainer(context)

        val testEmail = "testuser_${System.currentTimeMillis()}@example.com"
        val authResult = container.authRepository.signUp(
            name = "Parth",
            email = testEmail,
            password = "Password123!",
            confirmPassword = "Password123!"
        )
        assertTrue(authResult.isSuccess)
        val user = authResult.getOrThrow()

        val initialBalance = container.walletRepository.getCalculatedBalance(user.userId)

        // First Claim
        val firstClaimResult = container.earnRepository.claimDailyBonus(user.userId)
        assertTrue(firstClaimResult is RewardGrantResult.Success)
        val success = firstClaimResult as RewardGrantResult.Success
        assertEquals(DailyBonusConfig.BONUS_AMOUNT_COINS, success.coinsGranted)
        assertEquals(initialBalance + DailyBonusConfig.BONUS_AMOUNT_COINS, success.newBalance)

        // Persistent check
        val isClaimedToday = container.earnRepository.isDailyBonusClaimedToday(user.userId)
        assertTrue(isClaimedToday)

        // Duplicate Claim Attempt (idempotency + daily validation protection)
        val secondClaimResult = container.earnRepository.claimDailyBonus(user.userId)
        assertTrue(secondClaimResult is RewardGrantResult.AlreadyClaimed)

        // Balance should not have increased twice
        val finalBalance = container.walletRepository.getCalculatedBalance(user.userId)
        assertEquals(initialBalance + DailyBonusConfig.BONUS_AMOUNT_COINS, finalBalance)
    }

    @Test
    fun `verify spin game centralized configuration has valid segments and 5 daily limit`() {
        assertEquals(5, SpinGameConfig.dailySpinLimit)
        assertTrue(SpinGameConfig.spinGameEnabled)
        val segments = SpinGameConfig.getActiveSegments()
        assertEquals(8, segments.size)
        assertTrue(segments.all { it.amount > 0 })
        assertTrue(segments.all { it.weight > 0 })
        assertTrue(segments.all { it.label.isNotBlank() })
    }

    @Test
    fun `authoritative spin execution respects limits grants coins and logs SPIN_REWARD transaction`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AppContainer(context)

        val testEmail = "spinuser_${System.currentTimeMillis()}@example.com"
        val authResult = container.authRepository.signUp(
            name = "SpinPlayer",
            email = testEmail,
            password = "Password123!",
            confirmPassword = "Password123!"
        )
        assertTrue(authResult.isSuccess)
        val user = authResult.getOrThrow()

        val initialBalance = container.walletRepository.getCalculatedBalance(user.userId)
        val initialStats = container.spinGameEngine.getDailySpinStats(user.userId)
        assertEquals(5, initialStats.spinsRemainingToday)
        assertEquals(0, initialStats.spinsUsedToday)

        // Execute Spin #1
        val spinResult = container.spinGameEngine.executeAuthoritativeSpin(user.userId)
        assertTrue(spinResult is SpinResult.Success)
        val success = spinResult as SpinResult.Success

        // Verify result data
        assertTrue(success.coinsAwarded > 0)
        assertTrue(success.segmentIndex in 0..7)
        assertEquals(initialBalance + success.coinsAwarded, success.newBalance)
        assertEquals(1, success.spinsUsedToday)
        assertEquals(4, success.spinsRemainingToday)

        // Check wallet transaction ledger
        val transactions = container.walletRepository.getRecentTransactions(user.userId, 10)
        val spinTx = transactions.firstOrNull { it.type == TransactionType.SPIN_REWARD }
        assertNotNull(spinTx)
        assertEquals(success.coinsAwarded, spinTx?.amount)
        assertEquals("game_spin_win", spinTx?.source)
    }

    @Test
    fun `sixth spin is rejected when daily limit of five is reached`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AppContainer(context)

        val testEmail = "limituser_${System.currentTimeMillis()}@example.com"
        val authResult = container.authRepository.signUp(
            name = "LimitPlayer",
            email = testEmail,
            password = "Password123!",
            confirmPassword = "Password123!"
        )
        assertTrue(authResult.isSuccess)
        val user = authResult.getOrThrow()

        // Exhaust all 5 free spins
        for (i in 1..5) {
            val res = container.spinGameEngine.executeAuthoritativeSpin(user.userId)
            assertTrue(res is SpinResult.Success)
        }

        // 6th spin should be rejected
        val sixthSpin = container.spinGameEngine.executeAuthoritativeSpin(user.userId)
        assertTrue(sixthSpin is SpinResult.LimitReached)
        val limitReached = sixthSpin as SpinResult.LimitReached
        assertEquals(5, limitReached.spinsUsedToday)
        assertEquals(5, limitReached.limit)
    }

    @Test
    fun `idempotent repeated spin request prevents duplicate coin credits`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AppContainer(context)

        val testEmail = "idempotentuser_${System.currentTimeMillis()}@example.com"
        val authResult = container.authRepository.signUp(
            name = "IdempotentPlayer",
            email = testEmail,
            password = "Password123!",
            confirmPassword = "Password123!"
        )
        assertTrue(authResult.isSuccess)
        val user = authResult.getOrThrow()

        val fixedSpinId = "fixed_spin_token_12345"

        // First attempt with fixed spin ID
        val firstResult = container.spinGameEngine.executeAuthoritativeSpin(user.userId, customSpinId = fixedSpinId)
        assertTrue(firstResult is SpinResult.Success)
        val firstSuccess = firstResult as SpinResult.Success

        val balanceAfterFirst = container.walletRepository.getCalculatedBalance(user.userId)

        // Duplicate attempt with same spin ID
        val duplicateResult = container.spinGameEngine.executeAuthoritativeSpin(user.userId, customSpinId = fixedSpinId)
        assertTrue(duplicateResult is SpinResult.Success)

        // Balance should NOT increase again
        val finalBalance = container.walletRepository.getCalculatedBalance(user.userId)
        assertEquals(balanceAfterFirst, finalBalance)
    }

    @Test
    fun `different users have independent daily spin limits`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AppContainer(context)

        val email1 = "user1_${System.currentTimeMillis()}@example.com"
        val email2 = "user2_${System.currentTimeMillis()}@example.com"

        val user1Result = container.authRepository.signUp(
            name = "UserOne",
            email = email1,
            password = "Password123!",
            confirmPassword = "Password123!"
        )
        val user2Result = container.authRepository.signUp(
            name = "UserTwo",
            email = email2,
            password = "Password123!",
            confirmPassword = "Password123!"
        )
        val user1 = user1Result.getOrThrow()
        val user2 = user2Result.getOrThrow()

        // User 1 logs in and uses 3 spins
        container.authRepository.login(email1, "Password123!")
        for (i in 1..3) {
            val res = container.spinGameEngine.executeAuthoritativeSpin(user1.userId)
            assertTrue(res is SpinResult.Success)
        }

        val user1Stats = container.spinGameEngine.getDailySpinStats(user1.userId)
        assertEquals(3, user1Stats.spinsUsedToday)
        assertEquals(2, user1Stats.spinsRemainingToday)

        // User 2 logs in and still has all 5 spins
        container.authRepository.login(email2, "Password123!")
        val user2Stats = container.spinGameEngine.getDailySpinStats(user2.userId)
        assertEquals(0, user2Stats.spinsUsedToday)
        assertEquals(5, user2Stats.spinsRemainingToday)
    }

    @Test
    fun `verify scratch game centralized configuration has valid tiers and daily limit`() {
        assertEquals(5, ScratchGameConfig.dailyScratchLimit)
        assertTrue(ScratchGameConfig.scratchGameEnabled)
        assertEquals(0.70f, ScratchGameConfig.revealThresholdPercent, 0.01f)
        val tiers = ScratchGameConfig.getActiveTiers()
        assertEquals(8, tiers.size)
        assertTrue(tiers.all { it.rewardAmount > 0 })
        assertTrue(tiers.all { it.weight > 0 })
        assertTrue(tiers.all { it.label.isNotBlank() })
    }

    @Test
    fun `authoritative scratch card lifecycle creates session enforces threshold and credits SCRATCH_REWARD`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AppContainer(context)

        val testEmail = "scratchuser_${System.currentTimeMillis()}@example.com"
        val authResult = container.authRepository.signUp(
            name = "ScratchPlayer",
            email = testEmail,
            password = "Password123!",
            confirmPassword = "Password123!"
        )
        assertTrue(authResult.isSuccess)
        val user = authResult.getOrThrow()

        val initialBalance = container.walletRepository.getCalculatedBalance(user.userId)
        val initialStats = container.scratchGameEngine.getDailyScratchStats(user.userId)
        assertEquals(5, initialStats.scratchesRemainingToday)
        assertEquals(0, initialStats.scratchesUsedToday)

        // 1. Create Scratch Session (Authoritative server selection)
        val createResult = container.scratchGameEngine.createScratchSession(user.userId)
        assertTrue(createResult is ScratchCardResult.CardCreated)
        val card = createResult as ScratchCardResult.CardCreated
        assertTrue(card.tier.rewardAmount > 0)
        assertNotNull(card.session.scratchId)

        // 2. Attempt complete with insufficient scratch threshold (e.g. 50% < 70%)
        val failResult = container.scratchGameEngine.completeScratchSession(
            userId = user.userId,
            scratchId = card.session.scratchId,
            revealedPercent = 0.50f
        )
        assertTrue(failResult is ScratchCardResult.Error)

        // 3. Complete with valid threshold (100%)
        val completeResult = container.scratchGameEngine.completeScratchSession(
            userId = user.userId,
            scratchId = card.session.scratchId,
            revealedPercent = 1.0f
        )
        assertTrue(completeResult is ScratchCardResult.RewardGranted)
        val reward = completeResult as ScratchCardResult.RewardGranted
        assertEquals(card.tier.rewardAmount, reward.coinsAwarded)
        assertEquals(initialBalance + card.tier.rewardAmount, reward.newBalance)
        assertEquals(1, reward.scratchesUsedToday)
        assertEquals(4, reward.scratchesRemainingToday)

        // 4. Verify transaction in immutable ledger
        val transactions = container.walletRepository.getRecentTransactions(user.userId, 10)
        val scratchTx = transactions.firstOrNull { it.type == TransactionType.SCRATCH_REWARD }
        assertNotNull(scratchTx)
        assertEquals(card.tier.rewardAmount, scratchTx?.amount)
        assertEquals("game_scratch_card", scratchTx?.source)
    }

    @Test
    fun `scratch game enforces daily limit of 5 cards per day`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AppContainer(context)

        val testEmail = "scratchlimit_${System.currentTimeMillis()}@example.com"
        val authResult = container.authRepository.signUp(
            name = "ScratchLimitPlayer",
            email = testEmail,
            password = "Password123!",
            confirmPassword = "Password123!"
        )
        assertTrue(authResult.isSuccess)
        val user = authResult.getOrThrow()

        // Exhaust 5 cards
        for (i in 1..5) {
            val cardRes = container.scratchGameEngine.createScratchSession(user.userId)
            assertTrue(cardRes is ScratchCardResult.CardCreated)
            val card = cardRes as ScratchCardResult.CardCreated
            val completeRes = container.scratchGameEngine.completeScratchSession(
                userId = user.userId,
                scratchId = card.session.scratchId,
                revealedPercent = 1.0f
            )
            assertTrue(completeRes is ScratchCardResult.RewardGranted)
        }

        // 6th card request should be rejected by server
        val sixthCardRes = container.scratchGameEngine.createScratchSession(user.userId)
        assertTrue(sixthCardRes is ScratchCardResult.LimitReached)
        val limit = sixthCardRes as ScratchCardResult.LimitReached
        assertEquals(5, limit.scratchesUsedToday)
        assertEquals(5, limit.limit)
    }

    @Test
    fun `verify puzzle centralized configuration has valid catalog and daily limit`() {
        assertEquals(5, PuzzleConfig.dailyPuzzleLimit)
        assertTrue(PuzzleConfig.puzzleGameEnabled)
        assertEquals(30, PuzzleConfig.defaultTimeLimitSeconds)
        val activePuzzles = PuzzleConfig.getActivePuzzles()
        assertTrue(activePuzzles.size >= 8)
        assertTrue(activePuzzles.all { it.question.isNotBlank() })
        assertTrue(activePuzzles.all { it.options.size >= 2 })
        assertTrue(activePuzzles.all { it.correctAnswerIndex in it.options.indices })
        assertTrue(activePuzzles.all { it.rewardAmount > 0 })
    }

    @Test
    fun `authoritative puzzle session lifecycle masks answer and credits PUZZLE_REWARD on correct answer`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AppContainer(context)

        val testEmail = "puzzleplayer_${System.currentTimeMillis()}@example.com"
        val authResult = container.authRepository.signUp(
            name = "PuzzlePlayer",
            email = testEmail,
            password = "Password123!",
            confirmPassword = "Password123!"
        )
        assertTrue(authResult.isSuccess)
        val user = authResult.getOrThrow()

        val initialBalance = container.walletRepository.getCalculatedBalance(user.userId)
        val initialStats = container.puzzleGameEngine.getDailyPuzzleStats(user.userId)
        assertEquals(5, initialStats.puzzlesRemainingToday)
        assertEquals(0, initialStats.puzzlesCompletedToday)

        // 1. Create Session
        val createResult = container.puzzleGameEngine.createPuzzleSession(user.userId)
        assertTrue(createResult is PuzzleSessionResult.QuestionDelivered)
        val delivery = createResult as PuzzleSessionResult.QuestionDelivered

        assertNotNull(delivery.sessionId)
        assertTrue(delivery.question.options.isNotEmpty())
        assertTrue(delivery.question.rewardAmount > 0)

        // Find actual correct answer from authoritative catalog
        val actualDef = PuzzleConfig.puzzleCatalog.first { it.puzzleId == delivery.question.puzzleId }

        // 2. Submit Correct Answer
        val submitResult = container.puzzleGameEngine.submitAnswer(
            userId = user.userId,
            sessionId = delivery.sessionId,
            selectedAnswerIndex = actualDef.correctAnswerIndex
        )
        assertTrue(submitResult is PuzzleSubmitResult.Correct)
        val correctRes = submitResult as PuzzleSubmitResult.Correct
        assertEquals(actualDef.rewardAmount, correctRes.coinsAwarded)
        assertEquals(initialBalance + actualDef.rewardAmount, correctRes.newBalance)
        assertEquals(1, correctRes.puzzlesCompletedToday)
        assertEquals(4, correctRes.puzzlesRemainingToday)

        // 3. Verify ledger transaction
        val txs = container.walletRepository.getRecentTransactions(user.userId, 10)
        val puzzleTx = txs.firstOrNull { it.type == TransactionType.PUZZLE_REWARD }
        assertNotNull(puzzleTx)
        assertEquals(actualDef.rewardAmount, puzzleTx?.amount)
        assertEquals("game_puzzle", puzzleTx?.source)

        // 4. Duplicate submission protection
        val dupSubmit = container.puzzleGameEngine.submitAnswer(
            userId = user.userId,
            sessionId = delivery.sessionId,
            selectedAnswerIndex = actualDef.correctAnswerIndex
        )
        assertTrue(dupSubmit is PuzzleSubmitResult.AlreadySubmitted)
    }

    @Test
    fun `puzzle engine awards zero coins for incorrect answer and records attempt`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AppContainer(context)

        val testEmail = "puzzlewrong_${System.currentTimeMillis()}@example.com"
        val authResult = container.authRepository.signUp(
            name = "PuzzleWrongPlayer",
            email = testEmail,
            password = "Password123!",
            confirmPassword = "Password123!"
        )
        assertTrue(authResult.isSuccess)
        val user = authResult.getOrThrow()

        val initialBalance = container.walletRepository.getCalculatedBalance(user.userId)

        // 1. Create Session
        val createResult = container.puzzleGameEngine.createPuzzleSession(user.userId)
        assertTrue(createResult is PuzzleSessionResult.QuestionDelivered)
        val delivery = createResult as PuzzleSessionResult.QuestionDelivered
        val actualDef = PuzzleConfig.puzzleCatalog.first { it.puzzleId == delivery.question.puzzleId }

        // Submit wrong answer
        val wrongIndex = if (actualDef.correctAnswerIndex == 0) 1 else 0
        val submitResult = container.puzzleGameEngine.submitAnswer(
            userId = user.userId,
            sessionId = delivery.sessionId,
            selectedAnswerIndex = wrongIndex
        )

        assertTrue(submitResult is PuzzleSubmitResult.Incorrect)
        val incorrectRes = submitResult as PuzzleSubmitResult.Incorrect
        assertEquals(actualDef.correctAnswerIndex, incorrectRes.correctAnswerIndex)
        assertEquals(1, incorrectRes.puzzlesCompletedToday)
        assertEquals(4, incorrectRes.puzzlesRemainingToday)

        // Balance should NOT increase
        val balanceAfter = container.walletRepository.getCalculatedBalance(user.userId)
        assertEquals(initialBalance, balanceAfter)
    }

    @Test
    fun `puzzle engine enforces daily limit of 5 puzzles per day`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AppContainer(context)

        val testEmail = "puzzlelimit_${System.currentTimeMillis()}@example.com"
        val authResult = container.authRepository.signUp(
            name = "PuzzleLimitPlayer",
            email = testEmail,
            password = "Password123!",
            confirmPassword = "Password123!"
        )
        assertTrue(authResult.isSuccess)
        val user = authResult.getOrThrow()

        // Complete 5 puzzles
        for (i in 1..5) {
            val createRes = container.puzzleGameEngine.createPuzzleSession(user.userId)
            assertTrue(createRes is PuzzleSessionResult.QuestionDelivered)
            val delivery = createRes as PuzzleSessionResult.QuestionDelivered
            val actualDef = PuzzleConfig.puzzleCatalog.first { it.puzzleId == delivery.question.puzzleId }

            val subRes = container.puzzleGameEngine.submitAnswer(
                userId = user.userId,
                sessionId = delivery.sessionId,
                selectedAnswerIndex = actualDef.correctAnswerIndex
            )
            assertTrue(subRes is PuzzleSubmitResult.Correct)
        }

        // 6th puzzle session creation must be blocked
        val sixthCreate = container.puzzleGameEngine.createPuzzleSession(user.userId)
        assertTrue(sixthCreate is PuzzleSessionResult.LimitReached)
        val limit = sixthCreate as PuzzleSessionResult.LimitReached
        assertEquals(5, limit.puzzlesCompletedToday)
        assertEquals(5, limit.limit)
    }
}
