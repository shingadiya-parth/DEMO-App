package com.example.ui.screens.tictactoe

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.TicTacToeConfig
import com.example.core.config.TicTacToeMark
import com.example.core.config.TicTacToeOutcome
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicTacToeScreen(
    viewModel: TicTacToeViewModel,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val coinBalance by viewModel.coinBalance.collectAsState()
    val gameHistory by viewModel.gameHistory.collectAsState()

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
            .testTag("tictactoe_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tic-Tac-Toe",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.TextNavy
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("tictactoe_back_button")
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
                .testTag("tictactoe_content"),
            contentPadding = PaddingValues(horizontal = AppSpacing.screenHorizontal, vertical = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            // Daily Limit Card
            item {
                GameLimitCard(
                    usedCount = uiState.dailyStats.matchesUsedToday,
                    limit = uiState.dailyStats.dailyLimit,
                    gameLabel = "Matches"
                )
            }

            // Game Board Card
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
                        // Turn Status Indicator
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (uiState.isAiThinking) AppColors.SurfaceVariant else AppColors.Primary.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AppSpacing.sm, horizontal = AppSpacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (uiState.isAiThinking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = AppColors.Primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Computer is thinking (O)...",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = AppColors.TextSecondary
                                    )
                                } else {
                                    Text(
                                        text = when (uiState.outcome) {
                                            TicTacToeOutcome.IN_PROGRESS -> "Your Turn: Tap an empty cell (X)"
                                            TicTacToeOutcome.WIN -> "🎉 Match Finished: You Won!"
                                            TicTacToeOutcome.LOSS -> "Match Finished: Computer Won"
                                            TicTacToeOutcome.DRAW -> "Match Finished: Draw"
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (uiState.outcome == TicTacToeOutcome.WIN) AppColors.SuccessGreenDark else AppColors.TextNavy
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.lg))

                        // 3x3 Grid
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .aspectRatio(1f)
                                .background(AppColors.SurfaceVariant, RoundedCornerShape(16.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (row in 0..2) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (col in 0..2) {
                                        val index = row * 3 + col
                                        val mark = uiState.board[index]
                                        val isWinningCell = uiState.winningLine?.contains(index) == true

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    when {
                                                        isWinningCell -> AppColors.SuccessGreenLight
                                                        mark != TicTacToeMark.EMPTY -> Color.White
                                                        else -> Color.White.copy(alpha = 0.9f)
                                                    }
                                                )
                                                .clickable(
                                                    enabled = mark == TicTacToeMark.EMPTY &&
                                                            !uiState.isAiThinking &&
                                                            uiState.outcome == TicTacToeOutcome.IN_PROGRESS
                                                ) {
                                                    viewModel.onCellClicked(index)
                                                }
                                                .testTag("cell_$index"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            when (mark) {
                                                TicTacToeMark.X -> {
                                                    Icon(
                                                        imageVector = Icons.Filled.Close,
                                                        contentDescription = "X",
                                                        tint = AppColors.Primary,
                                                        modifier = Modifier.size(44.dp)
                                                    )
                                                }
                                                TicTacToeMark.O -> {
                                                    Icon(
                                                        imageVector = Icons.Filled.RadioButtonUnchecked,
                                                        contentDescription = "O",
                                                        tint = AppColors.AccentCoral,
                                                        modifier = Modifier.size(40.dp)
                                                    )
                                                }
                                                TicTacToeMark.EMPTY -> {}
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.lg))

                        // Restart/New Game button
                        OutlinedButton(
                            onClick = { viewModel.startNewMatch() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("tictactoe_new_game_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = AppColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NEW MATCH",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AppColors.Primary
                            )
                        }
                    }
                }
            }

            // AdMob Extra Match Preparation (Coming Soon)
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
                                    text = "Watch Ad + Extra Match",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AppColors.TextNavy
                                )
                                Text(
                                    text = "Get +1 bonus match after daily quota",
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

            // Tic-Tac-Toe History
            item {
                val tttTxs = gameHistory.filter { it.type == TransactionType.TIC_TAC_TOE_REWARD }
                GameHistorySection(
                    gameTitle = "Tic-Tac-Toe",
                    transactions = tttTxs
                )
            }
        }

        // Result Modal
        if (uiState.showResultModal && uiState.lastCompletedState != null) {
            val state = uiState.lastCompletedState!!
            GameResultModal(
                title = when (state.outcome) {
                    TicTacToeOutcome.WIN -> "🎉 You Won!"
                    TicTacToeOutcome.DRAW -> "It's a Draw!"
                    TicTacToeOutcome.LOSS -> "Good Game!"
                    else -> "Match Over"
                },
                subtitle = when (state.outcome) {
                    TicTacToeOutcome.WIN -> "Awesome strategy! You defeated the AI engine."
                    TicTacToeOutcome.DRAW -> "Well matched! Both players defended successfully."
                    TicTacToeOutcome.LOSS -> "The AI got the winning line this time. Try again!"
                    else -> ""
                },
                coinsAwarded = state.coinsAwarded,
                newBalance = state.newBalance ?: coinBalance,
                isVictory = state.outcome == TicTacToeOutcome.WIN,
                isDraw = state.outcome == TicTacToeOutcome.DRAW,
                canPlayAgain = state.matchesRemainingToday > 0,
                onPlayAgain = {
                    viewModel.dismissResultModal()
                    viewModel.startNewMatch()
                },
                onBackToPlay = {
                    viewModel.dismissResultModal()
                }
            )
        }
    }
}
