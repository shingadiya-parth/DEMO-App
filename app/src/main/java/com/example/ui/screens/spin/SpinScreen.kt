package com.example.ui.screens.spin

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.config.SpinRewardSegment
import com.example.data.model.CoinTransaction
import com.example.domain.engine.CoinConversionHelper
import com.example.domain.engine.SpinResult
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpinScreen(
    viewModel: SpinViewModel,
    onBack: () -> Unit = {},
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val coinBalance by viewModel.coinBalance.collectAsState()
    val spinHistory by viewModel.spinHistory.collectAsState()

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
            .testTag("spin_screen_content"),
        contentPadding = PaddingValues(bottom = AppSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Header Bar
        item {
            SpinTopBar(
                coinBalance = coinBalance,
                onBack = onBack
            )
        }

        // 2. Ad Banner Slot
        item {
            AdBannerContainer(
                placement = AdPlacement.BANNER_GAMES,
                modifier = Modifier.padding(bottom = AppSpacing.xs)
            )
        }

        // 3. Wheel Card Container
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
                    Text(
                        text = "Fortune Wheel",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.TextNavy
                    )
                    Text(
                        text = "Spin and win guaranteed NestCoins!",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    // Wheel Graphic & Animated Canvas
                    SpinWheel(
                        targetRotation = uiState.targetRotation,
                        segments = viewModel.segments
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    // Remaining Spins Status Indicator
                    SpinsQuotaCard(
                        spinsUsed = uiState.dailyStats.spinsUsedToday,
                        dailyLimit = uiState.dailyStats.dailyLimit,
                        spinsRemaining = uiState.dailyStats.spinsRemainingToday
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.lg))

                    // Main Spin Action Button
                    SpinActionButton(
                        isSpinning = uiState.isSpinning,
                        spinsRemaining = uiState.dailyStats.spinsRemainingToday,
                        onSpinClick = { viewModel.performSpin() }
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    // Rewarded Ad Extra Reward Button
                    RewardedAdButton(
                        title = "Watch Video for +25 Coins",
                        rewardBadge = "+25 Coins",
                        isLoading = uiState.isAdLoading,
                        onClick = { viewModel.onExtraAdSpinClick() },
                        modifier = Modifier.padding(horizontal = AppSpacing.sm)
                    )
                }
            }
        }

        // 4. Spin History Section
        item {
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            SpinHistoryHeader()
        }

        if (spinHistory.isEmpty()) {
            item {
                SpinHistoryEmptyState()
            }
        } else {
            items(spinHistory.take(10)) { tx ->
                SpinHistoryItem(transaction = tx)
            }
        }
    }

    // Celebratory Win Dialog Modal
    if (uiState.showWinDialog && uiState.winningResult != null) {
        SpinWinDialog(
            result = uiState.winningResult!!,
            onContinue = { viewModel.dismissWinDialog() }
        )
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
fun SpinTopBar(
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
                modifier = Modifier.testTag("spin_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AppColors.TextNavy
                )
            }
            Spacer(modifier = Modifier.width(AppSpacing.xxs))
            Text(
                text = "Spin & Win",
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
fun SpinWheel(
    targetRotation: Float,
    segments: List<SpinRewardSegment>
) {
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(
            durationMillis = 3500,
            easing = CubicBezierEasing(0.12f, 0.8f, 0.25f, 1f)
        ),
        label = "wheel_rotation_animation"
    )

    Box(
        modifier = Modifier
            .size(300.dp)
            .testTag("spin_wheel_box"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(280.dp)
                .rotate(animatedRotation)
        ) {
            val diameter = size.minDimension
            val radius = diameter / 2f
            val center = Offset(radius, radius)
            val segmentCount = segments.size
            val sectorAngle = 360f / segmentCount

            drawCircle(
                color = Color(0xFFE2E8F0),
                radius = radius,
                center = center
            )

            segments.forEachIndexed { index, segment ->
                val startAngle = index * sectorAngle
                drawArc(
                    color = Color(segment.colorHex),
                    startAngle = startAngle,
                    sweepAngle = sectorAngle,
                    useCenter = true,
                    size = Size(diameter, diameter)
                )
            }

            for (i in 0 until segmentCount) {
                val angleRad = Math.toRadians((i * sectorAngle).toDouble())
                val endX = (center.x + radius * cos(angleRad)).toFloat()
                val endY = (center.y + radius * sin(angleRad)).toFloat()
                drawLine(
                    color = Color.White.copy(alpha = 0.6f),
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            drawCircle(
                color = Color(0xFFF1F5F9),
                radius = radius - 3.dp.toPx(),
                center = center,
                style = Stroke(width = 6.dp.toPx())
            )

            drawIntoCanvas { canvas ->
                val textPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 14.sp.toPx()
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(120, 0, 0, 0))
                }

                segments.forEachIndexed { index, segment ->
                    val angleDeg = (index * sectorAngle) + (sectorAngle / 2f)
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val textRadius = radius * 0.68f
                    val textX = (center.x + textRadius * cos(angleRad)).toFloat()
                    val textY = (center.y + textRadius * sin(angleRad)).toFloat()

                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.rotate(angleDeg + 90f, textX, textY)
                    canvas.nativeCanvas.drawText(segment.label, textX, textY + 5.dp.toPx(), textPaint)
                    canvas.nativeCanvas.restore()
                }
            }

            drawCircle(
                color = Color.White,
                radius = 32.dp.toPx(),
                center = center
            )
        }

        Surface(
            modifier = Modifier.size(54.dp),
            shape = CircleShape,
            color = AppColors.SurfaceLight,
            shadowElevation = 6.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7))
                        )
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = AppColors.GoldCoin,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Canvas(
            modifier = Modifier
                .size(34.dp)
                .align(Alignment.TopCenter)
        ) {
            val path = Path().apply {
                moveTo(size.width / 2f, size.height)
                lineTo(0f, 0f)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(
                path = path,
                color = Color(0xFFF59E0B)
            )
            val innerPath = Path().apply {
                moveTo(size.width / 2f, size.height - 4.dp.toPx())
                lineTo(4.dp.toPx(), 3.dp.toPx())
                lineTo(size.width - 4.dp.toPx(), 3.dp.toPx())
                close()
            }
            drawPath(
                path = innerPath,
                color = Color(0xFFFCD34D)
            )
        }
    }
}

