package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing

/**
 * Reusable Section Header with title and optional action/subtitle.
 */
@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenHorizontal, vertical = AppSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = AppColors.TextNavy
        )

        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = AppColors.Primary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

/**
 * Original colorful placeholder illustration badges for games and rewards.
 */
@Composable
fun IllustrationPlaceholder(
    gameId: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val (icon, gradientColors) = when (gameId) {
        "spin_win" -> Icons.Filled.Refresh to listOf(Color(0xFF818CF8), Color(0xFF4F46E5))
        "scratch_reveal" -> Icons.Filled.Star to listOf(Color(0xFFFBBF24), Color(0xFFD97706))
        "puzzles" -> Icons.Filled.Extension to listOf(Color(0xFF34D399), Color(0xFF059669))
        "coin_toss" -> Icons.Filled.Casino to listOf(Color(0xFFF472B6), Color(0xFFDB2777))
        "tictactoe" -> Icons.Filled.VideogameAsset to listOf(Color(0xFF60A5FA), Color(0xFF2563EB))
        "word_puzzle" -> Icons.Filled.GridView to listOf(Color(0xFFA78BFA), Color(0xFF7C3AED))
        "bubble_pop" -> Icons.Filled.SportsEsports to listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
        "daily_challenge" -> Icons.Filled.Star to listOf(Color(0xFFFB923C), Color(0xFFEA580C))
        else -> Icons.Filled.Gamepad to listOf(Color(0xFF94A3B8), Color(0xFF475569))
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(AppRadius.card * 0.7f))
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

/**
 * Generic Loading State Container
 */
@Composable
fun AppLoadingState(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.xxxl),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = AppColors.Primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(AppSpacing.md))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
        }
    }
}

/**
 * Generic Empty State Container
 */
@Composable
fun AppEmptyState(
    title: String,
    description: String,
    icon: ImageVector = Icons.Filled.HelpOutline,
    actionButton: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.xxl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AppColors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.TextMuted,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(AppSpacing.md))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = AppColors.TextNavy
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(horizontal = AppSpacing.md)
            )
            if (actionButton != null) {
                Spacer(modifier = Modifier.height(AppSpacing.lg))
                actionButton()
            }
        }
    }
}
