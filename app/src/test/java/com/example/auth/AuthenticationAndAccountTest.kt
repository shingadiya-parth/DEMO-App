package com.example.auth

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.AppDatabase
import com.example.core.security.AuthCryptoHelper
import com.example.data.model.AccountStatus
import com.example.data.model.TransactionType
import com.example.data.repository.AuthException
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthSessionManager
import com.example.data.repository.AuthState
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthenticationAndAccountTest {

    private lateinit var database: AppDatabase
    private lateinit var sessionManager: AuthSessionManager
    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: UserRepository
    private lateinit var walletRepository: WalletRepository

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
        walletRepository = WalletRepository(database, database.coinTransactionDao(), database.userDao())
    }

    @After
    fun teardown() {
        database.close()
        sessionManager.clearSession()
    }

    @Test
    fun testPasswordHashingIsSaltedAndSecure() {
        val password = "StrongPassword123!"
        val salt1 = AuthCryptoHelper.generateSalt()
        val salt2 = AuthCryptoHelper.generateSalt()

        val hash1 = AuthCryptoHelper.hashPassword(password, salt1)
        val hash2 = AuthCryptoHelper.hashPassword(password, salt2)

        // Hashes with different salts MUST NOT match
        assertNotEquals(hash1, hash2)

        // Verification must pass with the correct salt
        assertTrue(AuthCryptoHelper.verifyPassword(password, salt1, hash1))
        assertFalse(AuthCryptoHelper.verifyPassword("WrongPassword", salt1, hash1))
    }

    @Test
    fun testSignUpCreatesUserAccountAndWalletLedger() = runTest {
        val result = authRepository.signUp(
            name = "Test User",
            email = "user@test.com",
            password = "SecretPassword123",
            confirmPassword = "SecretPassword123",
            referralCode = null
        )

        assertTrue(result.isSuccess)
        val user = result.getOrThrow()
        assertEquals("Test User", user.displayName)
        assertEquals("user@test.com", user.email)
        assertEquals(AccountStatus.ACTIVE, user.accountStatus)

        // Check that authState is updated
        val state = authRepository.authState.first()
        assertTrue(state is AuthState.Authenticated)
        assertEquals(user.userId, (state as AuthState.Authenticated).user.userId)

        // Check starting wallet balance from authoritative ledger
        val balance = walletRepository.observeCalculatedBalance(user.userId).first()
        assertEquals(0L, balance) // Initial balance strictly 0 without unearned coins
    }

    @Test
    fun testSignUpValidationFailures() = runTest {
        // Password too short
        val shortPassResult = authRepository.signUp("User", "u@test.com", "12345", "12345")
        assertTrue(shortPassResult.isFailure)
        assertTrue(shortPassResult.exceptionOrNull() is AuthException.WeakPassword)

        // Password mismatch
        val mismatchResult = authRepository.signUp("User", "u@test.com", "123456", "654321")
        assertTrue(mismatchResult.isFailure)
        assertTrue(mismatchResult.exceptionOrNull() is AuthException.PasswordMismatch)

        // Invalid email
        val invalidEmailResult = authRepository.signUp("User", "notanemail", "123456", "123456")
        assertTrue(invalidEmailResult.isFailure)
        assertTrue(invalidEmailResult.exceptionOrNull() is AuthException.InvalidEmail)

        // Duplicate email
        authRepository.signUp("User One", "dup@test.com", "123456", "123456")
        val dupResult = authRepository.signUp("User Two", "dup@test.com", "123456", "123456")
        assertTrue(dupResult.isFailure)
        assertTrue(dupResult.exceptionOrNull() is AuthException.EmailAlreadyRegistered)
    }

    @Test
    fun testLoginAndLogoutFlow() = runTest {
        authRepository.signUp("Alex", "alex@game.com", "AlexPass123", "AlexPass123")
        authRepository.logout()

        assertEquals(AuthState.Unauthenticated, authRepository.authState.first())

        // Login with wrong password
        val wrongLogin = authRepository.login("alex@game.com", "WrongPass")
        assertTrue(wrongLogin.isFailure)

        // Login with correct password
        val successLogin = authRepository.login("alex@game.com", "AlexPass123")
        assertTrue(successLogin.isSuccess)

        val loggedInUser = successLogin.getOrThrow()
        assertEquals("alex@game.com", loggedInUser.email)
        assertEquals("Alex", loggedInUser.displayName)
    }

    @Test
    fun testPasswordResetFlow() = runTest {
        authRepository.signUp("Sam", "sam@game.com", "OldPassword123", "OldPassword123")
        authRepository.logout()

        // Request reset code
        val tokenResult = authRepository.requestPasswordReset("sam@game.com")
        assertTrue(tokenResult.isSuccess)
        val token = tokenResult.getOrThrow()
        assertEquals(6, token.length)

        // Reset password with token
        val resetResult = authRepository.resetPassword(
            email = "sam@game.com",
            token = token,
            newPassword = "NewPassword123",
            confirmPassword = "NewPassword123"
        )
        assertTrue(resetResult.isSuccess)

        // Old password fails
        val oldLogin = authRepository.login("sam@game.com", "OldPassword123")
        assertTrue(oldLogin.isFailure)

        // New password succeeds
        val newLogin = authRepository.login("sam@game.com", "NewPassword123")
        assertTrue(newLogin.isSuccess)
    }

    @Test
    fun testProfileUpdateAllowedFields() = runTest {
        val user = authRepository.signUp("Jordan", "jordan@game.com", "Pass123", "Pass123").getOrThrow()

        val updateResult = authRepository.updateProfile(
            displayName = "Jordan Pro",
            country = "US",
            avatar = "avatar_4"
        )
        assertTrue(updateResult.isSuccess)

        val updatedUser = userRepository.getCurrentUser()
        assertNotNull(updatedUser)
        assertEquals("Jordan Pro", updatedUser?.displayName)
        assertEquals("US", updatedUser?.country)
        assertEquals("avatar_4", updatedUser?.avatar)

        // Security check: email, userId, referralCode remain intact
        assertEquals("jordan@game.com", updatedUser?.email)
        assertEquals(user.userId, updatedUser?.userId)
        assertEquals(user.referralCode, updatedUser?.referralCode)
    }

    @Test
    fun testDeleteAccountCascadingWipe() = runTest {
        val user = authRepository.signUp("DeleteMe", "delete@game.com", "Pass123", "Pass123").getOrThrow()

        // Add an extra transaction and check presence
        walletRepository.recordTransaction(
            userId = user.userId,
            amount = 50L,
            type = TransactionType.GAME_REWARD,
            source = "spin_win",
            idempotencyKey = "key_1"
        )

        val initialTxCount = database.coinTransactionDao().observeTransactions(user.userId).first().size
        assertTrue(initialTxCount > 0)

        // Perform full account deletion
        val deleteResult = authRepository.deleteAccount()
        assertTrue(deleteResult.isSuccess)

        // Verify session is cleared
        assertEquals(AuthState.Unauthenticated, authRepository.authState.first())

        // Verify all user records wiped
        val deletedUser = database.userDao().getUser(user.userId)
        assertNull(deletedUser)

        val credentials = database.authDao().getCredentialsByUserId(user.userId)
        assertNull(credentials)

        val txAfterDelete = database.coinTransactionDao().observeTransactions(user.userId).first()
        assertEquals(0, txAfterDelete.size)
    }
}
