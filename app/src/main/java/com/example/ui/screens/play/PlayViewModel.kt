package com.example.ui.screens.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.GameCategory
import com.example.data.model.GameDefinition
import com.example.data.model.GamePlayStats
import com.example.data.repository.GameRepository
import com.example.data.repository.UserRepository
import com.example.domain.engine.GameEngine
import com.example.domain.engine.RewardGrantResult
import com.example.services.ads.AdActionConfig
import com.example.services.ads.AdMobService
import com.example.services.ads.AdPlacement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayUiState(
    val selectedCategory: GameCategory? = null,
    val selectedGameForDemo: GameDefinition? = null,
    val isGameDemoActive: Boolean = false,
    val simulationResult: RewardGrantResult? = null,
    val toastMessage: String? = null
)

class PlayViewModel(
    private val gameRepository: GameRepository,
    private val gameEngine: GameEngine,
    private val adMobService: AdMobService,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayUiState())
    val uiState: StateFlow<PlayUiState> = _uiState.asStateFlow()

    val games: List<GameDefinition> = gameRepository.getAllGames()

    val todayStats: StateFlow<List<GamePlayStats>> = userRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null) gameRepository.observeTodayStats(user.userId) else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectCategory(category: GameCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun openGamePreview(game: GameDefinition) {
        _uiState.value = _uiState.value.copy(
            selectedGameForDemo = game,
            isGameDemoActive = true,
            simulationResult = null
        )
    }

    fun closeGamePreview() {
        _uiState.value = _uiState.value.copy(
            selectedGameForDemo = null,
            isGameDemoActive = false,
            simulationResult = null
        )
    }

    fun testCompleteRound(game: GameDefinition, isVictory: Boolean = true, watchAd2x: Boolean = false) {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch
            val session = gameEngine.startSession(user.userId, game.gameId)
            val score = if (isVictory) 1000 else 250
            val multiplier = if (watchAd2x) 2.0 else 1.0

            val result = gameEngine.completeGameRound(
                session = session,
                game = game,
                score = score,
                isVictory = isVictory,
                adMultiplier = multiplier
            )

            _uiState.value = _uiState.value.copy(
                simulationResult = result,
                toastMessage = when (result) {
                    is RewardGrantResult.Success -> result.message
                    is RewardGrantResult.AlreadyClaimed -> result.message
                    is RewardGrantResult.Rejected -> result.reason
                }
            )
        }
    }

    fun triggerRewardedAdForDouble(game: GameDefinition) {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch
            val config = AdActionConfig(
                rewardType = com.example.services.ads.AdRewardType.AD_DOUBLE_GAME_COINS,
                source = "rewarded_ad_${game.gameId}",
                rewardAmount = game.baseRewardCoins,
                cooldownSeconds = 30L,
                dailyLimit = 5,
                title = "2x Coin Multiplier",
                actionKeyOverride = "REWARDED_2X_${game.gameId}"
            )

            adMobService.showRewardedAd(
                userId = user.userId,
                placement = AdPlacement.REWARDED_DOUBLE_GAME_COINS,
                actionConfig = config,
                onRewardGranted = { grantResult ->
                    _uiState.value = _uiState.value.copy(
                        simulationResult = grantResult,
                        toastMessage = when (grantResult) {
                            is RewardGrantResult.Success -> "Ad verified! ${grantResult.message}"
                            is RewardGrantResult.AlreadyClaimed -> grantResult.message
                            is RewardGrantResult.Rejected -> grantResult.reason
                        }
                    )
                },
                onAdFailedOrSkipped = { error ->
                    _uiState.value = _uiState.value.copy(toastMessage = error)
                }
            )
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }

    class Factory(
        private val gameRepository: GameRepository,
        private val gameEngine: GameEngine,
        private val adMobService: AdMobService,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayViewModel(gameRepository, gameEngine, adMobService, userRepository) as T
        }
    }
}
