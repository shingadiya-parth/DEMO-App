package com.example.ui.screens.cointoss

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.CoinSide
import com.example.core.config.CoinTossConfig
import com.example.data.model.TransactionType
import com.example.domain.engine.CoinConversionHelper
import com.example.services.ads.AdPlacement
import com.example.ui.components.AdBannerContainer
import com.example.ui.components.AppCard
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.GameHistorySection
import com.example.ui.components.GameLimitCard
import com.example.ui.components.GameResultModal
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinTossScreen(
    viewModel: CoinTossViewModel,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val coinBalance by viewModel.coinBalance.collectAsState()
    val gameHistory by viewModel.gameHistory.collectAsState()

    val rotation = remember { Animatable(0f) }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    LaunchedEffect(uiState.isFlipping) {
        if (uiState.isFlipping) {
            rotation.animateTo(
                targetValue = rotation.value + 1800f,
                animationSpec = tween(
                    durationMillis = CoinTossConfig.animationDurationMs.toInt(),
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BackgroundLight)
            .testTag("coin_toss_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Coin Toss",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.TextNavy
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("coin_toss_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColors.TextNavy
                        )
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier.padding(end = AppSpacing.md),
                        shape = RoundedCornerShape(20.dp),
                        color = AppColors.SurfaceLight,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MonetizationOn,
                                contentDescription = null,
                                tint = AppColors.GoldCoin,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${CoinConversionHelper.formatCoins(coinBalance)} Coins",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = AppColors.TextNavy
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.BackgroundLight)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("coin_toss_content"),
            contentPadding = PaddingValues(horizontal = AppSpacing.screenHorizontal, vertical = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            // Daily Limit Card
            item {
                GameLimitCard(
                    usedCount = uiState.dailyStats.attemptsUsedToday,
                    limit = uiState.dailyStats.dailyLimit,
                    gameLabel = "Tosses"
                )
            }

            // Coin Flip Arena
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Predict & Win Coins",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.TextNavy
                        )
                        Text(
                            text = "Win +${CoinTossConfig.winningReward} NestCoins on every correct guess!",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )

                        Spacer(modifier = Modifier.height(AppSpacing.xl))

                        // 3D Animated Coin
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .graphicsLayer {
                                    rotationY = rotation.value
                                    cameraDistance = 12f * density
                                }
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            AppColors.GoldCoinLight,
                                            AppColors.GoldCoin,
                                            AppColors.GoldCoinDark
                                        )
                                    )
                                )
                                .border(5.dp, Color(0xFFFFF3BF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.displayedSide == CoinSide.HEADS) Icons.Filled.Stars else Icons.Filled.MonetizationOn,
                                    contentDescription = uiState.displayedSide.displayName,
                                    tint = Color(0xFF5A3A00),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.displayedSide.displayName.uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.5.sp
                                    ),
                                    color = Color(0xFF5A3A00)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.xl))

                        // Selection Chips: HEADS vs TAILS
                        Text(
                            text = "Choose your side:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = AppColors.TextNavy
                        )

                        Spacer(modifier = Modifier.height(AppSpacing.sm))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                        ) {
                            CoinSide.entries.forEach { side ->
                                val isSelected = uiState.selectedSide == side
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(enabled = !uiState.isFlipping) {
                                            viewModel.selectSide(side)
                                        }
                                        .testTag("side_choice_${side.name.lowercase()}"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) AppColors.Primary else AppColors.SurfaceVariant,
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, AppColors.SurfaceBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = if (side == CoinSide.HEADS) Icons.Filled.Stars else Icons.Filled.MonetizationOn,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else AppColors.TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = side.displayName.uppercase(),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) Color.White else AppColors.TextNavy
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.lg))

                        // Toss Button
                        AppPrimaryButton(
                            text = if (uiState.isFlipping) "Flipping Coin..." else "TOSS COIN",
                            onClick = { viewModel.tossCoin() },
                            enabled = !uiState.isFlipping && uiState.dailyStats.attemptsRemainingToday > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("toss_coin_button")
                        )
                    }
                }
            }

            // AdMob Extra Toss Preparation (Coming Soon)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AppColors.SurfaceLight
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SmartDisplay,
                                contentDescription = null,
                                tint = AppColors.AccentPurple,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.sm))
                            Column {
                                Text(
                                    text = "Watch Ad + Extra Toss",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AppColors.TextNavy
                                )
                                Text(
                                    text = "Get +1 bonus flip after daily quota",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AppColors.SurfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = AppColors.TextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "COMING SOON",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AppColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Ad Banner Slot
            item {
                AdBannerContainer(placement = AdPlacement.BANNER_GAMES)
            }

            // Coin Toss History
            item {
                val coinTossTxs = gameHistory.filter { it.type == TransactionType.COIN_TOSS_REWARD }
                GameHistorySection(
                    gameTitle = "Coin Toss",
                    transactions = coinTossTxs
                )
            }
        }

        // Result Modal
        if (uiState.showResultModal && uiState.lastResult != null) {
            val result = uiState.lastResult!!
            GameResultModal(
                title = if (result.isWin) "🎉 You Won!" else "Better Luck Next Time!",
                subtitle = if (result.isWin) {
                    "Outcome was ${result.outcome.displayName}! You predicted correctly."
                } else {
                    "Outcome was ${result.outcome.displayName}. Keep trying to win coins!"
                },
                coinsAwarded = result.coinsAwarded,
                newBalance = result.newBalance,
                isVictory = result.isWin,
                canPlayAgain = result.attemptsRemainingToday > 0,
                onPlayAgain = {
                    viewModel.dismissResultModal()
                    viewModel.tossCoin()
                },
                onBackToPlay = {
                    viewModel.dismissResultModal()
                }
            )
        }
    }
}
