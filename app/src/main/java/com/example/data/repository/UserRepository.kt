package com.example.data.repository

import com.example.data.local.UserDao
import com.example.data.model.AccountStatus
import com.example.data.model.UserAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class UserRepository(
    private val userDao: UserDao,
    private val authRepository: AuthRepository
) {

    fun observeCurrentUser(): Flow<UserAccount?> {
        return authRepository.currentUserFlow
    }

    suspend fun getCurrentUser(): UserAccount? {
        val userId = authRepository.currentUserId ?: return null
        return userDao.getUser(userId)
    }

    fun getCurrentUserId(): String? {
        return authRepository.currentUserId
    }

    suspend fun updateProfile(displayName: String, country: String, avatar: String): Result<UserAccount> {
        return authRepository.updateProfile(displayName, country, avatar)
    }

    suspend fun updateLastActivity() {
        val userId = authRepository.currentUserId ?: return
        userDao.updateLastActivity(userId)
    }

    suspend fun getUserById(userId: String): UserAccount? {
        return userDao.getUser(userId)
    }
}
