package com.example.ui.screens.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.RedemptionEligibilityResult
import com.example.data.model.RedemptionRequest
import com.example.data.model.RedemptionReward
import com.example.data.model.RewardCategory
import com.example.data.repository.RedemptionRepository
import com.example.data.repository.RedemptionResult
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class RewardsUiState(
    val selectedCategory: RewardCategory? = null,
    val selectedRewardDetail: RedemptionReward? = null,
    val selectedRewardConfirm: RedemptionReward? = null,
    val selectedRedemptionDetail: RedemptionRequest? = null,
    val destinationAccountInput: String = "",
    val isSubmitting: Boolean = false,
    val successRedemption: RedemptionRequest? = null,
    val toastMessage: String? = null,
    val activeTab: RewardsTab = RewardsTab.CATALOG
)

enum class RewardsTab {
    CATALOG,
    MY_REDEMPTIONS
}

class RewardsViewModel(
    private val redemptionRepository: RedemptionRepository,
    private val walletRepository: WalletRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    // Live balance flow
    val currentBalance: StateFlow<Long> = userRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null) walletRepository.observeCalculatedBalance(user.userId) else flowOf(0L)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    // Live redemption history flow
    val redemptionHistory: StateFlow<List<RedemptionRequest>> = userRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null) redemptionRepository.observeUserRequests(user.userId) else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getCatalog(): List<RedemptionReward> {
        val all = redemptionRepository.getRewardCatalog()
        val cat = _uiState.value.selectedCategory
        return if (cat == null) all else all.filter { it.category == cat }
    }

    fun selectCategory(category: RewardCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun selectTab(tab: RewardsTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun openRewardDetail(reward: RedemptionReward) {
        _uiState.value = _uiState.value.copy(
            selectedRewardDetail = reward,
            selectedRewardConfirm = null
        )
    }

    fun closeRewardDetail() {
        _uiState.value = _uiState.value.copy(selectedRewardDetail = null)
    }

    fun openConfirmationModal(reward: RedemptionReward) {
        // Pre-fill destination with user email if available
        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            val prefill = if (reward.category == RewardCategory.UPI_REWARDS) "" else (user?.email ?: "")
            _uiState.value = _uiState.value.copy(
                selectedRewardDetail = null,
                selectedRewardConfirm = reward,
                destinationAccountInput = prefill
            )
        }
    }

    fun closeConfirmationModal() {
        _uiState.value = _uiState.value.copy(
            selectedRewardConfirm = null,
            destinationAccountInput = "",
            isSubmitting = false
        )
    }

    fun openRedemptionDetail(redemption: RedemptionRequest) {
        _uiState.value = _uiState.value.copy(selectedRedemptionDetail = redemption)
    }

    fun closeRedemptionDetail() {
        _uiState.value = _uiState.value.copy(selectedRedemptionDetail = null)
    }

    fun updateDestinationInput(input: String) {
        _uiState.value = _uiState.value.copy(destinationAccountInput = input)
    }

    fun submitRedemption() {
        val reward = _uiState.value.selectedRewardConfirm ?: return
        val destination = _uiState.value.destinationAccountInput.trim()

        if (_uiState.value.isSubmitting) return

        if (destination.isBlank()) {
            val label = if (reward.category == RewardCategory.UPI_REWARDS) "UPI ID" else "Delivery Email / Phone"
            _uiState.value = _uiState.value.copy(toastMessage = "Please enter your $label")
            return
        }

        _uiState.value = _uiState.value.copy(isSubmitting = true)

        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            if (user == null) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    toastMessage = "Please sign in to redeem rewards."
                )
                return@launch
            }

            // Centralized idempotency key per intent
            val clientKey = "REDEEM_ACTION_${user.userId}_${reward.rewardId}_${System.currentTimeMillis()}"

            val result = redemptionRepository.submitRedemptionRequest(
                userId = user.userId,
                rewardId = reward.rewardId,
                destinationAccount = destination,
                idempotencyKey = clientKey
            )

            when (result) {
                is RedemptionResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        selectedRewardConfirm = null,
                        selectedRewardDetail = null,
                        successRedemption = result.request,
                        toastMessage = "Redemption request submitted! Ref: ${result.request.redemptionId.take(12)}"
                    )
                }
                is RedemptionResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        toastMessage = result.message
                    )
                }
            }
        }
    }

    fun closeSuccessDialog() {
        _uiState.value = _uiState.value.copy(successRedemption = null)
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
