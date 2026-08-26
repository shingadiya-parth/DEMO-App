package com.example.ui.screens.puzzle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.config.ClientPuzzleQuestion
import com.example.core.config.PuzzleConfig
import com.example.data.model.CoinTransaction
import com.example.data.model.TransactionType
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import com.example.domain.engine.DailyPuzzleStats
import com.example.domain.engine.PuzzleGameEngine
import com.example.domain.engine.PuzzleSessionResult
import com.example.domain.engine.PuzzleSubmitResult
import com.example.domain.engine.RewardGrantResult
import com.example.services.ads.AdActionConfig
import com.example.services.ads.AdEligibilityResult
import com.example.services.ads.AdMobService
import com.example.services.ads.AdPlacement
import com.example.services.ads.AdRewardType
import kotlinx.coroutines.Job
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

enum class PuzzleUiStage {
    LOADING,
    QUESTION_READY,
    SUBMITTING,
    RESULT_CORRECT,
    RESULT_INCORRECT,
    RESULT_EXPIRED,
    LIMIT_REACHED,
    ERROR
}

data class PuzzleUiState(
    val stage: PuzzleUiStage = PuzzleUiStage.LOADING,
    val sessionId: String? = null,
    val question: ClientPuzzleQuestion? = null,
    val selectedOptionIndex: Int? = null,
    val timeRemainingSeconds: Int = 30,
    val totalTimeSeconds: Int = 30,
    val isTimerRunning: Boolean = false,
    val dailyStats: DailyPuzzleStats = DailyPuzzleStats(
        dailyLimit = PuzzleConfig.dailyPuzzleLimit,
        puzzlesCompletedToday = 0,
        puzzlesRemainingToday = PuzzleConfig.dailyPuzzleLimit,
        resetDate = ""
    ),
    val coinsWon: Long = 0L,
    val newBalance: Long = 0L,
    val explanation: String? = null,
    val correctAnswerIndex: Int? = null,
    val isSubmitting: Boolean = false,
    val isAdLoading: Boolean = false,
    val adRewardDialog: RewardGrantResult.Success? = null,
    val toastMessage: String? = null,
    val errorMessage: String? = null
)

