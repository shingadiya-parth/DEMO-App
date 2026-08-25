package com.example.ui.screens.earn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.EarnActivity
import com.example.data.model.UserAccount
import com.example.data.repository.EarnRepository
import com.example.data.repository.TransactionResult
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EarnUiState(
    val user: UserAccount? = null,
    val streakDay: Int = 1,
    val toastMessage: String? = null,
    val referralInput: String = ""
)

class EarnViewModel(
    private val earnRepository: EarnRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EarnUiState())
    val uiState: StateFlow<EarnUiState> = _uiState.asStateFlow()

    val currentUser = userRepository.observeCurrentUser().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun getInAppActivities(): List<EarnActivity> = earnRepository.getInAppEarnActivities()

    fun claimStreak(day: Int) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val result = earnRepository.claimDailyStreak(user.userId, day)
            when (result) {
                is TransactionResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        toastMessage = "+${result.transaction.amount} Coins added to your ledger!"
                    )
                }
                is TransactionResult.Duplicate -> {
                    _uiState.value = _uiState.value.copy(
                        toastMessage = "Daily streak bonus already claimed today."
                    )
                }
                is TransactionResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        toastMessage = result.message
                    )
                }
            }
        }
    }

    fun applyReferral(code: String) {
        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(toastMessage = "Please enter a valid referral code")
            return
        }

        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val result = earnRepository.claimReferralBonus(user.userId, code)
            when (result) {
                is TransactionResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        toastMessage = "Referral code applied! +350 Coins credited."
                    )
                }
                is TransactionResult.Duplicate -> {
                    _uiState.value = _uiState.value.copy(
                        toastMessage = "You have already claimed a referral bonus."
                    )
                }
                is TransactionResult.Error -> {
                    _uiState.value = _uiState.value.copy(toastMessage = result.message)
                }
            }
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    class Factory(
        private val earnRepository: EarnRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EarnViewModel(earnRepository, userRepository) as T
        }
    }
}
