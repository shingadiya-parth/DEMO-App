package com.example.ui.screens.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.RedemptionRequest
import com.example.data.model.RedemptionReward
import com.example.data.repository.RedemptionRepository
import com.example.data.repository.RedemptionResult
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RewardsUiState(
    val selectedReward: RedemptionReward? = null,
    val isRedemptionDialogVisible: Boolean = false,
    val destinationAccountInput: String = "",
    val toastMessage: String? = null
)

class RewardsViewModel(
    private val redemptionRepository: RedemptionRepository,
    private val walletRepository: WalletRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    val currentBalance: StateFlow<Long> = userRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null) walletRepository.observeCalculatedBalance(user.userId) else flowOf(0L)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    val redemptionHistory: StateFlow<List<RedemptionRequest>> = userRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null) redemptionRepository.observeUserRequests(user.userId) else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getCatalog(): List<RedemptionReward> = redemptionRepository.getRewardCatalog()

    fun openRedeemDialog(reward: RedemptionReward) {
        _uiState.value = _uiState.value.copy(
            selectedReward = reward,
            isRedemptionDialogVisible = true,
            destinationAccountInput = ""
        )
    }

    fun closeRedeemDialog() {
        _uiState.value = _uiState.value.copy(
            selectedReward = null,
            isRedemptionDialogVisible = false
        )
    }

    fun updateDestinationInput(input: String) {
        _uiState.value = _uiState.value.copy(destinationAccountInput = input)
    }

    fun submitRedemption() {
        val reward = _uiState.value.selectedReward ?: return
        val destination = _uiState.value.destinationAccountInput.trim()

        if (destination.isBlank()) {
            _uiState.value = _uiState.value.copy(toastMessage = "Please enter your email or delivery account")
            return
        }

        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch
            val result = redemptionRepository.submitRedemptionRequest(
                userId = user.userId,
                rewardId = reward.rewardId,
                destinationAccount = destination
            )

            when (result) {
                is RedemptionResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isRedemptionDialogVisible = false,
                        selectedReward = null,
                        toastMessage = "Redemption request submitted! ID: ${result.request.id.take(8)}"
                    )
                }
                is RedemptionResult.Error -> {
                    _uiState.value = _uiState.value.copy(toastMessage = result.message)
                }
            }
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    class Factory(
        private val redemptionRepository: RedemptionRepository,
        private val walletRepository: WalletRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RewardsViewModel(redemptionRepository, walletRepository, userRepository) as T
        }
    }
}
