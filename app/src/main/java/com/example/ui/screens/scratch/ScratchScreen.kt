package com.example.ui.screens.scratch

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.ScratchGameConfig
import com.example.core.config.ScratchRewardTier
import com.example.data.model.CoinTransaction
import com.example.domain.engine.CoinConversionHelper
import com.example.services.ads.AdPlacement
import com.example.ui.components.AdBannerContainer
import com.example.ui.components.AdRewardConfirmationDialog
import com.example.ui.components.AppCard
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
fun ScratchScreen(
    viewModel: ScratchViewModel,
    onBack: () -> Unit = {},
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val coinBalance by viewModel.coinBalance.collectAsState()
    val scratchHistory by viewModel.scratchHistory.collectAsState()

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
            .testTag("scratch_screen_content"),
        contentPadding = PaddingValues(bottom = AppSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Header Bar
        item {
            ScratchTopBar(
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

        // 2. Main Scratch Card Area
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
                        text = "🎁 Scratch & Reveal",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.TextNavy
                    )
                    Text(
                        text = "Scratch the card surface to uncover guaranteed NestCoins",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    // Daily Quota Card
                    ScratchQuotaCard(
                        scratchesUsed = uiState.dailyStats.scratchesUsedToday,
                        dailyLimit = uiState.dailyStats.dailyLimit,
                        scratchesRemaining = uiState.dailyStats.scratchesRemainingToday
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    // Main Scratch Surface Component
                    ScratchCardSurface(
                        stage = uiState.stage,
                        currentTier = uiState.currentTier,
                        revealedPercent = uiState.revealedPercent,
                        coinsAwarded = uiState.coinsAwarded,
                        newBalance = uiState.newBalance,
                        isLoading = uiState.isLoadingSession,
                        scratchesRemaining = uiState.dailyStats.scratchesRemainingToday,
                        onScratchProgress = { percent -> viewModel.onScratchProgress(percent) },
                        onNextCard = { viewModel.onNextScratchCard() }
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    // Accessible Alternative Action: "Reveal Card"
                    if ((uiState.stage == ScratchStage.READY || uiState.stage == ScratchStage.SCRATCHING) &&
                        uiState.dailyStats.scratchesRemainingToday > 0 &&
                        uiState.scratchSession != null
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.onAccessibleRevealClick() },
                            shape = RoundedCornerShape(AppRadius.button),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.sm)
                                .testTag("accessible_reveal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AppColors.Primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Quick Reveal",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.Primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                    // Rewarded Ad Extra Reward Button
                    RewardedAdButton(
                        title = "Watch Video for +25 Coins",
                        rewardBadge = "+25 Coins",
                        isLoading = uiState.isAdLoading,
                        onClick = { viewModel.onExtraAdScratchClick() },
                        modifier = Modifier.padding(horizontal = AppSpacing.sm)
                    )
                }
            }
        }

        // 3. Scratch History Section
        item {
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            ScratchHistoryHeader()
        }

        if (scratchHistory.isEmpty()) {
            item {
                ScratchHistoryEmptyState()
            }
        } else {
            items(scratchHistory.take(10)) { tx ->
                ScratchHistoryItem(transaction = tx)
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
fun ScratchTopBar(
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
                modifier = Modifier.testTag("scratch_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AppColors.TextNavy
                )
            }
            Spacer(modifier = Modifier.width(AppSpacing.xxs))
            Text(
                text = "Scratch & Reveal",
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
fun ScratchQuotaCard(
    scratchesUsed: Int,
    dailyLimit: Int,
    scratchesRemaining: Int
) {
    Surface(
        shape = RoundedCornerShape(AppRadius.card),
        color = if (scratchesRemaining > 0) AppColors.AccentPurpleLight else AppColors.ErrorRedLight,
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
                        tint = if (scratchesRemaining > 0) AppColors.AccentPurple else AppColors.ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(
                        text = "Today's Scratch Cards",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.TextNavy
                    )
                }

                Text(
                    text = "$scratchesRemaining / $dailyLimit remaining",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (scratchesRemaining > 0) AppColors.AccentPurpleDark else AppColors.ErrorRed
                    )
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            val progress = (scratchesRemaining.toFloat() / dailyLimit.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (scratchesRemaining > 0) AppColors.AccentPurple else AppColors.ErrorRed,
                trackColor = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(AppSpacing.xxs))

            Text(
                text = "Resets daily at 00:00 • 5 free cards per day",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary
            )
        }
    }
}

/**
 * Interactive Scratch Card Canvas with Hardware-Accelerated Bitmask Clear.
 */
@Composable
fun ScratchCardSurface(
    stage: ScratchStage,
    currentTier: ScratchRewardTier?,
    revealedPercent: Float,
    coinsAwarded: Long,
    newBalance: Long,
    isLoading: Boolean,
    scratchesRemaining: Int,
    onScratchProgress: (Float) -> Unit,
    onNextCard: () -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var scratchBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scratchCanvas by remember { mutableStateOf<AndroidCanvas?>(null) }
    var totalCells by remember { mutableStateOf(100) }
    val scratchedCells = remember { mutableSetOf<Int>() }

    val erasePaint = remember {
        AndroidPaint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            isAntiAlias = true
            strokeWidth = 70f
            style = AndroidPaint.Style.STROKE
            strokeJoin = AndroidPaint.Join.ROUND
            strokeCap = AndroidPaint.Cap.ROUND
        }
    }

    // Initialize or reset scratch overlay bitmap
    LaunchedEffect(size, currentTier?.rewardId) {
        if (size.width > 0 && size.height > 0) {
            val bmp = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
            val cvs = AndroidCanvas(bmp)

            // Draw premium metallic/silver scratch pattern
            val bgPaint = AndroidPaint().apply {
                isAntiAlias = true
            }

            // Silver gradient background
            val gradient = android.graphics.LinearGradient(
                0f, 0f, size.width.toFloat(), size.height.toFloat(),
                intArrayOf(
                    android.graphics.Color.parseColor("#94A3B8"),
                    android.graphics.Color.parseColor("#CBD5E1"),
                    android.graphics.Color.parseColor("#64748B"),
                    android.graphics.Color.parseColor("#E2E8F0")
                ),
                null,
                android.graphics.Shader.TileMode.CLAMP
            )
            bgPaint.shader = gradient
            cvs.drawRect(0f, 0f, size.width.toFloat(), size.height.toFloat(), bgPaint)

            // Decorative gold border on scratch surface
            val borderPaint = AndroidPaint().apply {
                color = android.graphics.Color.parseColor("#F59E0B")
                style = AndroidPaint.Style.STROKE
                strokeWidth = 6f
                isAntiAlias = true
            }
            cvs.drawRoundRect(6f, 6f, size.width.toFloat() - 6f, size.height.toFloat() - 6f, 16f, 16f, borderPaint)

            // Text instruction on the scratch layer
            val textPaint = AndroidPaint().apply {
                color = android.graphics.Color.parseColor("#1E293B")
                textSize = 38f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                textAlign = AndroidPaint.Align.CENTER
                isAntiAlias = true
            }
            cvs.drawText("🎁 SCRATCH HERE 🎁", size.width / 2f, size.height / 2f - 10f, textPaint)

            val subPaint = AndroidPaint().apply {
                color = android.graphics.Color.parseColor("#475569")
                textSize = 26f
                textAlign = AndroidPaint.Align.CENTER
                isAntiAlias = true
            }
            cvs.drawText("Swipe to reveal reward", size.width / 2f, size.height / 2f + 36f, subPaint)

            scratchBitmap = bmp
            scratchCanvas = cvs
            scratchedCells.clear()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            scratchBitmap?.recycle()
            scratchBitmap = null
            scratchCanvas = null
        }
    }

    // Card Container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = AppSpacing.sm)
            .clip(RoundedCornerShape(AppRadius.largeCard))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7))
                )
            )
            .border(2.dp, AppColors.GoldCoin.copy(alpha = 0.6f), RoundedCornerShape(AppRadius.largeCard))
            .onSizeChanged { size = it }
            .testTag("scratch_surface_container"),
        contentAlignment = Alignment.Center
    ) {
        // UNDERNEATH REWARD CONTENT (Visible after or during scratch)
        if (currentTier != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppSpacing.md)
                    .testTag("scratch_reward_underneath"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (stage == ScratchStage.REVEALED || stage == ScratchStage.ALREADY_COMPLETED) {
                    // 🎉 Celebratory Won State
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(AppColors.GoldCoin, AppColors.GoldCoinDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentTier.iconEmoji,
                            fontSize = 28.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                    Text(
                        text = "🎉 YOU WON!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.TextNavy
                    )

                    Text(
                        text = "+${currentTier.rewardAmount} NestCoins",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = AppColors.Primary
                        ),
                        modifier = Modifier.testTag("scratch_coins_won_text")
                    )

                    if (newBalance > 0) {
                        Text(
                            text = "New Balance: ${NumberFormat.getNumberInstance(Locale.US).format(newBalance)} Coins",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = AppColors.TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                    if (scratchesRemaining > 0) {
                        Button(
                            onClick = onNextCard,
                            shape = RoundedCornerShape(AppRadius.button),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("next_scratch_card_button")
                        ) {
                            Text("Next Scratch Card (${scratchesRemaining} left)", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Text(
                            text = "No cards remaining today",
                            style = MaterialTheme.typography.labelSmall.copy(color = AppColors.TextMuted)
                        )
                    }
                } else {
                    // Revealed reward background during scratching
                    Text(
                        text = currentTier.iconEmoji,
                        fontSize = 36.sp
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.xxs))
                    Text(
                        text = "+${currentTier.rewardAmount}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = AppColors.Primary
                        )
                    )
                    Text(
                        text = "NestCoins",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppColors.GoldCoinDark
                        )
                    )
                    Text(
                        text = "Keep scratching to claim (${(revealedPercent * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
        } else if (scratchesRemaining <= 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = AppColors.TextMuted,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = "No Scratch Cards Left Today",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.TextNavy
                )
                Text(
                    text = "You've used all 5 free cards today. Resets at midnight!",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else if (isLoading) {
            CircularProgressIndicator(
                color = AppColors.Primary,
                modifier = Modifier.size(32.dp)
            )
        }

        // TOP SCRATCH OVERLAY CANVAS (Gesture drag eraser)
        if (stage != ScratchStage.REVEALED && stage != ScratchStage.ALREADY_COMPLETED && scratchesRemaining > 0) {
            var lastTouchPoint by remember { mutableStateOf<Offset?>(null) }

            scratchBitmap?.let { bmp ->
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    lastTouchPoint = offset
                                    scratchCanvas?.let { cvs ->
                                        cvs.drawCircle(offset.x, offset.y, 35f, erasePaint)
                                        // Track grid cells
                                        val cellX = (offset.x / (size.width / 10f)).toInt().coerceIn(0, 9)
                                        val cellY = (offset.y / (size.height / 10f)).toInt().coerceIn(0, 9)
                                        scratchedCells.add(cellY * 10 + cellX)
                                        val pct = scratchedCells.size.toFloat() / 100f
                                        onScratchProgress(pct)
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val currentPoint = change.position
                                    scratchCanvas?.let { cvs ->
                                        lastTouchPoint?.let { last ->
                                            cvs.drawLine(last.x, last.y, currentPoint.x, currentPoint.y, erasePaint)
                                        }
                                        cvs.drawCircle(currentPoint.x, currentPoint.y, 35f, erasePaint)

                                        val cellX = (currentPoint.x / (size.width / 10f)).toInt().coerceIn(0, 9)
                                        val cellY = (currentPoint.y / (size.height / 10f)).toInt().coerceIn(0, 9)
                                        scratchedCells.add(cellY * 10 + cellX)
                                        val pct = scratchedCells.size.toFloat() / 100f
                                        onScratchProgress(pct)
                                    }
                                    lastTouchPoint = currentPoint
                                },
                                onDragEnd = {
                                    lastTouchPoint = null
                                }
                            )
                        }
                        .testTag("scratch_interactive_canvas")
                ) {
                    drawImage(bmp.asImageBitmap())
                }
            }
        }
    }
}

@Composable
fun ExtraScratchAdButton(
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(AppRadius.button),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.sm)
            .testTag("watch_ad_extra_scratch_button")
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
                text = "WATCH AD + EXTRA SCRATCH",
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
fun ScratchHistoryHeader() {
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
            text = "Scratch History",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = AppColors.TextNavy
        )
    }
}

@Composable
fun ScratchHistoryEmptyState() {
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
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = AppColors.TextMuted,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = "No scratch cards revealed yet",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = AppColors.TextSecondary
            )
            Text(
                text = "Scratch your card above to uncover free rewards!",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted
            )
        }
    }
}

@Composable
fun ScratchHistoryItem(transaction: CoinTransaction) {
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
                        .background(AppColors.AccentPurpleLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎁",
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                Column {
                    Text(
                        text = "Scratch & Reveal",
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
