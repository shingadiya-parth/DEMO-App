package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.CoinConversionHelper
import com.example.ui.theme.AppAnimations
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing

/**
 * Animated Modern Reward Progress Bar
 */
@Composable
fun AppProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    progressColor: Color = AppColors.Primary,
    trackColor: Color = AppColors.SurfaceVariant
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = AppAnimations.DURATION_PROGRESS,
            easing = FastOutSlowInEasing
        ),
        label = "ProgressBarAnimation"
    )

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(AppRadius.pill)),
        color = progressColor,
        trackColor = trackColor
    )
}

/**
 * Reusable Reward Goal Card displaying next target milestone.
 * e.g.:
 * ₹10 Reward
 * 1,250 / 7,000 coins
 * [████░░░░░░]
 * 5,750 coins remaining
 */
@Composable
fun RewardGoalCard(
    currentCoins: Long,
    targetCoins: Long = 7000L,
    rewardTitle: String = "₹10 Google Play / Amazon Voucher",
    onRedeemClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val progress = (currentCoins.toFloat() / targetCoins.toFloat()).coerceIn(0f, 1f)
    val coinsRemaining = (targetCoins - currentCoins).coerceAtLeast(0L)
    val percentage = (progress * 100).toInt()
    val isGoalAchieved = currentCoins >= targetCoins

    AppCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = AppColors.SurfaceLight
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                            imageVector = Icons.Filled.CardGiftcard,
                            contentDescription = null,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(AppSpacing.md))
                    Column {
                        Text(
                            text = "Next Reward Goal",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                        Text(
                            text = rewardTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.TextNavy
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(AppRadius.small),
                    color = if (isGoalAchieved) AppColors.SuccessGreenLight else AppColors.PrimaryLight
                ) {
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isGoalAchieved) AppColors.SuccessGreenDark else AppColors.PrimaryDark,
                            fontWeight = FontWeight.Black
                        ),
                        modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xxs)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${CoinConversionHelper.formatCoins(currentCoins)} / ${CoinConversionHelper.formatCoins(targetCoins)} coins",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.TextNavy
                )
                Text(
                    text = if (isGoalAchieved) "Goal Reached!" else "${CoinConversionHelper.formatCoins(coinsRemaining)} coins left",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (isGoalAchieved) AppColors.SuccessGreenDark else AppColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            AppProgressBar(
                progress = progress,
                progressColor = if (isGoalAchieved) AppColors.SuccessGreen else AppColors.Primary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
