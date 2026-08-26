package com.example.ui.screens.puzzle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.ClientPuzzleQuestion
import com.example.core.config.PuzzleCategory
import com.example.core.config.PuzzleConfig
import com.example.core.config.PuzzleDifficulty
import com.example.data.model.CoinTransaction
import com.example.services.ads.AdPlacement
import com.example.ui.components.AdBannerContainer
import com.example.ui.components.AdRewardConfirmationDialog
import com.example.ui.components.AppCard
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.RewardedAdButton
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppElevation
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PuzzleScreen(
    viewModel: PuzzleViewModel,
    onBack: () -> Unit = {},
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val coinBalance by viewModel.coinBalance.collectAsState()
    val puzzleHistory by viewModel.puzzleHistory.collectAsState()

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BackgroundLight)
            .testTag("puzzle_screen_content"),
        contentPadding = PaddingValues(bottom = AppSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Header Bar
        item {
            PuzzleTopBar(
                coinBalance = coinBalance,
                onBack = onBack
            )
        }

        // Ad Banner Slot
        item {
            AdBannerContainer(
                placement = AdPlacement.BANNER_GAMES,
                modifier = Modifier.padding(bottom = AppSpacing.xs)
            )
        }

        // 2. Main Puzzle & Status Area
        item {
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenHorizontal),
                backgroundColor = AppColors.SurfaceLight
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header label
                    Text(
                        text = "🧠 Brain Puzzle",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.TextNavy
                    )
                    Text(
                        text = "Test your skills with daily trivia, logic, and math puzzles",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    // Daily Quota Card
                    PuzzleQuotaCard(
                        puzzlesCompleted = uiState.dailyStats.puzzlesCompletedToday,
                        dailyLimit = uiState.dailyStats.dailyLimit,
                        puzzlesRemaining = uiState.dailyStats.puzzlesRemainingToday
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    // Main Content depending on stage
                    when (uiState.stage) {
                        PuzzleUiStage.LOADING -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = AppColors.Primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        PuzzleUiStage.LIMIT_REACHED -> {
                            PuzzleLimitReachedCard()
                        }
                        PuzzleUiStage.ERROR -> {
                            PuzzleErrorCard(
                                message = uiState.errorMessage ?: "Failed to load puzzle",
                                onRetry = { viewModel.loadNextPuzzle() }
                            )
                        }
                        PuzzleUiStage.QUESTION_READY, PuzzleUiStage.SUBMITTING -> {
                            uiState.question?.let { question ->
                                // Timer Bar
                                if (uiState.isTimerRunning || uiState.timeRemainingSeconds > 0) {
                                    PuzzleTimerBar(
                                        remainingSeconds = uiState.timeRemainingSeconds,
                                        totalSeconds = uiState.totalTimeSeconds
                                    )
                                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                                }

                                // Question Card
                                QuestionDisplayCard(
                                    question = question,
                                    selectedOptionIndex = uiState.selectedOptionIndex,
                                    onSelectOption = { index -> viewModel.selectOption(index) },
                                    isSubmitting = uiState.isSubmitting
                                )

                                Spacer(modifier = Modifier.height(AppSpacing.lg))

                                // Submit Button
                                AppPrimaryButton(
                                    text = if (uiState.isSubmitting) "Evaluating Answer..." else "Submit Answer",
                                    onClick = { viewModel.submitAnswer(isTimeout = false) },
                                    enabled = uiState.selectedOptionIndex != null && !uiState.isSubmitting,
                                    isLoading = uiState.isSubmitting,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = AppSpacing.xs)
                                        .testTag("puzzle_submit_button")
                                )
                            }
                        }
                        PuzzleUiStage.RESULT_CORRECT, PuzzleUiStage.RESULT_INCORRECT, PuzzleUiStage.RESULT_EXPIRED -> {
                            uiState.question?.let { question ->
                                PuzzleResultCard(
                                    stage = uiState.stage,
                                    question = question,
                                    selectedOptionIndex = uiState.selectedOptionIndex,
                                    correctAnswerIndex = uiState.correctAnswerIndex ?: 0,
                                    explanation = uiState.explanation ?: "",
                                    coinsWon = uiState.coinsWon,
                                    newBalance = uiState.newBalance,
                                    puzzlesRemaining = uiState.dailyStats.puzzlesRemainingToday,
                                    onNextPuzzle = { viewModel.loadNextPuzzle() }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    // Rewarded Ad Extra Reward Button
                    RewardedAdButton(
                        title = "Watch Video for +25 Coins",
                        rewardBadge = "+25 Coins",
                        isLoading = uiState.isAdLoading,
                        onClick = { viewModel.onExtraAdPuzzleClick() },
                        modifier = Modifier.padding(horizontal = AppSpacing.sm)
                    )
                }
            }
        }

        // 3. Puzzle History Section
        item {
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            PuzzleHistoryHeader()
        }

        if (puzzleHistory.isEmpty()) {
            item {
                PuzzleHistoryEmptyState()
            }
        } else {
            items(puzzleHistory.take(10)) { tx ->
                PuzzleHistoryItem(transaction = tx)
            }
        }
    }

    // Celebratory Rewarded Ad Confirmation Dialog
    uiState.adRewardDialog?.let { adReward ->
        AdRewardConfirmationDialog(
            rewardTitle = "Bonus video reward credited to your NestCoin wallet.",
            rewardAmount = adReward.coinsGranted,
            newBalance = adReward.newBalance,
            onDismiss = { viewModel.dismissAdRewardDialog() }
        )
    }
}

@Composable
fun PuzzleTopBar(
    coinBalance: Long,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("puzzle_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AppColors.TextNavy
                )
            }
            Spacer(modifier = Modifier.width(AppSpacing.xxs))
            Text(
                text = "Brain Puzzle",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = AppColors.TextNavy
            )
        }

        // Balance Chip
        Surface(
            shape = RoundedCornerShape(AppRadius.pill),
            color = AppColors.SurfaceLight,
            shadowElevation = AppElevation.subtle,
            modifier = Modifier.padding(end = AppSpacing.xs)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.MonetizationOn,
                    contentDescription = "Coins",
                    tint = AppColors.GoldCoin,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.xxs))
                Text(
                    text = "${NumberFormat.getNumberInstance(Locale.US).format(coinBalance)} Coins",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextNavy
                    )
                )
            }
        }
    }
}

