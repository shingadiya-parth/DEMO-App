package com.example.ui.screens.spin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.GamePlayStats
import com.example.data.model.TransactionType
import com.example.data.repository.GameRepository
import com.example.data.repository.UserRepository
import com.example.domain.engine.RewardEngine
import com.example.domain.engine.RewardGrantResult
import com.example.services.ads.AdActionConfig
import com.example.services.ads.AdMobService
import com.example.services.ads.AdPlacement
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

data class SpinUiState(
    val isSpinning: Boolean = false,
    val targetRotation: Float = 0f,
    val selectedSectorIndex: Int = 0,
    val lastRewardResult: RewardGrantResult? = null,
    val availableSpinsRemaining: Int = 10,
    val toastMessage: String? = null
)

class SpinViewModel(
    private val rewardEngine: RewardEngine,
    private val gameRepository: GameRepository,
    private val adMobService: AdMobService,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpinUiState())
    val uiState: StateFlow<SpinUiState> = _uiState.asStateFlow()

    val wheelSectors = listOf(25L, 50L, 75L, 100L, 150L, 200L, 350L, 700L)

    val todayStats: StateFlow<List<GamePlayStats>> = userRepository.observeCurrentUser().flatMapLatest { user ->
        if (user != null) gameRepository.observeTodayStats(user.userId) else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun performSpin(isRewardedAdBonus: Boolean = false) {
        if (_uiState.value.isSpinning) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSpinning = true, lastRewardResult = null)

            val winningIndex = Random.nextInt(wheelSectors.size)
            val sectorCoins = wheelSectors[winningIndex]
            val extraRounds = 5 * 360f
            val sectorAngle = 360f / wheelSectors.size
            val targetAngle = extraRounds + (winningIndex * sectorAngle) + (sectorAngle / 2f)

            _uiState.value = _uiState.value.copy(
                targetRotation = targetAngle,
                selectedSectorIndex = winningIndex
            )

            delay(3000)

            val user = userRepository.getCurrentUser()
            if (user != null) {
                val sessionToken = UUID.randomUUID().toString()
                val grantResult = rewardEngine.processGameReward(
                    userId = user.userId,
                    gameId = "spin_win",
                    calculatedScore = sectorCoins.toInt(),
                    rawCoinsProposed = sectorCoins,
                    multiplier = if (isRewardedAdBonus) 2.0 else 1.0,
                    sessionId = sessionToken
                )

                _uiState.value = _uiState.value.copy(
                    isSpinning = false,
                    lastRewardResult = grantResult,
                    toastMessage = when (grantResult) {
                        is RewardGrantResult.Success -> grantResult.message
                        is RewardGrantResult.AlreadyClaimed -> grantResult.message
                        is RewardGrantResult.Rejected -> grantResult.reason
                    }
                )
            } else {
                _uiState.value = _uiState.value.copy(isSpinning = false)
            }
        }
    }

    fun watchRewardedAdForExtraSpin() {
        viewModelScope.launch {
            val user = userRepository.getCurrentUser() ?: return@launch
            val config = AdActionConfig(
                actionKey = "EXTRA_SPIN_${System.currentTimeMillis()}",
                rewardType = TransactionType.AD_REWARD,
                rewardAmount = 50L,
                source = "rewarded_ad_spin_bonus",
                cooldownSeconds = 30L,
                dailyLimit = 5,
                title = "Extra Spin Bonus"
            )

            adMobService.showRewardedAd(
                userId = user.userId,
                placement = AdPlacement.REWARDED_SPIN_EXTRA,
                actionConfig = config,
                onRewardGranted = {
                    performSpin(isRewardedAdBonus = true)
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
        private val rewardEngine: RewardEngine,
        private val gameRepository: GameRepository,
        private val adMobService: AdMobService,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SpinViewModel(rewardEngine, gameRepository, adMobService, userRepository) as T
        }
    }
}