class PuzzleViewModel(
    private val puzzleGameEngine: PuzzleGameEngine,
    private val walletRepository: WalletRepository,
    private val userRepository: UserRepository,
    private val adMobService: AdMobService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PuzzleUiState())
    val uiState: StateFlow<PuzzleUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    // Live balance from immutable ledger
    val coinBalance: StateFlow<Long> = userRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null) walletRepository.observeCalculatedBalance(user.userId) else flowOf(0L)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    // Puzzle history strictly from central ledger (filtered for PUZZLE_REWARD)
    val puzzleHistory: StateFlow<List<CoinTransaction>> = userRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null) {
            walletRepository.observeTransactions(user.userId).map { txList ->
                txList.filter { it.type == TransactionType.PUZZLE_REWARD }
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
        loadNextPuzzle()
    }

    fun refreshDailyStats() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch
            val stats = puzzleGameEngine.getDailyPuzzleStats(user.userId)
            _uiState.value = _uiState.value.copy(dailyStats = stats)
        }
    }

    fun loadNextPuzzle() {
        timerJob?.cancel()
        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            if (user == null) {
                _uiState.value = _uiState.value.copy(
                    stage = PuzzleUiStage.ERROR,
                    errorMessage = "Please log in to play."
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                stage = PuzzleUiStage.LOADING,
                selectedOptionIndex = null,
                explanation = null,
                correctAnswerIndex = null,
                coinsWon = 0L,
                errorMessage = null
            )

            val result = puzzleGameEngine.createPuzzleSession(user.userId)

            when (result) {
                is PuzzleSessionResult.QuestionDelivered -> {
                    val limit = result.timeLimitSeconds.takeIf { it > 0 } ?: PuzzleConfig.defaultTimeLimitSeconds
                    _uiState.value = _uiState.value.copy(
                        stage = PuzzleUiStage.QUESTION_READY,
                        sessionId = result.sessionId,
                        question = result.question,
                        selectedOptionIndex = null,
                        timeRemainingSeconds = limit,
                        totalTimeSeconds = limit,
                        isTimerRunning = limit > 0,
                        dailyStats = _uiState.value.dailyStats.copy(
                            puzzlesCompletedToday = result.puzzlesCompletedToday,
                            puzzlesRemainingToday = result.puzzlesRemainingToday
                        )
                    )
                    startCountdownTimer(limit)
                }
                is PuzzleSessionResult.LimitReached -> {
                    _uiState.value = _uiState.value.copy(
                        stage = PuzzleUiStage.LIMIT_REACHED,
                        question = null,
                        dailyStats = _uiState.value.dailyStats.copy(
                            puzzlesCompletedToday = result.puzzlesCompletedToday,
                            puzzlesRemainingToday = 0
                        ),
                        toastMessage = result.message
                    )
                }
                is PuzzleSessionResult.GameDisabled -> {
                    _uiState.value = _uiState.value.copy(
                        stage = PuzzleUiStage.ERROR,
                        errorMessage = result.message
                    )
                }
                is PuzzleSessionResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        stage = PuzzleUiStage.ERROR,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    private fun startCountdownTimer(seconds: Int) {
        timerJob?.cancel()
        if (seconds <= 0) return

        timerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
                _uiState.value = _uiState.value.copy(timeRemainingSeconds = remaining)
            }
            // Timer expired
            if (_uiState.value.stage == PuzzleUiStage.QUESTION_READY) {
                submitAnswer(isTimeout = true)
            }
        }
    }

    fun selectOption(index: Int) {
        if (_uiState.value.stage == PuzzleUiStage.QUESTION_READY) {
            _uiState.value = _uiState.value.copy(selectedOptionIndex = index)
        }
    }

    fun submitAnswer(isTimeout: Boolean = false) {
        val currentState = _uiState.value
        val sessionId = currentState.sessionId ?: return
        val selectedIndex = currentState.selectedOptionIndex

        if (!isTimeout && selectedIndex == null) {
            _uiState.value = currentState.copy(toastMessage = "Please select an answer first.")
            return
        }

        timerJob?.cancel()
        _uiState.value = currentState.copy(
            isSubmitting = true,
            isTimerRunning = false,
            stage = PuzzleUiStage.SUBMITTING
        )

        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            if (user == null) {
                _uiState.value = _uiState.value.copy(
                    stage = PuzzleUiStage.ERROR,
                    isSubmitting = false,
                    errorMessage = "User session expired."
                )
                return@launch
            }

            val answerToSubmit = if (isTimeout) -1 else (selectedIndex ?: -1)
            val result = puzzleGameEngine.submitAnswer(
                userId = user.userId,
                sessionId = sessionId,
                selectedAnswerIndex = answerToSubmit
            )

            // Trigger non-intrusive interstitial ad if eligible after puzzle completion
            adMobService.showInterstitialAd(
                placement = AdPlacement.INTERSTITIAL_PUZZLE_COMPLETE,
                onAdDismissed = { /* Interstitial closed smoothly */ }
            )

            when (result) {
                is PuzzleSubmitResult.Correct -> {
                    _uiState.value = _uiState.value.copy(
                        stage = PuzzleUiStage.RESULT_CORRECT,
                        isSubmitting = false,
                        coinsWon = result.coinsAwarded,
                        newBalance = result.newBalance,
                        explanation = result.explanation,
                        correctAnswerIndex = result.correctAnswerIndex,
                        dailyStats = _uiState.value.dailyStats.copy(
                            puzzlesCompletedToday = result.puzzlesCompletedToday,
                            puzzlesRemainingToday = result.puzzlesRemainingToday
                        )
                    )
                }
                is PuzzleSubmitResult.Incorrect -> {
                    _uiState.value = _uiState.value.copy(
                        stage = PuzzleUiStage.RESULT_INCORRECT,
                        isSubmitting = false,
                        coinsWon = 0L,
                        explanation = result.explanation,
                        correctAnswerIndex = result.correctAnswerIndex,
                        dailyStats = _uiState.value.dailyStats.copy(
                            puzzlesCompletedToday = result.puzzlesCompletedToday,
                            puzzlesRemainingToday = result.puzzlesRemainingToday
                        )
                    )
                }
                is PuzzleSubmitResult.Expired -> {
                    _uiState.value = _uiState.value.copy(
                        stage = PuzzleUiStage.RESULT_EXPIRED,
                        isSubmitting = false,
                        coinsWon = 0L,
                        explanation = result.explanation,
                        correctAnswerIndex = result.correctAnswerIndex,
                        toastMessage = result.message
                    )
                }
                is PuzzleSubmitResult.AlreadySubmitted -> {
                    _uiState.value = _uiState.value.copy(
                        stage = PuzzleUiStage.RESULT_INCORRECT,
                        isSubmitting = false,
                        toastMessage = result.message
                    )
                }
                is PuzzleSubmitResult.LimitReached -> {
                    _uiState.value = _uiState.value.copy(
                        stage = PuzzleUiStage.LIMIT_REACHED,
                        isSubmitting = false,
                        toastMessage = result.message
                    )
                }
                is PuzzleSubmitResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        stage = PuzzleUiStage.ERROR,
                        isSubmitting = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun onExtraAdPuzzleClick() {
        if (_uiState.value.isAdLoading) return

        viewModelScope.launch {
            val user = userRepository.getCurrentUser()
            if (user == null) {
                _uiState.value = _uiState.value.copy(errorMessage = "Please log in to claim video rewards.")
                return@launch
            }

            val actionConfig = AdActionConfig(
                rewardType = AdRewardType.AD_EXTRA_PUZZLE,
                source = "puzzle_screen",
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
                    // Proceed
                }
            }

            _uiState.value = _uiState.value.copy(isAdLoading = true)

            adMobService.showRewardedAd(
                userId = user.userId,
                placement = AdPlacement.REWARDED_PUZZLE_EXTRA,
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

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    class Factory(
        private val puzzleGameEngine: PuzzleGameEngine,
        private val walletRepository: WalletRepository,
        private val userRepository: UserRepository,
        private val adMobService: AdMobService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PuzzleViewModel(puzzleGameEngine, walletRepository, userRepository, adMobService) as T
        }
    }
}
