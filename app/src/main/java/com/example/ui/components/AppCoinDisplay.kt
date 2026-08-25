package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.CoinConversionHelper
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.CoinTypography

/**
 * Reusable Coin Icon with warm radial gold gradient.
 */
@Composable
fun AppCoinIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(AppColors.GoldCoin, AppColors.GoldCoinDark)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MonetizationOn,
            contentDescription = "Coin",
            tint = Color.White,
            modifier = Modifier.size(size * 0.75f)
        )
    }
}

/**
 * Large/Hero Coin Balance Display for Hero Cards.
 * e.g. 🪙 1,250 Coins ≈ ₹1.78
 */
@Composable
fun HeroCoinBalance(
    balance: Long,
    modifier: Modifier = Modifier,
    showRupeeEquivalent: Boolean = true
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppCoinIcon(size = 38.dp)
            Spacer(modifier = Modifier.width(AppSpacing.md))
            Text(
                text = CoinConversionHelper.formatCoins(balance),
                style = CoinTypography.heroCoinValue
            )
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            Text(
                text = "Coins",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextSecondary
                ),
                modifier = Modifier.align(Alignment.Bottom).padding(bottom = 4.dp)
            )
        }

        if (showRupeeEquivalent) {
            Text(
                text = "≈ ${CoinConversionHelper.getCurrencyEstimate(balance)} (Rate: 700 Coins = ₹1)",
                style = CoinTypography.rupeeSubtitle,
                modifier = Modifier.padding(start = 48.dp, top = 2.dp)
            )
        }
    }
}

/**
 * Compact Coin Chip for TopBar and sub-screens.
 */
@Composable
fun CompactCoinChip(
    balance: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(AppRadius.pill),
        color = AppColors.GoldCoinLight,
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppCoinIcon(size = 20.dp)
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            Text(
                text = CoinConversionHelper.formatCoins(balance),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = AppColors.GoldCoinDark
                )
            )
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            Text(
                text = "(${CoinConversionHelper.getCurrencyEstimate(balance)})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextSecondary
                )
            )
        }
    }
}
