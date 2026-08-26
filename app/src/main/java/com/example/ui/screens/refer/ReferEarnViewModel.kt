package com.example.ui.screens.refer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ReferralSummary
import com.example.data.model.UserAccount
import com.example.data.repository.ReferralRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReferEarnUiState(
    val inputCode: String = "",
    val isApplying: Boolean = false,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

class ReferEarnViewModel(
    private val referralRepository: ReferralRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReferEarnUiState())
    val uiState: StateFlow<ReferEarnUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<UserAccount?> = userRepository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val referralSummary: StateFlow<ReferralSummary?> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                referralRepository.observeReferralSummary(user.userId)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun onCodeInputChanged(newCode: String) {
        _uiState.value = _uiState.value.copy(
            inputCode = newCode.uppercase().filter { it.isLetterOrDigit() }.take(16),
            errorMessage = null
        )
    }

    fun applyReferralCode() {
        val user = currentUser.value ?: return
        val code = _uiState.value.inputCode.trim()

        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid referral code.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true, errorMessage = null)
            val result = referralRepository.applyReferralCode(user.userId, code)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        inputCode = "",
                        isApplying = false,
                        feedbackMessage = "Referral code applied! Play 3 games to complete qualification and earn your reward."
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isApplying = false,
                        errorMessage = error.message ?: "Failed to apply referral code."
                    )
                }
            )
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null, errorMessage = null)
    }

    class Factory(
        private val referralRepository: ReferralRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReferEarnViewModel(referralRepository, userRepository) as T
        }
    }
}
