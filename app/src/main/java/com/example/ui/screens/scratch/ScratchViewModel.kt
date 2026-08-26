package com.example.ui.screens.scratch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.config.ScratchGameConfig
import com.example.core.config.ScratchRewardTier
import com.example.data.model.CoinTransaction
import com.example.data.model.ScratchSession
import com.example.data.model.ScratchSessionStatus
import com.example.data.model.TransactionType
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import com.example.domain.engine.DailyScratchStats
import com.example.domain.engine.RewardGrantResult
import com.example.domain.engine.ScratchCardResult
import com.example.domain.engine.ScratchGameEngine
import com.example.services.ads.AdActionConfig
import com.example.services.ads.AdEligibilityResult
import com.example.services.ads.AdMobService
import com.example.services.ads.AdPlacement
import com.example.services.ads.AdRewardType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScratchStage {
    READY,
    SCRATCHING,
    REVEALING,
    REVEALED,
    ALREADY_COMPLETED,
    ERROR
}

data class ScratchUiState(
    val stage: ScratchStage = ScratchStage.READY,
    val scratchSession: ScratchSession? = null,
    val currentTier: ScratchRewardTier? = null,
    val revealedPercent: Float = 0f,
    val dailyStats: DailyScratchStats = DailyScratchStats(
        dailyLimit = ScratchGameConfig.dailyScratchLimit,
        scratchesUsedToday = 0,
        scratchesRemainingToday = ScratchGameConfig.dailyScratchLimit,
        resetDate = ""
    ),
    val coinsAwarded: Long = 0L,
    val newBalance: Long = 0L,
    val isLoadingSession: Boolean = false,
    val isRevealing: Boolean = false,
    val isAdLoading: Boolean = false,
    val adRewardDialog: RewardGrantResult.Success? = null,
    val toastMessage: String? = null,
    val errorMessage: String? = null
)

