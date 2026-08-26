package com.example.ui.screens.bubble

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.BubblePopConfig
import com.example.data.model.TransactionType
import com.example.domain.engine.CoinConversionHelper
import com.example.services.ads.AdPlacement
import com.example.ui.components.AdBannerContainer
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.GameHistorySection
import com.example.ui.components.GameLimitCard
import com.example.ui.components.GameResultModal
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppSpacing

private val bubbleGradients = listOf(
    listOf(Color(0xFF60A5FA), Color(0xFF2563EB)), // Vibrant Blue
    listOf(Color(0xFF34D399), Color(0xFF059669)), // Emerald
    listOf(Color(0xFFF472B6), Color(0xFFDB2777)), // Pink
    listOf(Color(0xFFFBBF24), Color(0xFFD97706)), // Amber
    listOf(Color(0xFFA78BFA), Color(0xFF7C3AED))  // Purple
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BubblePopScreen(
    viewModel: BubblePopViewModel,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val coinBalance by viewModel.coinBalance.collectAsState()
    val gameHistory by viewModel.gameHistory.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "bubble_float")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bubble_scale"
    )

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BackgroundLight)
            .testTag("bubble_pop_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bubble Pop",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.TextNavy
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("bubble_pop_back_button")
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
                .testTag("bubble_pop_content"),
            contentPadding = PaddingValues(horizontal = AppSpacing.screenHorizontal, vertical = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            // Daily Limit Card
            item {
                GameLimitCard(
                    usedCount = uiState.dailyStats.roundsUsedToday,
                    limit = uiState.dailyStats.dailyLimit,
                    gameLabel = "Rounds"
                )
            }

            // Game Arena Card
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
                            .padding(AppSpacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Score & Timer Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Score Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AppColors.Primary.copy(alpha = 0.08f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Score:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = AppColors.TextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${uiState.score} pts",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = AppColors.Primary
                                    )
                                }
                            }

                            // Timer Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (uiState.timeRemainingSeconds <= 5 && uiState.isPlaying) {
                                    AppColors.WarningAmberLight
                                } else {
                                    AppColors.SurfaceVariant
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Timer,
                                        contentDescription = null,
                                        tint = if (uiState.timeRemainingSeconds <= 5 && uiState.isPlaying) {
                                            AppColors.AccentCoral
                                        } else {
                                            AppColors.TextNavy
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${uiState.timeRemainingSeconds}s",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (uiState.timeRemainingSeconds <= 5 && uiState.isPlaying) {
                                                AppColors.AccentCoral
                                            } else {
                                                AppColors.TextNavy
                                            }
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.sm))

                        // Timer Progress Line
                        val timerProgress = (uiState.timeRemainingSeconds.toFloat() / BubblePopConfig.roundDurationSeconds).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { timerProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (uiState.timeRemainingSeconds <= 5) AppColors.AccentCoral else AppColors.Primary,
                            trackColor = AppColors.SurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(AppSpacing.md))

                        // Bubble Grid (4x4)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(AppColors.SurfaceVariant)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!uiState.isPlaying) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Pop as many bubbles as you can!",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = AppColors.TextNavy
                                    )
                                    Text(
                                        text = "30 seconds • 50+ pts = 10 Coins • 150+ pts = 50 Coins",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(AppSpacing.lg))
                                    AppPrimaryButton(
                                        text = if (uiState.isRoundStarting) "Starting..." else "START ROUND",
                                        onClick = { viewModel.startRound() },
                                        enabled = !uiState.isRoundStarting && uiState.dailyStats.roundsRemainingToday > 0,
                                        modifier = Modifier.width(200.dp)
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (row in 0..3) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            for (col in 0..3) {
                                                val bubbleIndex = row * 4 + col
                                                val bubble = uiState.bubbles.getOrNull(bubbleIndex)

                                                if (bubble != null) {
                                                    val gradient = bubbleGradients[bubble.colorIndex % bubbleGradients.size]
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .aspectRatio(1f)
                                                            .scale(floatAnim)
                                                            .clip(CircleShape)
                                                            .background(Brush.radialGradient(gradient))
                                                            .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                                            .clickable(
                                                                interactionSource = remember { MutableInteractionSource() },
                                                                indication = null
                                                            ) {
                                                                viewModel.onBubbleTapped(bubble.id)
                                                            }
                                                            .testTag("bubble_${bubble.id}"),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        // Inner shine highlight
                                                        Box(
                                                            modifier = Modifier
                                                                .size(12.dp)
                                                                .clip(CircleShape)
                                                                .background(Color.White.copy(alpha = 0.5f))
                                                                .align(Alignment.TopStart)
                                                                .padding(start = 6.dp, top = 6.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.md))

                        // Score Tier Target Indicator
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = AppColors.BackgroundLight
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AppSpacing.sm, horizontal = AppSpacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reward Tiers:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = AppColors.TextSecondary
                                )
                                Text(
                                    text = "50+: 10c | 100+: 25c | 150+: 50c",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = AppColors.Primary
                                )
                            }
                        }
                    }
                }
            }

            // AdMob Extra Round Preparation (Coming Soon)
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
                                    text = "Watch Ad + Extra Round",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AppColors.TextNavy
                                )
                                Text(
                                    text = "Get +1 bonus round after daily quota",
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

            // Bubble Pop History
            item {
                val bubbleTxs = gameHistory.filter { it.type == TransactionType.BUBBLE_POP_REWARD }
                GameHistorySection(
                    gameTitle = "Bubble Pop",
                    transactions = bubbleTxs
                )
            }
        }

        // Result Modal
        if (uiState.showResultModal && uiState.lastResult != null) {
            val result = uiState.lastResult!!
            GameResultModal(
                title = "🎉 Round Complete!",
                subtitle = "You popped ${result.bubblesPopped} bubbles! Tier: ${result.tier.tierName}",
                score = result.finalScore,
                coinsAwarded = result.coinsAwarded,
                newBalance = result.newBalance,
                isVictory = result.coinsAwarded > 0,
                canPlayAgain = result.roundsRemainingToday > 0,
                onPlayAgain = {
                    viewModel.dismissResultModal()
                    viewModel.startRound()
                },
                onBackToPlay = {
                    viewModel.dismissResultModal()
                }
            )
        }
    }
}
