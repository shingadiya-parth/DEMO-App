package com.example.data.repository

import androidx.room.withTransaction
import com.example.core.database.AppDatabase
import com.example.core.security.AuthCryptoHelper
import com.example.data.local.AuthDao
import com.example.data.local.CoinTransactionDao
import com.example.data.local.GamePlayStatsDao
import com.example.data.local.RedemptionDao
import com.example.data.local.UserDao
import com.example.data.model.AccountStatus
import com.example.data.model.AuthCredentials
import com.example.data.model.UserAccount
import com.example.data.model.Wallet
import com.example.data.model.WalletStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val user: UserAccount) : AuthState
}

sealed class AuthException(message: String) : Exception(message) {
    class InvalidCredentials(message: String = "Invalid email or password.") : AuthException(message)
    class EmailAlreadyRegistered(message: String = "An account with this email already exists.") : AuthException(message)
    class WeakPassword(message: String = "Password must be at least 6 characters.") : AuthException(message)
    class PasswordMismatch(message: String = "Passwords do not match.") : AuthException(message)
    class InvalidEmail(message: String = "Please enter a valid email address.") : AuthException(message)
    class AccountDisabled(message: String = "This account has been suspended or deactivated.") : AuthException(message)
    class UserNotFound(message: String = "No account found with this email address.") : AuthException(message)
    class InvalidResetToken(message: String = "Invalid or expired reset code.") : AuthException(message)
    class NotAuthenticated(message: String = "User is not authenticated.") : AuthException(message)
    class GeneralError(message: String) : AuthException(message)
}

