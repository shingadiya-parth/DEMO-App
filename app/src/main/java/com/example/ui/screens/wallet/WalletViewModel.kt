package com.example.ui.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CoinTransaction
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import com.example.data.repository.EarningsSummary
import com.example.data.repository.TransactionFilter
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

data class WalletUiState(
    val isLoading: Boolean = false,
    val selectedFilter: TransactionFilter = TransactionFilter.ALL,
    val summary: EarningsSummary = EarningsSummary(
        balance = 0L,
        lifetimeEarned = 0L,
        lifetimeSpent = 0L,
        currencyEstimate = "₹0.00",
        rateExplanation = "700 Coins = ₹1.00 INR"
    ),
    val transactions: List<CoinTransaction> = emptyList(),
    val isPaging: Boolean = false,
    val hasMorePages: Boolean = true,
    val errorMessage: String? = null
)

class WalletViewModel(
    private val walletRepository: WalletRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(TransactionFilter.ALL)
    val selectedFilter: StateFlow<TransactionFilter> = _selectedFilter.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val currentUserIdFlow = authRepository.authState.flatMapLatest { state ->
        when (state) {
            is AuthState.Authenticated -> flowOf(state.user.userId)
            else -> flowOf(null)
        }
    }

    val liveBalance: StateFlow<Long> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId != null) walletRepository.observeCalculatedBalance(userId) else flowOf(0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val liveSummary: StateFlow<EarningsSummary> = currentUserIdFlow.flatMapLatest { userId ->
        if (userId != null) walletRepository.observeEarningsSummary(userId) else flowOf(
            EarningsSummary(0L, 0L, 0L, "₹0.00", "700 Coins = ₹1.00 INR")
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        EarningsSummary(0L, 0L, 0L, "₹0.00", "700 Coins = ₹1.00 INR")
    )

    val liveTransactions: StateFlow<List<CoinTransaction>> = combine(
        currentUserIdFlow,
        _selectedFilter
    ) { userId, filter ->
        Pair(userId, filter)
    }.flatMapLatest { (userId, filter) ->
        if (userId != null) {
            walletRepository.observeFilteredTransactions(userId, filter)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: TransactionFilter) {
        _selectedFilter.value = filter
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun refreshWallet() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val userId = authRepository.currentUserId
            if (userId != null) {
                walletRepository.getEarningsSummary(userId)
            }
            _isRefreshing.value = false
        }
    }

    class Factory(
        private val walletRepository: WalletRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WalletViewModel(walletRepository, authRepository) as T
        }
    }
}
