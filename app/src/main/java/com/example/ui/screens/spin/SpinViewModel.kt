package com.example.ui.screens.spin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.config.SpinGameConfig
import com.example.core.config.SpinRewardSegment
import com.example.data.model.CoinTransaction
import com.example.data.model.TransactionType
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import com.example.domain.engine.DailySpinStats
import com.example.domain.engine.RewardGrantResult
import com.example.domain.engine.SpinGameEngine
import com.example.domain.engine.SpinResult
import com.example.services.ads.AdActionConfig
import com.example.services.ads.AdEligibilityResult
import com.example.services.ads.AdMobService
import com.example.services.ads.AdPlacement
import com.example.services.ads.AdRewardType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SpinUiState(
    val isSpinning: Boolean = false,
    val targetRotation: Float = 0f,
    val dailyStats: DailySpinStats = DailySpinStats(
        dailyLimit = SpinGameConfig.dailySpinLimit,
        spinsUsedToday = 0,
        spinsRemainingToday = SpinGameConfig.dailySpinLimit,
        resetDate = ""
    ),
    val winningResult: SpinResult.Success? = null,
    val showWinDialog: Boolean = false,
    val isAdLoading: Boolean = false,
    val adRewardDialog: RewardGrantResult.Success? = null,
    val toastMessage: String? = null,
    val errorMessage: String? = null
)

