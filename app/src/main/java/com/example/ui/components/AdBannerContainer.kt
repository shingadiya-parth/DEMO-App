package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.services.ads.AdEnvironment
import com.example.services.ads.AdMobConfig
import com.example.services.ads.AdPlacement
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppRadius

/**
 * Standard AdMob Banner View Container.
 * Formatted for standard 320x50 / adaptive banner display.
 * Includes graceful fallback and test mode indicator.
 */
@Composable
fun AdBannerContainer(
    placement: AdPlacement,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(AppRadius.sm))
            .border(1.dp, AppColors.SurfaceBorder, RoundedCornerShape(AppRadius.sm))
            .testTag("admob_banner_container"),
        color = AppColors.SurfaceLight
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(AppColors.Primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (AdMobConfig.environment == AdEnvironment.DEVELOPMENT_TEST) "TEST AD" else "AD",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Primary,
                            fontSize = 10.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Google AdMob Banner Slot",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.TextNavy
                    )
                    Text(
                        text = "Unit: ${placement.slotId.take(22)}...",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = AppColors.TextSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "AdMob Slot Info",
                tint = AppColors.TextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
