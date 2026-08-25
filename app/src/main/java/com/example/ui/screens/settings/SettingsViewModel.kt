package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserAccount
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val toastMessage: String? = null,
    val isLoggingOut: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val showLogoutConfirmation: Boolean = false,
    val showPrivacyPolicyDialog: Boolean = false,
    val showTermsDialog: Boolean = false
)

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<UserAccount?> = userRepository.observeCurrentUser().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun openLogoutDialog() {
        _uiState.update { it.copy(showLogoutConfirmation = true) }
    }

    fun dismissLogoutDialog() {
        _uiState.update { it.copy(showLogoutConfirmation = false) }
    }

    fun openDeleteDialog() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun openPrivacyPolicy() {
        _uiState.update { it.copy(showPrivacyPolicyDialog = true) }
    }

    fun dismissPrivacyPolicy() {
        _uiState.update { it.copy(showPrivacyPolicyDialog = false) }
    }

    fun openTerms() {
        _uiState.update { it.copy(showTermsDialog = true) }
    }

    fun dismissTerms() {
        _uiState.update { it.copy(showTermsDialog = false) }
    }

    fun logout(onSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoggingOut = true, showLogoutConfirmation = false) }
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(isLoggingOut = false, toastMessage = "Signed out successfully.") }
            onSuccess()
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        _uiState.update { it.copy(isDeletingAccount = true, showDeleteConfirmation = false) }
        viewModelScope.launch {
            val result = authRepository.deleteAccount()
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        toastMessage = "Your account and all associated data have been permanently deleted."
                    )
                }
                onSuccess()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        toastMessage = error.message ?: "Failed to delete account."
                    )
                }
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
        private val walletRepository: WalletRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(authRepository, userRepository, walletRepository) as T
        }
    }
}
