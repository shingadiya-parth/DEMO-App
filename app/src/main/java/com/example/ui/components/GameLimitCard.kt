package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppSpacing

/**
 * Reusable Game Daily Limit & Quota Tracking Card.
 */
@Composable
fun GameLimitCard(
    usedCount: Int,
    limit: Int,
    gameLabel: String = "Games",
    modifier: Modifier = Modifier
) {
    val remaining = (limit - usedCount).coerceAtLeast(0)
    val isLimitReached = remaining == 0
    val progress = if (limit > 0) (usedCount.toFloat() / limit).coerceIn(0f, 1f) else 1f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("game_limit_card"),
        shape = RoundedCornerShape(16.dp),
        color = if (isLimitReached) AppColors.WarningAmberLight.copy(alpha = 0.4f) else AppColors.SurfaceLight,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isLimitReached) Icons.Filled.LockClock else Icons.Filled.HourglassEmpty,
                        contentDescription = null,
                        tint = if (isLimitReached) AppColors.WarningAmberDark else AppColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(
                        text = "Today's $gameLabel",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.TextNavy
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isLimitReached) AppColors.WarningAmberDark else AppColors.Primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$usedCount / $limit Plays",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isLimitReached) Color.White else AppColors.Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (isLimitReached) AppColors.WarningAmberDark else AppColors.Primary,
                trackColor = AppColors.SurfaceVariant
            )

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            if (isLimitReached) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = AppColors.WarningAmberDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Daily limit reached. Resets at midnight (00:00).",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.WarningAmberDark
                        )
                    )
                }
            } else {
                Text(
                    text = "$remaining $gameLabel remaining today",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}