class SpinViewModel(
    private val spinGameEngine: SpinGameEngine,
    private val walletRepository: WalletRepository,
    private val userRepository: UserRepository,
    private val adMobService: AdMobService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpinUiState())
    val uiState: StateFlow<SpinUiState> = _uiState.asStateFlow()

    val segments: List<SpinRewardSegment> = SpinGameConfig.getActiveSegments()

    // Live balance from immutable ledger
    val coinBalance: StateFlow<Long> = userRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null) walletRepository.observeCalculatedBalance(user.userId) else flowOf(0L)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    // Spin history strictly from central ledger (filtered for SPIN_REWARD)
    val spinHistory: StateFlow<List<CoinTransaction>> = userRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null) {
            walletRepository.observeTransactions(user.userId).map { txList ->
                txList.filter { it.type == TransactionType.SPIN_REWARD }
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        refreshDailyStats()
    }

    fun refreshDailyStats() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch
            val stats = spinGameEngine.getDailySpinStats(user.userId)
            _uiState.value = _uiState.value.copy(dailyStats = stats)
        }
    }

    fun performSpin() {
        if (_uiState.value.isSpinning) return

        val stats = _uiState.value.dailyStats
        if (stats.spinsRemainingToday <= 0) {
            _uiState.value = _uiState.value.copy(
                toastMessage = "No spins remaining today. Watch a video ad for bonus coins!"
            )
            return
        }

        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            if (user == null) {
                _uiState.value = _uiState.value.copy(errorMessage = "Please log in to play.")
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isSpinning = true,
                errorMessage = null,
                showWinDialog = false,
                winningResult = null
            )

            // Authoritative server/engine-side result generation & wallet credit
            val result = spinGameEngine.executeAuthoritativeSpin(user.userId)

            when (result) {
                is SpinResult.Success -> {
                    val winningIndex = result.segmentIndex
                    val segmentCount = segments.size
                    val sectorAngle = 360f / segmentCount
                    val sectorCenterAngle = (winningIndex * sectorAngle) + (sectorAngle / 2f)

                    // Calculate rotation to align winning segment center with top pointer (270°)
                    val currentRot = _uiState.value.targetRotation
                    val currentModulo = ((currentRot % 360f) + 360f) % 360f
                    val deltaAngle = ((270f - sectorCenterAngle - currentModulo) % 360f + 360f) % 360f
                    val fullRounds = 6 * 360f
                    val targetRot = currentRot + fullRounds + deltaAngle

                    _uiState.value = _uiState.value.copy(
                        targetRotation = targetRot,
                        dailyStats = _uiState.value.dailyStats.copy(
                            spinsUsedToday = result.spinsUsedToday,
                            spinsRemainingToday = result.spinsRemainingToday
                        ),
                        winningResult = result
                    )

                    // Wait for the wheel deceleration animation to finish (3.5 seconds)
                    delay(3600)

                    _uiState.value = _uiState.value.copy(
                        isSpinning = false,
                        showWinDialog = true
                    )
                }

                is SpinResult.LimitReached -> {
                    _uiState.value = _uiState.value.copy(
                        isSpinning = false,
                        dailyStats = _uiState.value.dailyStats.copy(
                            spinsUsedToday = result.spinsUsedToday,
                            spinsRemainingToday = (result.limit - result.spinsUsedToday).coerceAtLeast(0)
                        ),
                        toastMessage = result.message
                    )
                }

                is SpinResult.GameDisabled -> {
                    _uiState.value = _uiState.value.copy(
                        isSpinning = false,
                        errorMessage = result.message
                    )
                }

                is SpinResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSpinning = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun dismissWinDialog() {
        _uiState.value = _uiState.value.copy(showWinDialog = false)
        refreshDailyStats()
    }

    fun onExtraAdSpinClick() {
        if (_uiState.value.isAdLoading) return

        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            if (user == null) {
                _uiState.value = _uiState.value.copy(errorMessage = "Please log in to claim video rewards.")
                return@launch
            }

            val actionConfig = AdActionConfig(
                rewardType = AdRewardType.AD_EXTRA_SPIN,
                source = "spin_screen",
                rewardAmount = 25L,
                title = "Watch Video for +25 NestCoins"
            )

            val eligibility = adMobService.checkRewardedAdEligibility(user.userId, actionConfig)
            when (eligibility) {
                is AdEligibilityResult.CooldownActive -> {
                    _uiState.value = _uiState.value.copy(
                        toastMessage = "Video ad cooldown: ${eligibility.remainingSeconds}s remaining."
                    )
                    return@launch
                }
                is AdEligibilityResult.DailyLimitReached -> {
                    _uiState.value = _uiState.value.copy(
                        toastMessage = "Daily limit of ${eligibility.maxLimit} video rewards reached for today."
                    )
                    return@launch
                }
                is AdEligibilityResult.Disabled -> {
                    _uiState.value = _uiState.value.copy(toastMessage = eligibility.message)
                    return@launch
                }
                is AdEligibilityResult.NotReady -> {
                    _uiState.value = _uiState.value.copy(toastMessage = eligibility.message)
                    return@launch
                }
                is AdEligibilityResult.Eligible -> {
                    // Start ad flow
                }
            }

            _uiState.value = _uiState.value.copy(isAdLoading = true)

            adMobService.showRewardedAd(
                userId = user.userId,
                placement = AdPlacement.REWARDED_SPIN_EXTRA,
                actionConfig = actionConfig,
                onRewardGranted = { grantResult ->
                    _uiState.value = _uiState.value.copy(isAdLoading = false)
                    when (grantResult) {
                        is RewardGrantResult.Success -> {
                            _uiState.value = _uiState.value.copy(adRewardDialog = grantResult)
                        }
                        is RewardGrantResult.AlreadyClaimed -> {
                            _uiState.value = _uiState.value.copy(toastMessage = grantResult.message)
                        }
                        is RewardGrantResult.Rejected -> {
                            _uiState.value = _uiState.value.copy(errorMessage = grantResult.reason)
                        }
                    }
                },
                onAdFailedOrSkipped = { failureReason ->
                    _uiState.value = _uiState.value.copy(
                        isAdLoading = false,
                        toastMessage = failureReason
                    )
                }
            )
        }
    }

    fun dismissAdRewardDialog() {
        _uiState.value = _uiState.value.copy(adRewardDialog = null)
        refreshDailyStats()
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    class Factory(
        private val spinGameEngine: SpinGameEngine,
        private val walletRepository: WalletRepository,
        private val userRepository: UserRepository,
        private val adMobService: AdMobService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SpinViewModel(spinGameEngine, walletRepository, userRepository, adMobService) as T
        }
    }
}
