package com.example.redemption

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppDatabase
import com.example.data.model.AccountStatus
import com.example.data.model.RedemptionEligibilityResult
import com.example.data.model.RedemptionStatus
import com.example.data.model.RewardCategory
import com.example.data.model.TransactionType
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthSessionManager
import com.example.data.repository.RedemptionRepository
import com.example.data.repository.RedemptionResult
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RedemptionSystemTest {

    private lateinit var database: AppDatabase
    private lateinit var sessionManager: AuthSessionManager
    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: UserRepository
    private lateinit var walletRepository: WalletRepository
    private lateinit var redemptionRepository: RedemptionRepository

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
        redemptionRepository = RedemptionRepository(
            redemptionDao = database.redemptionDao(),
            walletRepository = walletRepository,
            userRepository = userRepository,
            database = database
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testRewardCatalogLoadingAndFiltering() = runTest {
        val catalog = redemptionRepository.getRewardCatalog()
        assertTrue("Catalog should contain default rewards", catalog.isNotEmpty())

        val giftCards = redemptionRepository.getRewardsByCategory(RewardCategory.GIFT_CARDS)
        assertTrue(giftCards.all { it.category == RewardCategory.GIFT_CARDS })

        val upiRewards = redemptionRepository.getRewardsByCategory(RewardCategory.UPI_REWARDS)
        assertTrue(upiRewards.all { it.category == RewardCategory.UPI_REWARDS })
    }

    @Test
    fun testEligibilityCheckFailsWhenInsufficientBalance() = runTest {
        val user = authRepository.signUp("User1", "user1@mail.com", "Pass123", "Pass123").getOrThrow()

        // Balance is 0, required is 7,000 coins for ₹10
        val eligibility = redemptionRepository.checkRewardEligibility(user.userId, "rew_inr_10_play")
        assertTrue(eligibility is RedemptionEligibilityResult.Ineligible)
        val inelig = eligibility as RedemptionEligibilityResult.Ineligible
        assertEquals(7000L, inelig.coinsNeeded)
    }

    @Test
    fun testEligibilityCheckPassesWhenBalanceSufficient() = runTest {
        val user = authRepository.signUp("User2", "user2@mail.com", "Pass123", "Pass123").getOrThrow()

        // Credit 10,000 coins
        walletRepository.addCoins(user.userId, TransactionType.GAME_REWARD, "bonus", 10000L, idempotencyKey = "k_credit_1")

        val eligibility = redemptionRepository.checkRewardEligibility(user.userId, "rew_inr_10_play")
        assertTrue(eligibility is RedemptionEligibilityResult.Eligible)
    }

    @Test
    fun testRedemptionDeductsCoinsAndCreatesRequestWithSnapshot() = runTest {
        val user = authRepository.signUp("User3", "user3@mail.com", "Pass123", "Pass123").getOrThrow()

        // Credit 15,000 coins
        walletRepository.addCoins(user.userId, TransactionType.GAME_REWARD, "bonus", 15000L, idempotencyKey = "k_credit_2")

        val result = redemptionRepository.submitRedemptionRequest(
            userId = user.userId,
            rewardId = "rew_inr_10_amazon",
            destinationAccount = "user3@mail.com"
        )

        assertTrue(result is RedemptionResult.Success)
        val success = result as RedemptionResult.Success
        assertEquals(8000L, success.remainingBalance) // 15000 - 7000 = 8000
        assertEquals(RedemptionStatus.PENDING, success.request.status)
        assertEquals("Amazon Pay ₹10 Voucher", success.request.rewardNameSnapshot)
        assertEquals(10.0, success.request.rewardValueSnapshot, 0.001)
        assertEquals(7000L, success.request.requiredCoinsSnapshot)
        assertEquals("user3@mail.com", success.request.destinationAccount)

        // Verify in database
        val requests = redemptionRepository.observeUserRequests(user.userId).first()
        assertEquals(1, requests.size)
        assertEquals(success.request.redemptionId, requests[0].redemptionId)
    }

    @Test
    fun testIdempotentRedemptionSubmissionPreventsDuplicateDeduction() = runTest {
        val user = authRepository.signUp("User4", "user4@mail.com", "Pass123", "Pass123").getOrThrow()

        walletRepository.addCoins(user.userId, TransactionType.GAME_REWARD, "bonus", 20000L, idempotencyKey = "k_credit_3")

        val idempKey = "UNIQUE_REDEMPTION_INTENT_999"

        val firstSubmit = redemptionRepository.submitRedemptionRequest(
            userId = user.userId,
            rewardId = "rew_inr_10_play",
            destinationAccount = "user4@mail.com",
            idempotencyKey = idempKey
        )
        assertTrue(firstSubmit is RedemptionResult.Success)
        assertEquals(13000L, (firstSubmit as RedemptionResult.Success).remainingBalance)

        // Resubmit with exact same idempotency key
        val secondSubmit = redemptionRepository.submitRedemptionRequest(
            userId = user.userId,
            rewardId = "rew_inr_10_play",
            destinationAccount = "user4@mail.com",
            idempotencyKey = idempKey
        )
        assertTrue(secondSubmit is RedemptionResult.Success)

        // Wallet balance must NOT be deducted twice (still 13000)
        assertEquals(13000L, walletRepository.getCalculatedBalance(user.userId))
    }

    @Test
    fun testRefundCreatesReversalAndRestoresWalletBalance() = runTest {
        val user = authRepository.signUp("User5", "user5@mail.com", "Pass123", "Pass123").getOrThrow()

        walletRepository.addCoins(user.userId, TransactionType.GAME_REWARD, "bonus", 10000L, idempotencyKey = "k_credit_4")

        val submitResult = redemptionRepository.submitRedemptionRequest(
            userId = user.userId,
            rewardId = "rew_inr_10_play",
            destinationAccount = "user5@mail.com"
        )
        val req = (submitResult as RedemptionResult.Success).request
        assertEquals(3000L, walletRepository.getCalculatedBalance(user.userId))

        // Execute admin refund
        val refundResult = redemptionRepository.refundRedemption(
            redemptionId = req.redemptionId,
            reason = "Invalid promo code delivery / stock issue",
            adminIdentifier = "ADMIN_SUPPORT_01"
        )

        assertTrue(refundResult.isSuccess)
        val refundedReq = refundResult.getOrThrow()
        assertEquals(RedemptionStatus.REFUNDED, refundedReq.status)

        // Wallet balance must be restored back to 10,000
        assertEquals(10000L, walletRepository.getCalculatedBalance(user.userId))

        // Verify ledger has REVERSAL transaction
        val transactions = database.coinTransactionDao().observeTransactions(user.userId).first()
        val reversalTx = transactions.find { it.type == TransactionType.REVERSAL }
        assertNotNull(reversalTx)
        assertEquals(7000L, reversalTx?.amount)
    }
}
