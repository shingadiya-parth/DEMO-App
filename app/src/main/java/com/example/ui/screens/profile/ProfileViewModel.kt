package com.example.ui.screens.profile

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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isEditing: Boolean = false,
    val editDisplayName: String = "",
    val editCountry: String = "IN",
    val editAvatar: String = "avatar_1",
    val isSaving: Boolean = false,
    val toastMessage: String? = null,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    val currentUser: StateFlow<UserAccount?> = userRepository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentBalance: StateFlow<Long> = currentUser.flatMapLatest { user ->
        if (user != null) walletRepository.observeCalculatedBalance(user.userId) else flowOf(0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val lifetimeEarned: StateFlow<Long> = currentUser.flatMapLatest { user ->
        if (user != null) walletRepository.observeLifetimeEarned(user.userId) else flowOf(0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val lifetimeSpent: StateFlow<Long> = currentUser.flatMapLatest { user ->
        if (user != null) walletRepository.observeLifetimeSpent(user.userId) else flowOf(0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun openEditProfile() {
        val user = currentUser.value
        if (user != null) {
            _uiState.update {
                it.copy(
                    isEditing = true,
                    editDisplayName = user.displayName,
                    editCountry = user.country,
                    editAvatar = user.avatar,
                    errorMessage = null
                )
            }
        }
    }

    fun dismissEditProfile() {
        _uiState.update { it.copy(isEditing = false, errorMessage = null) }
    }

    fun onDisplayNameChange(name: String) {
        _uiState.update { it.copy(editDisplayName = name, errorMessage = null) }
    }

    fun onCountryChange(country: String) {
        _uiState.update { it.copy(editCountry = country, errorMessage = null) }
    }

    fun onAvatarChange(avatar: String) {
        _uiState.update { it.copy(editAvatar = avatar, errorMessage = null) }
    }

    fun saveProfile() {
        val state = _uiState.value
        if (state.editDisplayName.isBlank() || state.editDisplayName.length < 2) {
            _uiState.update { it.copy(errorMessage = "Name must be at least 2 characters.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.updateProfile(
                displayName = state.editDisplayName,
                country = state.editCountry,
                avatar = state.editAvatar
            )
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isEditing = false,
                        toastMessage = "Profile updated successfully!"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Failed to update profile."
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
            return ProfileViewModel(authRepository, userRepository, walletRepository) as T
        }
    }
}