@Composable
fun PuzzleQuotaCard(
    puzzlesCompleted: Int,
    dailyLimit: Int,
    puzzlesRemaining: Int
) {
    Surface(
        shape = RoundedCornerShape(AppRadius.card),
        color = if (puzzlesRemaining > 0) AppColors.PrimaryLight else AppColors.ErrorRedLight,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.sm)
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AvTimer,
                        contentDescription = null,
                        tint = if (puzzlesRemaining > 0) AppColors.Primary else AppColors.ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(
                        text = "Today's Puzzles",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.TextNavy
                    )
                }

                Text(
                    text = "$puzzlesRemaining / $dailyLimit remaining",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (puzzlesRemaining > 0) AppColors.PrimaryDark else AppColors.ErrorRed
                    )
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            val progress = (puzzlesRemaining.toFloat() / dailyLimit.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (puzzlesRemaining > 0) AppColors.Primary else AppColors.ErrorRed,
                trackColor = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(AppSpacing.xxs))

            Text(
                text = "Resets daily at 00:00 • 5 free puzzles per day",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
fun PuzzleTimerBar(
    remainingSeconds: Int,
    totalSeconds: Int
) {
    val progress = (remainingSeconds.toFloat() / totalSeconds.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
    val timerColor = when {
        remainingSeconds <= 5 -> AppColors.ErrorRed
        remainingSeconds <= 10 -> AppColors.GoldCoinDark
        else -> AppColors.SuccessGreen
    }

    Surface(
        shape = RoundedCornerShape(AppRadius.pill),
        color = timerColor.copy(alpha = 0.15f),
        modifier = Modifier.padding(horizontal = AppSpacing.sm)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Timer,
                contentDescription = "Timer",
                tint = timerColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$remainingSeconds s",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = timerColor
                )
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(100.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = timerColor,
                trackColor = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun QuestionDisplayCard(
    question: ClientPuzzleQuestion,
    selectedOptionIndex: Int?,
    onSelectOption: (Int) -> Unit,
    isSubmitting: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.sm)
    ) {
        // Badges: Category & Difficulty
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(AppRadius.pill),
                color = AppColors.AccentPurpleLight
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = question.category.iconEmoji, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = question.category.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppColors.AccentPurpleDark
                        )
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(AppRadius.pill),
                color = when (question.difficulty) {
                    PuzzleDifficulty.EASY -> AppColors.SuccessGreenLight
                    PuzzleDifficulty.MEDIUM -> AppColors.GoldCoinLight
                    PuzzleDifficulty.HARD -> AppColors.ErrorRedLight
                }
            ) {
                Text(
                    text = "${question.difficulty.displayName} • +${question.rewardAmount} Coins",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = when (question.difficulty) {
                            PuzzleDifficulty.EASY -> AppColors.SuccessGreen
                            PuzzleDifficulty.MEDIUM -> AppColors.GoldCoinDark
                            PuzzleDifficulty.HARD -> AppColors.ErrorRed
                        }
                    ),
                    modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.md))

        // Question Statement
        Surface(
            shape = RoundedCornerShape(AppRadius.card),
            color = Color(0xFFF8FAFC),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = question.question,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp
                ),
                color = AppColors.TextNavy,
                modifier = Modifier.padding(AppSpacing.md),
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.md))

        // Multiple-Choice Options (A, B, C, D)
        val optionLabels = listOf("A", "B", "C", "D", "E", "F")

        question.options.forEachIndexed { index, optionText ->
            val isSelected = selectedOptionIndex == index
            val label = optionLabels.getOrElse(index) { "${index + 1}" }

            Surface(
                shape = RoundedCornerShape(AppRadius.card),
                color = if (isSelected) AppColors.PrimaryLight else AppColors.SurfaceLight,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) AppColors.Primary else Color(0xFFE2E8F0)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable(enabled = !isSubmitting) { onSelectOption(index) }
                    .testTag("puzzle_option_$index")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circle Badge (A, B, C, D)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) AppColors.Primary else Color(0xFFEDF2F7)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else AppColors.TextNavy
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(AppSpacing.md))

                    Text(
                        text = optionText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = AppColors.TextNavy
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun PuzzleResultCard(
    stage: PuzzleUiStage,
    question: ClientPuzzleQuestion,
    selectedOptionIndex: Int?,
    correctAnswerIndex: Int,
    explanation: String,
    coinsWon: Long,
    newBalance: Long,
    puzzlesRemaining: Int,
    onNextPuzzle: () -> Unit
) {
    val isCorrect = stage == PuzzleUiStage.RESULT_CORRECT
    val isExpired = stage == PuzzleUiStage.RESULT_EXPIRED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Result Banner
        Surface(
            shape = RoundedCornerShape(AppRadius.card),
            color = if (isCorrect) AppColors.SuccessGreenLight else if (isExpired) AppColors.GoldCoinLight else AppColors.ErrorRedLight,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(AppSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isCorrect) "🎉" else if (isExpired) "⏰" else "❌",
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.height(AppSpacing.xxs))
                Text(
                    text = if (isCorrect) "Correct!" else if (isExpired) "Time's Up!" else "Not quite!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isCorrect) AppColors.SuccessGreen else if (isExpired) AppColors.GoldCoinDark else AppColors.ErrorRed
                )

                if (isCorrect) {
                    Spacer(modifier = Modifier.height(AppSpacing.xxs))
                    Text(
                        text = "+$coinsWon NestCoins",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = AppColors.Primary
                        ),
                        modifier = Modifier.testTag("puzzle_coins_won_text")
                    )
                    if (newBalance > 0) {
                        Text(
                            text = "New Balance: ${NumberFormat.getNumberInstance(Locale.US).format(newBalance)} Coins",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = AppColors.TextSecondary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(AppSpacing.xxs))
                    Text(
                        text = "0 NestCoins",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextMuted
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.md))

        // Explanation Card
        Surface(
            shape = RoundedCornerShape(AppRadius.card),
            color = Color(0xFFF1F5F9),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(AppSpacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "💡", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(
                        text = "Correct Answer & Explanation",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextNavy
                        )
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.xs))

                val correctText = question.options.getOrElse(correctAnswerIndex) { "Option ${correctAnswerIndex + 1}" }
                Text(
                    text = "Answer: $correctText",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppColors.SuccessGreen
                    )
                )

                if (explanation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        // Next Puzzle CTA
        if (puzzlesRemaining > 0) {
            Button(
                onClick = onNextPuzzle,
                shape = RoundedCornerShape(AppRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("next_puzzle_button")
            ) {
                Text(
                    text = "Next Puzzle ($puzzlesRemaining left today)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(AppRadius.card),
                color = AppColors.SurfaceLight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "You've finished all free puzzles for today! Resets at midnight.",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(AppSpacing.md)
                )
            }
        }
    }
}

@Composable
fun PuzzleLimitReachedCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = AppColors.TextMuted,
            modifier = Modifier.size(42.dp)
        )
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        Text(
            text = "Today's Puzzles are Complete",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = AppColors.TextNavy
        )
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Text(
            text = "You have completed all 5 free daily puzzles. Come back tomorrow for new challenges!",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PuzzleErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = AppColors.ErrorRed,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = AppColors.TextNavy,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AppSpacing.md))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(AppRadius.button),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
        ) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Retry")
        }
    }
}

