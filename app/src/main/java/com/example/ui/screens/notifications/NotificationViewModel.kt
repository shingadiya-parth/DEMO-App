package com.example.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotificationRecord
import com.example.data.model.NotificationPreferences
import com.example.data.model.NotificationType
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import com.example.data.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationUiState(
    val isLoading: Boolean = false,
    val notifications: List<AppNotificationRecord> = emptyList(),
    val unreadCount: Int = 0,
    val preferences: NotificationPreferences = NotificationPreferences(),
    val isPushRegistered: Boolean = false,
    val errorMessage: String? = null,
    val activeTab: NotificationTab = NotificationTab.INBOX
)

enum class NotificationTab {
    INBOX,
    PREFERENCES
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModel(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _activeTab = MutableStateFlow(NotificationTab.INBOX)
    val activeTab: StateFlow<NotificationTab> = _activeTab.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val currentUserId: StateFlow<String> = authRepository.authState
        .map { (it as? AuthState.Authenticated)?.user?.userId ?: "" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.currentUserId ?: "")

    val notifications: StateFlow<List<AppNotificationRecord>> = currentUserId
        .flatMapLatest { uid ->
            if (uid.isBlank()) flowOf(emptyList())
            else notificationRepository.observeNotifications(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> = currentUserId
        .flatMapLatest { uid ->
            if (uid.isBlank()) flowOf(0)
            else notificationRepository.observeUnreadCount(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val preferences: StateFlow<NotificationPreferences> = currentUserId
        .flatMapLatest { uid ->
            if (uid.isBlank()) flowOf(NotificationPreferences())
            else notificationRepository.observePreferences(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationPreferences())

    fun selectTab(tab: NotificationTab) {
        _activeTab.value = tab
    }

    fun markAsRead(notificationId: String) {
        val uid = currentUserId.value
        if (uid.isNotBlank()) {
            viewModelScope.launch {
                try {
                    notificationRepository.markAsRead(notificationId, uid)
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to update notification: ${e.message}"
                }
            }
        }
    }

    fun markAllAsRead() {
        val uid = currentUserId.value
        if (uid.isNotBlank()) {
            viewModelScope.launch {
                try {
                    notificationRepository.markAllAsRead(uid)
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to mark all as read: ${e.message}"
                }
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        val uid = currentUserId.value
        if (uid.isNotBlank()) {
            viewModelScope.launch {
                try {
                    notificationRepository.deleteNotification(notificationId, uid)
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to delete notification: ${e.message}"
                }
            }
        }
    }

    fun togglePreference(categoryKey: String, isEnabled: Boolean) {
        val uid = currentUserId.value
        if (uid.isNotBlank()) {
            viewModelScope.launch {
                notificationRepository.toggleCategory(uid, categoryKey, isEnabled)
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        fun provideFactory(
            notificationRepository: NotificationRepository,
            authRepository: AuthRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NotificationViewModel(notificationRepository, authRepository) as T
            }
        }
    }
}
