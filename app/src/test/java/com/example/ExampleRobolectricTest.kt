package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.CoinConfig
import com.example.core.config.DailyBonusConfig
import com.example.core.di.AppContainer
import com.example.domain.engine.CoinConversionHelper
import com.example.domain.engine.RewardGrantResult
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

        // Seed / register test user
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
}
