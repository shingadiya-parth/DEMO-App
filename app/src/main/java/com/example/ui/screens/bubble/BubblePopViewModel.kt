package com.example.ui.screens.bubble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.config.BubblePopConfig
import com.example.core.config.BubbleScoreThreshold
import com.example.data.model.CoinTransaction
import com.example.data.model.TransactionType
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import com.example.domain.engine.BubblePopCompleteResult
import com.example.domain.engine.BubblePopGameEngine
import com.example.domain.engine.BubblePopSessionResult
import com.example.domain.engine.DailyBubblePopStats
import com.example.services.ads.AdMobService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class BubbleItem(
    val id: Int,
    val colorIndex: Int,
    val sizeDp: Int = 56,
    val isPopped: Boolean = false,
    val popAnimTrigger: Long = 0L
)

data class BubblePopUiState(
    val sessionId: String? = null,
    val isPlaying: Boolean = false,
    val isRoundStarting: Boolean = false,
    val isSubmitting: Boolean = false,
    val score: Int = 0,
    val bubblesPopped: Int = 0,
    val timeRemainingSeconds: Int = BubblePopConfig.roundDurationSeconds,
    val bubbles: List<BubbleItem> = emptyList(),
    val lastResult: BubblePopCompleteResult.Success? = null,
    val showResultModal: Boolean = false,
    val dailyStats: DailyBubblePopStats = DailyBubblePopStats(BubblePopConfig.dailyRoundLimit, 0, BubblePopConfig.dailyRoundLimit, ""),
    val toastMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class BubblePopViewModel(
    private val bubblePopEngine: BubblePopGameEngine,
    private val walletRepository: WalletRepository,
    private val userRepository: UserRepository,
    private val adMobService: AdMobService
) : ViewModel() {

    private val _uiState = MutableStateFlow(BubblePopUiState())
    val uiState: StateFlow<BubblePopUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    val coinBalance: StateFlow<Long> = userRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null) walletRepository.observeCalculatedBalance(user.userId) else flowOf(0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val gameHistory: StateFlow<List<CoinTransaction>> = userRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null) walletRepository.observeTransactions(user.userId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshDailyStats()
        initializeGrid()
    }

    private fun initializeGrid() {
        val initialBubbles = List(16) { id ->
            BubbleItem(
                id = id,
                colorIndex = Random.nextInt(5),
                sizeDp = Random.nextInt(50, 64)
            )
        }
        _uiState.value = _uiState.value.copy(bubbles = initialBubbles)
    }

    fun refreshDailyStats() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch
            val stats = bubblePopEngine.getDailyStats(user.userId)
            _uiState.value = _uiState.value.copy(dailyStats = stats)
        }
    }

    fun startRound() {
        if (_uiState.value.isPlaying || _uiState.value.isRoundStarting) return

        if (_uiState.value.dailyStats.roundsRemainingToday <= 0) {
            _uiState.value = _uiState.value.copy(
                toastMessage = "Daily limit reached. Come back tomorrow for more free rounds!"
            )
            return
        }

        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch

            _uiState.value = _uiState.value.copy(
                isRoundStarting = true,
                showResultModal = false,
                score = 0,
                bubblesPopped = 0,
                timeRemainingSeconds = BubblePopConfig.roundDurationSeconds
            )

            val sessionResult = bubblePopEngine.startRound(user.userId)
            when (sessionResult) {
                is BubblePopSessionResult.SessionStarted -> {
                    initializeGrid()
                    _uiState.value = _uiState.value.copy(
                        sessionId = sessionResult.sessionId,
                        isPlaying = true,
                        isRoundStarting = false,
                        timeRemainingSeconds = sessionResult.durationSeconds,
                        dailyStats = _uiState.value.dailyStats.copy(
                            roundsRemainingToday = sessionResult.roundsRemainingToday
                        )
                    )
                    startTimer()
                }
                is BubblePopSessionResult.LimitReached -> {
                    _uiState.value = _uiState.value.copy(
                        isRoundStarting = false,
                        toastMessage = sessionResult.message
                    )
                    refreshDailyStats()
                }
                is BubblePopSessionResult.GameDisabled -> {
                    _uiState.value = _uiState.value.copy(
                        isRoundStarting = false,
                        toastMessage = sessionResult.message
                    )
                }
                is BubblePopSessionResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isRoundStarting = false,
                        toastMessage = sessionResult.message
                    )
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isPlaying && _uiState.value.timeRemainingSeconds > 0) {
                delay(1000)
                val remaining = _uiState.value.timeRemainingSeconds - 1
                _uiState.value = _uiState.value.copy(timeRemainingSeconds = remaining)

                if (remaining <= 0) {
                    finishRound()
                    break
                }
            }
        }
    }

    fun onBubbleTapped(bubbleId: Int) {
        val currentState = _uiState.value
        if (!currentState.isPlaying || currentState.timeRemainingSeconds <= 0) return

        val bubbleList = currentState.bubbles.toMutableList()
        val index = bubbleList.indexOfFirst { it.id == bubbleId }
        if (index != -1) {
            // Replace popped bubble with a brand new colored bubble immediately
            val newBubble = BubbleItem(
                id = bubbleId,
                colorIndex = Random.nextInt(5),
                sizeDp = Random.nextInt(50, 64),
                popAnimTrigger = System.currentTimeMillis()
            )
            bubbleList[index] = newBubble

            val newPoppedCount = currentState.bubblesPopped + 1
            val newScore = newPoppedCount * BubblePopConfig.pointsPerBubble

            _uiState.value = currentState.copy(
                bubbles = bubbleList,
                bubblesPopped = newPoppedCount,
                score = newScore
            )
        }
    }

    private fun finishRound() {
        timerJob?.cancel()
        val currentState = _uiState.value
        val sessionId = currentState.sessionId ?: return

        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch
            _uiState.value = _uiState.value.copy(isPlaying = false, isSubmitting = true)

            val elapsedSeconds = BubblePopConfig.roundDurationSeconds - currentState.timeRemainingSeconds
            val completeResult = bubblePopEngine.completeRound(
                userId = user.userId,
                sessionId = sessionId,
                claimedBubblesPopped = currentState.bubblesPopped,
                elapsedSeconds = elapsedSeconds.coerceAtLeast(BubblePopConfig.roundDurationSeconds)
            )

            when (completeResult) {
                is BubblePopCompleteResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        lastResult = completeResult,
                        showResultModal = true,
                        dailyStats = DailyBubblePopStats(
                            dailyLimit = BubblePopConfig.dailyRoundLimit,
                            roundsUsedToday = completeResult.roundsUsedToday,
                            roundsRemainingToday = completeResult.roundsRemainingToday,
                            resetDate = _uiState.value.dailyStats.resetDate
                        )
                    )
                }
                is BubblePopCompleteResult.InvalidScore -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        toastMessage = completeResult.reason
                    )
                    refreshDailyStats()
                }
                is BubblePopCompleteResult.AlreadyCompleted -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        toastMessage = completeResult.message
                    )
                }
                is BubblePopCompleteResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        toastMessage = completeResult.message
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

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    class Factory(
        private val bubblePopEngine: BubblePopGameEngine,
        private val walletRepository: WalletRepository,
        private val userRepository: UserRepository,
        private val adMobService: AdMobService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BubblePopViewModel(
                bubblePopEngine,
                walletRepository,
                userRepository,
                adMobService
            ) as T
        }
    }
}
