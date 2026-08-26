package com.example.ui.screens.cointoss

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.config.CoinSide
import com.example.core.config.CoinTossConfig
import com.example.data.model.CoinTransaction
import com.example.data.model.TransactionType
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import com.example.domain.engine.CoinTossGameEngine
import com.example.domain.engine.CoinTossResult
import com.example.domain.engine.DailyCoinTossStats
import com.example.services.ads.AdMobService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class CoinTossUiState(
    val selectedSide: CoinSide = CoinSide.HEADS,
    val isFlipping: Boolean = false,
    val displayedSide: CoinSide = CoinSide.HEADS,
    val lastResult: CoinTossResult.Success? = null,
    val showResultModal: Boolean = false,
    val dailyStats: DailyCoinTossStats = DailyCoinTossStats(CoinTossConfig.dailyLimit, 0, CoinTossConfig.dailyLimit, ""),
    val toastMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class CoinTossViewModel(
    private val coinTossEngine: CoinTossGameEngine,
    private val walletRepository: WalletRepository,
    private val userRepository: UserRepository,
    private val adMobService: AdMobService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoinTossUiState())
    val uiState: StateFlow<CoinTossUiState> = _uiState.asStateFlow()

    val coinBalance: StateFlow<Long> = userRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null) walletRepository.observeCalculatedBalance(user.userId) else flowOf(0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val gameHistory: StateFlow<List<CoinTransaction>> = userRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null) walletRepository.observeTransactions(user.userId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshDailyStats()
    }

    fun selectSide(side: CoinSide) {
        if (!_uiState.value.isFlipping) {
            _uiState.value = _uiState.value.copy(selectedSide = side)
        }
    }

    fun refreshDailyStats() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch
            val stats = coinTossEngine.getDailyStats(user.userId)
            _uiState.value = _uiState.value.copy(dailyStats = stats)
        }
    }

    fun tossCoin() {
        val currentState = _uiState.value
        if (currentState.isFlipping) return

        if (currentState.dailyStats.attemptsRemainingToday <= 0) {
            _uiState.value = _uiState.value.copy(
                toastMessage = "Daily limit reached. Come back tomorrow for more free tosses!"
            )
            return
        }

        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: run {
                _uiState.value = _uiState.value.copy(toastMessage = "Authentication required.")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isFlipping = true)

            // Authoritative server-side evaluation
            val result = coinTossEngine.playCoinToss(
                userId = user.userId,
                userChoice = currentState.selectedSide
            )

            when (result) {
                is CoinTossResult.Success -> {
                    // Let the coin flip animation run
                    delay(CoinTossConfig.animationDurationMs)
                    _uiState.value = _uiState.value.copy(
                        isFlipping = false,
                        displayedSide = result.outcome,
                        lastResult = result,
                        showResultModal = true,
                        dailyStats = DailyCoinTossStats(
                            dailyLimit = CoinTossConfig.dailyLimit,
                            attemptsUsedToday = result.attemptsUsedToday,
                            attemptsRemainingToday = result.attemptsRemainingToday,
                            resetDate = _uiState.value.dailyStats.resetDate
                        )
                    )
                }
                is CoinTossResult.LimitReached -> {
                    _uiState.value = _uiState.value.copy(
                        isFlipping = false,
                        toastMessage = result.message
                    )
                    refreshDailyStats()
                }
                is CoinTossResult.GameDisabled -> {
                    _uiState.value = _uiState.value.copy(
                        isFlipping = false,
                        toastMessage = result.message
                    )
                }
                is CoinTossResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isFlipping = false,
                        toastMessage = result.message
                    )
                }
                is CoinTossResult.AlreadySubmitted -> {
                    _uiState.value = _uiState.value.copy(
                        isFlipping = false,
                        toastMessage = result.message
                    )
                }
            }
        }
    }

    fun dismissResultModal() {
        _uiState.value = _uiState.value.copy(showResultModal = false)
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    class Factory(
        private val coinTossEngine: CoinTossGameEngine,
        private val walletRepository: WalletRepository,
        private val userRepository: UserRepository,
        private val adMobService: AdMobService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CoinTossViewModel(
                coinTossEngine,
                walletRepository,
                userRepository,
                adMobService
            ) as T
        }
    }
}
