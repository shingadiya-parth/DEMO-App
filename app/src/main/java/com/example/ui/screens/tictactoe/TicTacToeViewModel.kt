package com.example.ui.screens.tictactoe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.config.TicTacToeConfig
import com.example.core.config.TicTacToeMark
import com.example.core.config.TicTacToeOutcome
import com.example.data.model.CoinTransaction
import com.example.data.model.TransactionType
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import com.example.domain.engine.DailyTicTacToeStats
import com.example.domain.engine.TicTacToeGameEngine
import com.example.domain.engine.TicTacToeMatchState
import com.example.domain.engine.TicTacToeResult
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

data class TicTacToeUiState(
    val sessionId: String? = null,
    val board: List<TicTacToeMark> = List(9) { TicTacToeMark.EMPTY },
    val outcome: TicTacToeOutcome = TicTacToeOutcome.IN_PROGRESS,
    val winningLine: List<Int>? = null,
    val isAiThinking: Boolean = false,
    val isStartingMatch: Boolean = false,
    val lastCompletedState: TicTacToeMatchState? = null,
    val showResultModal: Boolean = false,
    val dailyStats: DailyTicTacToeStats = DailyTicTacToeStats(TicTacToeConfig.dailyMatchLimit, 0, TicTacToeConfig.dailyMatchLimit, ""),
    val toastMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class TicTacToeViewModel(
    private val ticTacToeEngine: TicTacToeGameEngine,
    private val walletRepository: WalletRepository,
    private val userRepository: UserRepository,
    private val adMobService: AdMobService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicTacToeUiState())
    val uiState: StateFlow<TicTacToeUiState> = _uiState.asStateFlow()

    val coinBalance: StateFlow<Long> = userRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null) walletRepository.observeCalculatedBalance(user.userId) else flowOf(0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val gameHistory: StateFlow<List<CoinTransaction>> = userRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null) walletRepository.observeTransactions(user.userId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshDailyStats()
        startNewMatch()
    }

    fun refreshDailyStats() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch
            val stats = ticTacToeEngine.getDailyStats(user.userId)
            _uiState.value = _uiState.value.copy(dailyStats = stats)
        }
    }

    fun startNewMatch() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch
            _uiState.value = _uiState.value.copy(isStartingMatch = true, showResultModal = false)
            val result = ticTacToeEngine.startMatch(user.userId)
            when (result) {
                is TicTacToeResult.MatchCreated -> {
                    _uiState.value = _uiState.value.copy(
                        sessionId = result.sessionId,
                        board = result.board,
                        outcome = TicTacToeOutcome.IN_PROGRESS,
                        winningLine = null,
                        isAiThinking = false,
                        isStartingMatch = false,
                        lastCompletedState = null,
                        dailyStats = _uiState.value.dailyStats.copy(
                            matchesRemainingToday = result.matchesRemainingToday
                        )
                    )
                }
                is TicTacToeResult.LimitReached -> {
                    _uiState.value = _uiState.value.copy(
                        isStartingMatch = false,
                        toastMessage = result.message
                    )
                    refreshDailyStats()
                }
                is TicTacToeResult.GameDisabled -> {
                    _uiState.value = _uiState.value.copy(
                        isStartingMatch = false,
                        toastMessage = result.message
                    )
                }
                is TicTacToeResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isStartingMatch = false,
                        toastMessage = result.message
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isStartingMatch = false)
                }
            }
        }
    }

    fun onCellClicked(cellIndex: Int) {
        val currentState = _uiState.value
        val sessionId = currentState.sessionId ?: return

        if (currentState.isAiThinking || currentState.outcome != TicTacToeOutcome.IN_PROGRESS) {
            return
        }

        if (currentState.board.getOrNull(cellIndex) != TicTacToeMark.EMPTY) {
            return
        }

        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch

            // Show optimistic move for player
            val tempBoard = currentState.board.toMutableList().apply {
                set(cellIndex, TicTacToeMark.X)
            }
            _uiState.value = _uiState.value.copy(board = tempBoard, isAiThinking = true)

            // Small natural delay so AI turn feels intuitive
            delay(400)

            val moveResult = ticTacToeEngine.playMove(
                userId = user.userId,
                sessionId = sessionId,
                cellIndex = cellIndex
            )

            when (moveResult) {
                is TicTacToeResult.MoveResult -> {
                    val state = moveResult.state
                    _uiState.value = _uiState.value.copy(
                        board = state.board,
                        outcome = state.outcome,
                        winningLine = state.winningLine,
                        isAiThinking = false,
                        lastCompletedState = if (state.isCompleted) state else null,
                        showResultModal = state.isCompleted,
                        dailyStats = DailyTicTacToeStats(
                            dailyLimit = TicTacToeConfig.dailyMatchLimit,
                            matchesUsedToday = state.matchesUsedToday,
                            matchesRemainingToday = state.matchesRemainingToday,
                            resetDate = _uiState.value.dailyStats.resetDate
                        )
                    )
                }
                is TicTacToeResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isAiThinking = false,
                        toastMessage = moveResult.message
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isAiThinking = false)
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
        private val ticTacToeEngine: TicTacToeGameEngine,
        private val walletRepository: WalletRepository,
        private val userRepository: UserRepository,
        private val adMobService: AdMobService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TicTacToeViewModel(
                ticTacToeEngine,
                walletRepository,
                userRepository,
                adMobService
            ) as T
        }
    }
}
