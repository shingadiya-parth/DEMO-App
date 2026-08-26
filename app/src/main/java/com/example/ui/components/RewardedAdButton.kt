package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppRadius

/**
 * Reusable AdMob Rewarded Ad Button Component.
 * Supports loading states, cooldown badges, and custom rewards.
 */
@Composable
fun RewardedAdButton(
    title: String,
    rewardBadge: String = "+25 Coins",
    isLoading: Boolean = false,
    cooldownRemainingSeconds: Long = 0L,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCooldown = cooldownRemainingSeconds > 0L
    val isButtonEnabled = enabled && !isLoading && !isCooldown

    Button(
        onClick = onClick,
        enabled = isButtonEnabled,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("admob_rewarded_button"),
        shape = RoundedCornerShape(AppRadius.md),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.AccentPurple,
            contentColor = Color.White,
            disabledContainerColor = AppColors.SurfaceBorder,
            disabledContentColor = AppColors.TextSecondary
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Loading Ad...",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            } else if (isCooldown) {
                Text(
                    text = "Ad Cooldown (${cooldownRemainingSeconds}s)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.OndemandVideo,
                    contentDescription = "Watch Rewarded Ad",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "($rewardBadge)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.GoldCoin
                    )
                )
            }
        }
    }
}