class AuthRepository(
    private val database: AppDatabase,
    private val authDao: AuthDao,
    private val userDao: UserDao,
    private val coinTransactionDao: CoinTransactionDao,
    private val redemptionDao: RedemptionDao,
    private val gamePlayStatsDao: GamePlayStatsDao,
    private val sessionManager: AuthSessionManager,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUserId: String?
        get() = (_authState.value as? AuthState.Authenticated)?.user?.userId ?: sessionManager.getActiveUserId()

    init {
        externalScope.launch {
            initializeSession()
        }
    }

    /**
     * Observes the authoritative database user model for the currently logged in user.
     */
    val currentUserFlow: Flow<UserAccount?> = _authState.flatMapLatest { state ->
        when (state) {
            is AuthState.Authenticated -> userDao.observeUser(state.user.userId)
            else -> flowOf(null)
        }
    }

    private suspend fun initializeSession() = withContext(Dispatchers.IO) {
        val savedUserId = sessionManager.getActiveUserId()
        if (savedUserId != null) {
            val user = userDao.getUser(savedUserId)
            if (user != null && user.accountStatus == AccountStatus.ACTIVE) {
                // Ensure wallet exists
                val wallet = database.walletDao().getWalletByUserId(savedUserId)
                if (wallet == null) {
                    database.walletDao().insertWallet(
                        Wallet(
                            walletId = "wal_${UUID.randomUUID()}",
                            userId = savedUserId,
                            balance = 0L,
                            lifetimeEarned = 0L,
                            lifetimeSpent = 0L,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            walletStatus = WalletStatus.ACTIVE
                        )
                    )
                }
                userDao.updateLastActivity(savedUserId)
                _authState.value = AuthState.Authenticated(user)
                return@withContext
            } else {
                sessionManager.clearSession()
            }
        }
        _authState.value = AuthState.Unauthenticated
    }

    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        referralCode: String? = null
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()

        // Validations
        if (cleanName.length < 2) {
            return@withContext Result.failure(AuthException.GeneralError("Name must be at least 2 characters."))
        }
        if (!isValidEmail(cleanEmail)) {
            return@withContext Result.failure(AuthException.InvalidEmail())
        }
        if (password.length < 6) {
            return@withContext Result.failure(AuthException.WeakPassword())
        }
        if (password != confirmPassword) {
            return@withContext Result.failure(AuthException.PasswordMismatch())
        }

        // Check if email exists
        val existingCredentials = authDao.getCredentialsByEmail(cleanEmail)
        if (existingCredentials != null) {
            return@withContext Result.failure(AuthException.EmailAlreadyRegistered())
        }

        // Check referral code if provided
        var referrerUserId: String? = null
        if (!referralCode.isNullOrBlank()) {
            val cleanCode = referralCode.trim().uppercase()
            val referringUser = userDao.getUserByReferralCode(cleanCode)
            if (referringUser != null) {
                referrerUserId = referringUser.userId
            }
        }

        val userId = AuthCryptoHelper.generateUserId()
        val salt = AuthCryptoHelper.generateSalt()
        val passwordHash = AuthCryptoHelper.hashPassword(password, salt)
        val userReferralCode = AuthCryptoHelper.generateReferralCode()

        val credentials = AuthCredentials(
            userId = userId,
            email = cleanEmail,
            passwordHash = passwordHash,
            salt = salt,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val newUser = UserAccount(
            userId = userId,
            displayName = cleanName,
            email = cleanEmail,
            avatar = "avatar_1",
            country = "IN",
            coinBalance = 0L, // Initial wallet balance strictly 0
            totalCoinsEarned = 0L,
            totalCoinsSpent = 0L,
            accountCreationDate = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis(),
            lastActivity = System.currentTimeMillis(),
            referralCode = userReferralCode,
            referredBy = referrerUserId,
            accountStatus = AccountStatus.ACTIVE
        )

        val newWallet = Wallet(
            walletId = "wal_${UUID.randomUUID()}",
            userId = userId,
            balance = 0L,
            lifetimeEarned = 0L,
            lifetimeSpent = 0L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            walletStatus = WalletStatus.ACTIVE
        )

        database.withTransaction {
            authDao.insertCredentials(credentials)
            userDao.insertUser(newUser)
            database.walletDao().insertWallet(newWallet)
        }

        sessionManager.setActiveUserId(userId)
        _authState.value = AuthState.Authenticated(newUser)
        Result.success(newUser)
    }

    suspend fun login(email: String, password: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (!isValidEmail(cleanEmail)) {
            return@withContext Result.failure(AuthException.InvalidEmail())
        }
        if (password.isEmpty()) {
            return@withContext Result.failure(AuthException.InvalidCredentials("Password cannot be empty."))
        }

        val credentials = authDao.getCredentialsByEmail(cleanEmail)
            ?: return@withContext Result.failure(AuthException.InvalidCredentials())

        val isValid = AuthCryptoHelper.verifyPassword(password, credentials.salt, credentials.passwordHash)
        if (!isValid) {
            return@withContext Result.failure(AuthException.InvalidCredentials())
        }

        val user = userDao.getUser(credentials.userId)
            ?: return@withContext Result.failure(AuthException.UserNotFound())

        if (user.accountStatus == AccountStatus.SUSPENDED || user.accountStatus == AccountStatus.DEACTIVATED) {
            return@withContext Result.failure(AuthException.AccountDisabled())
        }

        // Ensure wallet exists
        val wallet = database.walletDao().getWalletByUserId(user.userId)
        if (wallet == null) {
            database.walletDao().insertWallet(
                Wallet(
                    walletId = "wal_${UUID.randomUUID()}",
                    userId = user.userId,
                    balance = 0L,
                    lifetimeEarned = 0L,
                    lifetimeSpent = 0L,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    walletStatus = WalletStatus.ACTIVE
                )
            )
        }

        userDao.updateLastLogin(user.userId)
        sessionManager.setActiveUserId(user.userId)
        _authState.value = AuthState.Authenticated(user)
        Result.success(user)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        sessionManager.clearSession()
        _authState.value = AuthState.Unauthenticated
    }

    suspend fun requestPasswordReset(email: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (!isValidEmail(cleanEmail)) {
            return@withContext Result.failure(AuthException.InvalidEmail())
        }

        val credentials = authDao.getCredentialsByEmail(cleanEmail)
            ?: return@withContext Result.failure(AuthException.UserNotFound())

        val token = AuthCryptoHelper.generateResetToken()
        val expiresAt = System.currentTimeMillis() + (15 * 60 * 1000) // 15 minutes
        authDao.updateResetToken(cleanEmail, token, expiresAt)

        Result.success(token)
    }

    suspend fun resetPassword(
        email: String,
        token: String,
        newPassword: String,
        confirmPassword: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanToken = token.trim()

        if (newPassword.length < 6) {
            return@withContext Result.failure(AuthException.WeakPassword())
        }
        if (newPassword != confirmPassword) {
            return@withContext Result.failure(AuthException.PasswordMismatch())
        }

        val credentials = authDao.getCredentialsByEmail(cleanEmail)
            ?: return@withContext Result.failure(AuthException.UserNotFound())

        if (credentials.resetToken == null || credentials.resetToken != cleanToken) {
            return@withContext Result.failure(AuthException.InvalidResetToken())
        }

        val newSalt = AuthCryptoHelper.generateSalt()
        val newHash = AuthCryptoHelper.hashPassword(newPassword, newSalt)
        authDao.updatePassword(cleanEmail, newHash, newSalt)
        Result.success(Unit)
    }

    suspend fun updateProfile(
        displayName: String,
        country: String,
        avatar: String
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        val currentUserId = this@AuthRepository.currentUserId
            ?: return@withContext Result.failure(AuthException.NotAuthenticated())

        val currentUser = userDao.getUser(currentUserId)
            ?: return@withContext Result.failure(AuthException.UserNotFound())

        val cleanName = displayName.trim()
        if (cleanName.length < 2) {
            return@withContext Result.failure(AuthException.GeneralError("Name must be at least 2 characters."))
        }

        val updatedUser = currentUser.copy(
            displayName = cleanName,
            country = country.trim().uppercase().ifEmpty { "IN" },
            avatar = avatar,
            lastActivity = System.currentTimeMillis()
        )

        userDao.updateUser(updatedUser)
        _authState.value = AuthState.Authenticated(updatedUser)
        Result.success(updatedUser)
    }

    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUserId = this@AuthRepository.currentUserId
            ?: return@withContext Result.failure(AuthException.NotAuthenticated())

        database.withTransaction {
            database.walletDao().deleteWalletForUser(currentUserId)
            coinTransactionDao.deleteTransactionsForUser(currentUserId)
            gamePlayStatsDao.deleteStatsForUser(currentUserId)
            redemptionDao.deleteRedemptionsForUser(currentUserId)
            userDao.deleteUser(currentUserId)
            authDao.deleteCredentials(currentUserId)
        }

        sessionManager.clearSession()
        _authState.value = AuthState.Unauthenticated
        Result.success(Unit)
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