class ScratchViewModel(
    private val scratchGameEngine: ScratchGameEngine,
    private val walletRepository: WalletRepository,
    private val userRepository: UserRepository,
    private val adMobService: AdMobService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScratchUiState())
    val uiState: StateFlow<ScratchUiState> = _uiState.asStateFlow()

    // Live balance from immutable ledger
    val coinBalance: StateFlow<Long> = userRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null) walletRepository.observeCalculatedBalance(user.userId) else flowOf(0L)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    // Scratch history strictly from central ledger (filtered for SCRATCH_REWARD)
    val scratchHistory: StateFlow<List<CoinTransaction>> = userRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null) {
            walletRepository.observeTransactions(user.userId).map { txList ->
                txList.filter { it.type == TransactionType.SCRATCH_REWARD }
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
        prepareScratchCard()
    }

    fun refreshDailyStats() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch
            val stats = scratchGameEngine.getDailyScratchStats(user.userId)
            _uiState.value = _uiState.value.copy(dailyStats = stats)
        }
    }

    /**
     * Request authoritative scratch session from backend/engine.
     */
    fun prepareScratchCard() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            if (user == null) {
                _uiState.value = _uiState.value.copy(errorMessage = "Please log in to play.")
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoadingSession = true,
                errorMessage = null
            )

            val result = scratchGameEngine.createScratchSession(user.userId)

            when (result) {
                is ScratchCardResult.CardCreated -> {
                    _uiState.value = _uiState.value.copy(
                        stage = ScratchStage.READY,
                        scratchSession = result.session,
                        currentTier = result.tier,
                        revealedPercent = 0f,
                        isLoadingSession = false,
                        dailyStats = _uiState.value.dailyStats.copy(
                            scratchesUsedToday = result.scratchesUsedToday,
                            scratchesRemainingToday = result.scratchesRemainingToday
                        )
                    )
                }
                is ScratchCardResult.LimitReached -> {
                    _uiState.value = _uiState.value.copy(
                        stage = ScratchStage.READY,
                        scratchSession = null,
                        isLoadingSession = false,
                        dailyStats = _uiState.value.dailyStats.copy(
                            scratchesUsedToday = result.scratchesUsedToday,
                            scratchesRemainingToday = (result.limit - result.scratchesUsedToday).coerceAtLeast(0)
                        ),
                        toastMessage = result.message
                    )
                }
                is ScratchCardResult.GameDisabled -> {
                    _uiState.value = _uiState.value.copy(
                        stage = ScratchStage.ERROR,
                        isLoadingSession = false,
                        errorMessage = result.message
                    )
                }
                is ScratchCardResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        stage = ScratchStage.ERROR,
                        isLoadingSession = false,
                        errorMessage = result.message
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isLoadingSession = false)
                }
            }
        }
    }

    /**
     * Called continuously as user drags finger across scratch canvas.
     */
    fun onScratchProgress(percent: Float) {
        val currentState = _uiState.value
        if (currentState.stage == ScratchStage.REVEALED || currentState.stage == ScratchStage.REVEALING || currentState.isRevealing) {
            return
        }

        val clamped = percent.coerceIn(0f, 1f)
        if (currentState.stage == ScratchStage.READY && clamped > 0.02f) {
            _uiState.value = currentState.copy(
                stage = ScratchStage.SCRATCHING,
                revealedPercent = clamped
            )
        } else {
            _uiState.value = currentState.copy(revealedPercent = clamped)
        }

        // Check if threshold reached
        if (clamped >= ScratchGameConfig.revealThresholdPercent) {
            triggerCompleteReward()
        }
    }

    /**
     * Alternative accessible reveal trigger (e.g. "Reveal Card" button).
     */
    fun onAccessibleRevealClick() {
        val currentState = _uiState.value
        if (currentState.stage == ScratchStage.REVEALED || currentState.stage == ScratchStage.REVEALING || currentState.isRevealing) {
            return
        }
        if (currentState.scratchSession == null) {
            prepareScratchCard()
            return
        }
        _uiState.value = currentState.copy(revealedPercent = 1.0f)
        triggerCompleteReward()
    }

    /**
     * Trigger authoritative verification and reward crediting.
     */
    private fun triggerCompleteReward() {
        val currentState = _uiState.value
        val session = currentState.scratchSession ?: return

        if (currentState.isRevealing) return

        _uiState.value = currentState.copy(
            stage = ScratchStage.REVEALING,
            isRevealing = true
        )

        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            if (user == null) {
                _uiState.value = _uiState.value.copy(
                    stage = ScratchStage.ERROR,
                    isRevealing = false,
                    errorMessage = "User session expired."
                )
                return@launch
            }

            val result = scratchGameEngine.completeScratchSession(
                userId = user.userId,
                scratchId = session.scratchId,
                revealedPercent = 1.0f
            )

            when (result) {
                is ScratchCardResult.RewardGranted -> {
                    _uiState.value = _uiState.value.copy(
                        stage = ScratchStage.REVEALED,
                        isRevealing = false,
                        coinsAwarded = result.coinsAwarded,
                        newBalance = result.newBalance,
                        dailyStats = _uiState.value.dailyStats.copy(
                            scratchesUsedToday = result.scratchesUsedToday,
                            scratchesRemainingToday = result.scratchesRemainingToday
                        ),
                        scratchSession = result.session
                    )
                }
                is ScratchCardResult.AlreadyCompleted -> {
                    _uiState.value = _uiState.value.copy(
                        stage = ScratchStage.ALREADY_COMPLETED,
                        isRevealing = false,
                        coinsAwarded = result.coinsAwarded,
                        toastMessage = result.message
                    )
                }
                is ScratchCardResult.LimitReached -> {
                    _uiState.value = _uiState.value.copy(
                        stage = ScratchStage.READY,
                        isRevealing = false,
                        toastMessage = result.message
                    )
                }
                is ScratchCardResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        stage = ScratchStage.ERROR,
                        isRevealing = false,
                        errorMessage = result.message
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isRevealing = false)
                }
            }
        }
    }

    /**
     * Start next scratch card if daily limit permits.
     */
    fun onNextScratchCard() {
        _uiState.value = _uiState.value.copy(
            stage = ScratchStage.READY,
            scratchSession = null,
            currentTier = null,
            revealedPercent = 0f,
            coinsAwarded = 0L
        )
        refreshDailyStats()
        prepareScratchCard()
    }

    fun onExtraAdScratchClick() {
        if (_uiState.value.isAdLoading) return

        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            if (user == null) {
                _uiState.value = _uiState.value.copy(errorMessage = "Please log in to claim video rewards.")
                return@launch
            }

            val actionConfig = AdActionConfig(
                rewardType = AdRewardType.AD_EXTRA_SCRATCH,
                source = "scratch_screen",
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
                placement = AdPlacement.REWARDED_SCRATCH_EXTRA,
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
        private val scratchGameEngine: ScratchGameEngine,
        private val walletRepository: WalletRepository,
        private val userRepository: UserRepository,
        private val adMobService: AdMobService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScratchViewModel(scratchGameEngine, walletRepository, userRepository, adMobService) as T
        }
    }
}
