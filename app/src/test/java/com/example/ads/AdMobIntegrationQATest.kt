package com.example.ads

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppDatabase
import com.example.core.security.FraudRiskEngine
import com.example.core.security.RateLimiter
import com.example.core.security.SecurityEventLogger
import com.example.data.repository.ActivityRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthSessionManager
import com.example.data.repository.EarnRepository
import com.example.data.repository.GameRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import com.example.domain.engine.RewardEngine
import com.example.domain.engine.RewardGrantResult
import com.example.services.ads.AdActionConfig
import com.example.services.ads.AdAnalytics
import com.example.services.ads.AdConsentManager
import com.example.services.ads.AdConsentStatus
import com.example.services.ads.AdEligibilityResult
import com.example.services.ads.AdEnvironment
import com.example.services.ads.AdMobConfig
import com.example.services.ads.AdMobService
import com.example.services.ads.AdPlacement
import com.example.services.ads.AdRewardType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AdMobIntegrationQATest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var sessionManager: AuthSessionManager
    private lateinit var authRepository: AuthRepository
    private lateinit var walletRepository: WalletRepository
    private lateinit var gameRepository: GameRepository
    private lateinit var userRepository: UserRepository
    private lateinit var activityRepository: ActivityRepository
    private lateinit var rewardEngine: RewardEngine
    private lateinit var adMobService: AdMobService
    private lateinit var adConsentManager: AdConsentManager

    private var testUserId1: String = ""
    private var testUserId2: String = ""

    @Before
    fun setUp() = runBlocking {
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

        val securityEventLogger = SecurityEventLogger(database.securityEventDao())
        val fraudRiskEngine = FraudRiskEngine(database.securityEventDao(), securityEventLogger)

        rewardEngine = RewardEngine(
            walletRepository = walletRepository,
            gameRepository = gameRepository,
            userRepository = userRepository,
            activityRepository = activityRepository,
            securityEventLogger = securityEventLogger,
            fraudRiskEngine = fraudRiskEngine
        )

        adMobService = AdMobService(rewardEngine)
        adConsentManager = AdConsentManager(context)

        // Seed active test users via auth repository
        val user1 = authRepository.signUp(
            name = "Ad Tester One",
            email = "adtest1@playrewards.com",
            password = "Password@123",
            confirmPassword = "Password@123",
            referralCode = null
        ).getOrThrow()
        testUserId1 = user1.userId

        authRepository.logout()

        val user2 = authRepository.signUp(
            name = "Ad Tester Two",
            email = "adtest2@playrewards.com",
            password = "Password@123",
            confirmPassword = "Password@123",
            referralCode = null
        ).getOrThrow()
        testUserId2 = user2.userId

        authRepository.login("adtest1@playrewards.com", "Password@123")

        AdMobConfig.environment = AdEnvironment.DEVELOPMENT_TEST
        adMobService.resetTrackingForTesting()
        AdAnalytics.clearLogs()
    }

    @After
    fun tearDown() {
        database.close()
        adMobService.resetTrackingForTesting()
    }

    @Test
    fun testAdMobConfiguration_SampleTestIdsUsedInDev() {
        assertEquals(AdEnvironment.DEVELOPMENT_TEST, AdMobConfig.environment)
        assertEquals(AdMobConfig.GOOGLE_TEST_APP_ID, AdMobConfig.getAppId())
        assertEquals(AdMobConfig.GOOGLE_TEST_BANNER_ID, AdMobConfig.getBannerAdUnitId())
        assertEquals(AdMobConfig.GOOGLE_TEST_INTERSTITIAL_ID, AdMobConfig.getInterstitialAdUnitId())
        assertEquals(AdMobConfig.GOOGLE_TEST_REWARDED_ID, AdMobConfig.getRewardedAdUnitId())
    }

    @Test
    fun testAdMobConfiguration_ProductionSwitchWithoutCrashing() {
        AdMobConfig.environment = AdEnvironment.PRODUCTION
        AdMobConfig.productionAppId = "ca-app-pub-9999999999999999~1111111111"
        AdMobConfig.productionBannerId = "ca-app-pub-9999999999999999/2222222222"
        AdMobConfig.productionInterstitialId = "ca-app-pub-9999999999999999/3333333333"
        AdMobConfig.productionRewardedId = "ca-app-pub-9999999999999999/4444444444"

        assertEquals("ca-app-pub-9999999999999999~1111111111", AdMobConfig.getAppId())
        assertEquals("ca-app-pub-9999999999999999/2222222222", AdMobConfig.getBannerAdUnitId())
        assertEquals("ca-app-pub-9999999999999999/3333333333", AdMobConfig.getInterstitialAdUnitId())
        assertEquals("ca-app-pub-9999999999999999/4444444444", AdMobConfig.getRewardedAdUnitId())

        val missing = AdMobConfig.getMissingProductionConfigs()
        assertTrue(missing.isEmpty())

        // Revert to Dev
        AdMobConfig.environment = AdEnvironment.DEVELOPMENT_TEST
    }

    @Test
    fun testAdMobInitialization_SafeAndLogged() {
        adMobService.initialize(context)
        assertTrue(adMobService.isInitialized.value)
        val logs = AdAnalytics.getRecentEventLogs()
        assertTrue(logs.any { it.contains("ad_requested") })
    }

    @Test
    fun testInterstitial_CooldownAndFrequencyControl() {
        AdMobConfig.isInterstitialEnabled = true
        AdMobConfig.interstitialCooldownSeconds = 10L // 10 seconds for test
        AdMobConfig.maxInterstitialsPerSession = 2

        assertTrue("First interstitial should be eligible", adMobService.canShowInterstitial())

        var dismissed = false
        adMobService.showInterstitial(AdPlacement.INTERSTITIAL_GAME_FINISH) {
            dismissed = true
        }
        assertTrue(dismissed)

        // Immediately checking again should fail due to cooldown
        assertFalse("Second immediate interstitial must be blocked by cooldown", adMobService.canShowInterstitial())
    }

    @Test
    fun testRewardedAd_ValidCompletionGrantsCoinsThroughRewardEngine() = runTest {
        val config = AdActionConfig(
            rewardType = AdRewardType.AD_COIN_REWARD,
            rewardAmount = 25L,
            dailyLimit = 10,
            cooldownSeconds = 0L
        )

        val initialBalance = walletRepository.getCalculatedBalance(testUserId1)
        assertEquals(0L, initialBalance)

        var granted = false
        adMobService.showRewardedAd(
            userId = testUserId1,
            placement = AdPlacement.REWARDED_DIRECT_COINS,
            actionConfig = config,
            onRewardGranted = { result ->
                if (result is RewardGrantResult.Success) {
                    granted = true
                    assertEquals(25L, result.coinsGranted)
                }
            },
            onAdFailedOrSkipped = { /* No-op */ }
        )

        assertTrue("Reward must be granted upon completion", granted)
        val newBalance = walletRepository.getCalculatedBalance(testUserId1)
        assertEquals(25L, newBalance)
    }

    @Test
    fun testRewardedAd_DailyLimitAuthoritativeEnforcement() = runTest {
        val config = AdActionConfig(
            rewardType = AdRewardType.AD_COIN_REWARD,
            rewardAmount = 10L,
            dailyLimit = 3,
            cooldownSeconds = 0L
        )

        // Claim 3 times successfully
        for (i in 1..3) {
            val eligibility = adMobService.checkRewardedAdEligibility(testUserId1, config)
            assertEquals("Claim $i must be eligible", AdEligibilityResult.Eligible, eligibility)

            adMobService.showRewardedAd(
                userId = testUserId1,
                placement = AdPlacement.REWARDED_DIRECT_COINS,
                actionConfig = config,
                onRewardGranted = { },
                onAdFailedOrSkipped = { }
            )
        }

        // 4th claim must be rejected by daily limit
        val fourthEligibility = adMobService.checkRewardedAdEligibility(testUserId1, config)
        assertTrue("4th claim must be DailyLimitReached", fourthEligibility is AdEligibilityResult.DailyLimitReached)

        var failureReported = false
        adMobService.showRewardedAd(
            userId = testUserId1,
            placement = AdPlacement.REWARDED_DIRECT_COINS,
            actionConfig = config,
            onRewardGranted = { },
            onAdFailedOrSkipped = { msg ->
                failureReported = true
                assertTrue(msg.contains("Daily limit"))
            }
        )
        assertTrue(failureReported)
        assertEquals(30L, walletRepository.getCalculatedBalance(testUserId1))
    }

    @Test
    fun testRewardedAd_CooldownEnforcement() = runTest {
        val config = AdActionConfig(
            rewardType = AdRewardType.AD_EXTRA_SPIN,
            rewardAmount = 15L,
            dailyLimit = 5,
            cooldownSeconds = 60L
        )

        // First claim
        adMobService.showRewardedAd(
            userId = testUserId1,
            placement = AdPlacement.REWARDED_SPIN_EXTRA,
            actionConfig = config,
            onRewardGranted = { },
            onAdFailedOrSkipped = { }
        )

        // Immediate second attempt
        val eligibility = adMobService.checkRewardedAdEligibility(testUserId1, config)
        assertTrue("Immediate next attempt must trigger CooldownActive", eligibility is AdEligibilityResult.CooldownActive)
    }

    @Test
    fun testRewardedAd_UserIsolation() = runTest {
        val config = AdActionConfig(
            rewardType = AdRewardType.AD_COIN_REWARD,
            rewardAmount = 20L,
            dailyLimit = 1,
            cooldownSeconds = 0L
        )

        // User 1 claims limit of 1
        adMobService.showRewardedAd(
            userId = testUserId1,
            placement = AdPlacement.REWARDED_DIRECT_COINS,
            actionConfig = config,
            onRewardGranted = { },
            onAdFailedOrSkipped = { }
        )

        // User 1 reached limit
        val u1Check = adMobService.checkRewardedAdEligibility(testUserId1, config)
        assertTrue(u1Check is AdEligibilityResult.DailyLimitReached)

        // User 2 switches session and checks eligibility
        authRepository.login("adtest2@playrewards.com", "Password@123")

        val u2Check = adMobService.checkRewardedAdEligibility(testUserId2, config)
        assertEquals("User 2 must be independently eligible", AdEligibilityResult.Eligible, u2Check)
    }

    @Test
    fun testAdConsentManager_DefaultAndReset() {
        assertEquals(AdConsentStatus.OBTAINED, adConsentManager.consentStatus.value)
        assertTrue(adConsentManager.canRequestAds.value)

        adConsentManager.resetConsentForTesting()
        assertEquals(AdConsentStatus.UNKNOWN, adConsentManager.consentStatus.value)
        assertFalse(adConsentManager.canRequestAds.value)

        adConsentManager.grantConsent()
        assertEquals(AdConsentStatus.OBTAINED, adConsentManager.consentStatus.value)
        assertTrue(adConsentManager.canRequestAds.value)
    }
}