@Composable
fun ExtraPuzzleAdButton(
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(AppRadius.button),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.sm)
            .testTag("watch_ad_extra_puzzle_button")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.OndemandVideo,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            Text(
                text = "WATCH AD + EXTRA PUZZLE",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Primary
                )
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = AppColors.GoldCoinLight
            ) {
                Text(
                    text = "COMING SOON",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AppColors.GoldCoinDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun PuzzleHistoryHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenHorizontal),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = null,
            tint = AppColors.Primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(AppSpacing.xs))
        Text(
            text = "Puzzle History",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = AppColors.TextNavy
        )
    }
}

@Composable
fun PuzzleHistoryEmptyState() {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenHorizontal, vertical = AppSpacing.xs),
        backgroundColor = AppColors.SurfaceLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Psychology,
                contentDescription = null,
                tint = AppColors.TextMuted,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = "No puzzle rewards yet",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = AppColors.TextSecondary
            )
            Text(
                text = "Solve questions above to earn free NestCoins!",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted
            )
        }
    }
}

@Composable
fun PuzzleHistoryItem(transaction: CoinTransaction) {
    val dateStr = formatTimestamp(transaction.createdAt)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenHorizontal, vertical = 4.dp),
        shape = RoundedCornerShape(AppRadius.card),
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.subtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppColors.PrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🧠",
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                Column {
                    Text(
                        text = transaction.metadata ?: "Brain Puzzle",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.TextNavy
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextMuted
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${transaction.amount} Coins",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppColors.SuccessGreen
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = AppColors.SuccessGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Credited",
                        style = MaterialTheme.typography.labelSmall.copy(color = AppColors.SuccessGreen, fontSize = 10.sp)
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val dayMillis = 24 * 60 * 60 * 1000L

    val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
    val timeStr = timeFormat.format(Date(timestamp))

    return when {
        diff < dayMillis -> "Today, $timeStr"
        diff < 2 * dayMillis -> "Yesterday, $timeStr"
        else -> {
            val dateFormat = SimpleDateFormat("d MMM, h:mm a", Locale.US)
            dateFormat.format(Date(timestamp))
        }
    }
}