@Composable
fun SpinsQuotaCard(
    spinsUsed: Int,
    dailyLimit: Int,
    spinsRemaining: Int
) {
    Surface(
        shape = RoundedCornerShape(AppRadius.card),
        color = if (spinsRemaining > 0) AppColors.PrimaryLight else AppColors.ErrorRedLight,
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
                        tint = if (spinsRemaining > 0) AppColors.Primary else AppColors.ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(
                        text = "Daily Free Spins",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.TextNavy
                    )
                }

                Text(
                    text = "$spinsRemaining / $dailyLimit remaining",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (spinsRemaining > 0) AppColors.PrimaryDark else AppColors.ErrorRed
                    )
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            val progress = (spinsRemaining.toFloat() / dailyLimit.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (spinsRemaining > 0) AppColors.Primary else AppColors.ErrorRed,
                trackColor = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(AppSpacing.xxs))

            Text(
                text = "Resets daily at 00:00 • Authoritative server limit",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
fun SpinActionButton(
    isSpinning: Boolean,
    spinsRemaining: Int,
    onSpinClick: () -> Unit
) {
    val isEnabled = !isSpinning && spinsRemaining > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onSpinClick,
            enabled = isEnabled,
            shape = RoundedCornerShape(AppRadius.button),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Primary,
                disabledContainerColor = if (spinsRemaining <= 0) Color(0xFFE2E8F0) else AppColors.Primary.copy(alpha = 0.6f),
                contentColor = Color.White,
                disabledContentColor = if (spinsRemaining <= 0) AppColors.TextMuted else Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("spin_now_button")
        ) {
            if (isSpinning) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                Text(
                    text = "SPINNING...",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            } else if (spinsRemaining <= 0) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Text(
                    text = "NO SPINS LEFT",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Text(
                    text = "SPIN NOW",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        if (spinsRemaining <= 0) {
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = "Watch video below or come back tomorrow for more spins.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SpinHistoryHeader() {
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
            text = "Spin History",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = AppColors.TextNavy
        )
    }
}

@Composable
fun SpinHistoryEmptyState() {
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
                text = "No spins recorded yet",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = AppColors.TextSecondary
            )
            Text(
                text = "Tap SPIN NOW above to win your first rewards!",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextMuted
            )
        }
    }
}

@Composable
fun SpinHistoryItem(transaction: CoinTransaction) {
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
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                Column {
                    Text(
                        text = "Spin & Win",
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

@Composable
fun SpinWinDialog(
    result: SpinResult.Success,
    onContinue: () -> Unit
) {
    val rupeeEstimate = CoinConversionHelper.getCurrencyEstimate(result.newBalance)
    val formattedBalance = NumberFormat.getNumberInstance(Locale.US).format(result.newBalance)

    Dialog(onDismissRequest = onContinue) {
        Surface(
            shape = RoundedCornerShape(AppRadius.bottomSheet),
            color = AppColors.SurfaceLight,
            shadowElevation = AppElevation.modal,
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md)
                .testTag("spin_win_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))
                            )
                        )
                        .border(3.dp, AppColors.GoldCoin, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MonetizationOn,
                        contentDescription = "Reward",
                        tint = AppColors.GoldCoinDark,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))

                Text(
                    text = "🎉 YOU WON!",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.TextNavy
                )

                Spacer(modifier = Modifier.height(AppSpacing.xs))

                Text(
                    text = "+${result.coinsAwarded} NestCoins",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.Primary
                    )
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                Surface(
                    shape = RoundedCornerShape(AppRadius.card),
                    color = AppColors.BackgroundLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(AppSpacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "New Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary
                        )
                        Text(
                            text = "$formattedBalance NestCoins",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.TextNavy
                        )
                        Text(
                            text = "≈ $rupeeEstimate",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = AppColors.SuccessGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.xs))

                Text(
                    text = "${result.spinsRemainingToday} free spins remaining today",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(AppSpacing.lg))

                Button(
                    onClick = onContinue,
                    shape = RoundedCornerShape(AppRadius.button),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("spin_win_continue_button")
                ) {
                    Text(
                        text = "CONTINUE",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
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
