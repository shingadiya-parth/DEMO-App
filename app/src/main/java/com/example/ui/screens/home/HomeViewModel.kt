package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.config.DailyBonusConfig
import com.example.data.model.CoinTransaction
import com.example.data.model.DailyStreak
import com.example.data.model.GameDefinition
import com.example.data.model.UserAccount
import com.example.data.repository.EarnRepository
import com.example.data.repository.GameRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import com.example.domain.engine.RewardGrantResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class DailyBonusUiState {
    data class Available(val rewardCoins: Long) : DailyBonusUiState()
    object Loading : DailyBonusUiState()
    data class Success(val coinsEarned: Long, val newBalance: Long) : DailyBonusUiState()
    data class AlreadyClaimed(val message: String = "Today's bonus already claimed. Come back tomorrow.") : DailyBonusUiState()
    data class Error(val message: String) : DailyBonusUiState()
}

data class RewardGoal(
    val rewardName: String = "₹10 Google Play / Amazon Voucher",
    val rewardValueInr: Int = 10,
    val targetCoins: Long = 7000L
)

class HomeViewModel(
    private val userRepository: UserRepository,
    private val walletRepository: WalletRepository,
    private val gameRepository: GameRepository,
    private val earnRepository: EarnRepository
) : ViewModel() {

    val currentUser: StateFlow<UserAccount?> = userRepository.observeCurrentUser().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val currentBalance: StateFlow<Long> = currentUser.flatMapLatest { user ->
        if (user != null) walletRepository.observeCalculatedBalance(user.userId) else flowOf(0L)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    val currentStreak: StateFlow<DailyStreak?> = currentUser.flatMapLatest { user ->
        if (user != null) earnRepository.observeDailyStreak(user.userId) else flowOf(null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val transactions: StateFlow<List<CoinTransaction>> = currentUser.flatMapLatest { user ->
        if (user != null) walletRepository.observeTransactions(user.userId) else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _dailyBonusState = MutableStateFlow<DailyBonusUiState>(
        DailyBonusUiState.Available(DailyBonusConfig.BONUS_AMOUNT_COINS)
    )
    val dailyBonusState: StateFlow<DailyBonusUiState> = _dailyBonusState.asStateFlow()

    private val _rewardSuccessDialog = MutableStateFlow<DailyBonusUiState.Success?>(null)
    val rewardSuccessDialog: StateFlow<DailyBonusUiState.Success?> = _rewardSuccessDialog.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val nextRewardGoal = RewardGoal(
        rewardName = "₹10 Google Play Voucher",
        rewardValueInr = 10,
        targetCoins = 7000L
    )

    init {
        refreshDailyBonusStatus()
    }

    /**
     * Checks if today's bonus has already been claimed and sets the proper UI state.
     */
    fun refreshDailyBonusStatus() {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val isClaimed = earnRepository.isDailyBonusClaimedToday(user.userId)
            if (isClaimed) {
                _dailyBonusState.value = DailyBonusUiState.AlreadyClaimed()
            } else {
                _dailyBonusState.value = DailyBonusUiState.Available(DailyBonusConfig.BONUS_AMOUNT_COINS)
            }
        }
    }

    /**
     * Time-based greeting generator: Morning, Afternoon, Evening, Night.
     */
    fun getTimeGreeting(calendar: Calendar = Calendar.getInstance()): String {
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }

    fun getFeaturedGames(): List<GameDefinition> {
        return gameRepository.getAllGames()
    }

    /**
     * Authoritative Daily Bonus claim handler.
     * Guaranteed duplicate claim protection via Idempotency and Reward Engine ledger.
     */
    fun claimDailyBonus() {
        if (_dailyBonusState.value is DailyBonusUiState.Loading) return

        viewModelScope.launch {
            val user = currentUser.value
            if (user == null) {
                _toastMessage.value = "Please sign in to claim bonus."
                return@launch
            }

            _dailyBonusState.value = DailyBonusUiState.Loading

            val result = earnRepository.claimDailyBonus(user.userId)
            when (result) {
                is RewardGrantResult.Success -> {
                    val successState = DailyBonusUiState.Success(
                        coinsEarned = result.coinsGranted,
                        newBalance = result.newBalance
                    )
                    _dailyBonusState.value = DailyBonusUiState.AlreadyClaimed()
                    _rewardSuccessDialog.value = successState
                    _toastMessage.value = "+${result.coinsGranted} Coins added to your wallet!"
                }
                is RewardGrantResult.AlreadyClaimed -> {
                    _dailyBonusState.value = DailyBonusUiState.AlreadyClaimed()
                    _toastMessage.value = "Today's bonus already claimed. Come back tomorrow!"
                }
                is RewardGrantResult.Rejected -> {
                    _dailyBonusState.value = DailyBonusUiState.Error(result.reason)
                    _toastMessage.value = result.reason
                }
            }
        }
    }

    fun dismissSuccessDialog() {
        _rewardSuccessDialog.value = null
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    class Factory(
        private val userRepository: UserRepository,
        private val walletRepository: WalletRepository,
        private val gameRepository: GameRepository,
        private val earnRepository: EarnRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(userRepository, walletRepository, gameRepository, earnRepository) as T
        }
    }
}
