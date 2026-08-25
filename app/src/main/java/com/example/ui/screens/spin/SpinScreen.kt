package com.example.ui.screens.spin

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.RewardGrantResult
import com.example.services.ads.AdPlacement
import com.example.ui.components.AdBannerContainer
import com.example.ui.components.AppCard
import com.example.ui.components.AppHeroCard
import com.example.ui.components.AppOutlineButton
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.HeroCoinBalance
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppElevation
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing

@Composable
fun SpinScreen(
    viewModel: SpinViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
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
        // 1. Header Information Card
        item {
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.screenHorizontal),
                backgroundColor = AppColors.SurfaceLight
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = AppColors.Primary
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.sm))
                            Text(
                                text = "Lucky Fortune Wheel",
                                style = MaterialTheme.typography.titleLarge,
                                color = AppColors.TextNavy
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(AppRadius.small),
                            color = AppColors.GoldCoinLight
                        ) {
                            Text(
                                text = "Win up to 700 Coins",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AppColors.GoldCoinDark,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xxs)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                    Text(
                        text = "Spin daily for guaranteed wallet coins. Free daily spins with optional 2x rewarded ad multipliers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }

        // 2. Wheel UI Placeholder & Wheel Canvas
        item {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .padding(AppSpacing.lg),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val diameter = size.minDimension
                    val radius = diameter / 2f
                    val center = Offset(radius, radius)
                    val sectorAngle = 360f / viewModel.wheelSectors.size

                    val sectorColors = listOf(
                        Color(0xFF3B82F6), Color(0xFF10B981),
                        Color(0xFFF59E0B), Color(0xFF8B5CF6),
                        Color(0xFFEC4899), Color(0xFF06B6D4),
                        Color(0xFF6366F1), Color(0xFF14B8A6)
                    )

                    viewModel.wheelSectors.forEachIndexed { index, _ ->
                        val startAngle = index * sectorAngle
                        drawArc(
                            color = sectorColors[index % sectorColors.size],
                            startAngle = startAngle,
                            sweepAngle = sectorAngle,
                            useCenter = true,
                            size = Size(diameter, diameter)
                        )
                    }

                    drawCircle(
                        color = Color.White,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 6.dp.toPx())
                    )
                }

                // Center Gold Star Hub
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = AppColors.SurfaceLight,
                    shadowElevation = AppElevation.raised
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Hub",
                            tint = AppColors.GoldCoin,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // 3. Daily Remaining Spins Indicator
        item {
            Surface(
                shape = RoundedCornerShape(AppRadius.pill),
                color = AppColors.PrimaryLight,
                modifier = Modifier.padding(bottom = AppSpacing.lg)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AvTimer,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(
                        text = "10 Daily Free Spins Available",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = AppColors.PrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // 4. Main Spin Action Buttons
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                AppPrimaryButton(
                    text = "Spin Wheel (Free)",
                    icon = Icons.Filled.Refresh,
                    onClick = { viewModel.performSpin() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spin_wheel_button")
                )

                AppOutlineButton(
                    text = "Watch Ad for 2x Multiplier Spin",
                    icon = Icons.Filled.OndemandVideo,
                    onClick = { viewModel.watchRewardedAdForExtraSpin() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("watch_ad_spin_button")
                )
            }
        }

        // 5. Ad Banner Container
        item {
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            AdBannerContainer(placement = AdPlacement.BANNER_GAMES)
        }
    }
}
