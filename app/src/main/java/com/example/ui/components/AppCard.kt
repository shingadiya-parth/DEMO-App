package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppElevation
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing

/**
 * Reusable Card component adhering to the design system tokens:
 * - Large rounded corners
 * - Soft subtle shadow
 * - Comfortable internal padding
 * - Clean borders where appropriate
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppRadius.card),
    backgroundColor: Color = AppColors.SurfaceLight,
    border: BorderStroke? = BorderStroke(1.dp, AppColors.SurfaceBorder.copy(alpha = 0.6f)),
    elevation: Dp = AppElevation.card,
    contentPadding: Dp = AppSpacing.cardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Card(
        modifier = modifier.then(clickableModifier),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(
            modifier = Modifier.padding(contentPadding)
        ) {
            content()
        }
    }
}

/**
 * Reusable Hero Gradient Card for prominent top summaries and feature highlights.
 */
@Composable
fun AppHeroCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(
        AppColors.PrimaryLight.copy(alpha = 0.7f),
        AppColors.SurfaceLight
    ),
    shape: Shape = RoundedCornerShape(AppRadius.largeCard),
    border: BorderStroke? = BorderStroke(1.dp, AppColors.Primary.copy(alpha = 0.15f)),
    contentPadding: Dp = AppSpacing.xl,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.card)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.verticalGradient(gradientColors))
                .padding(contentPadding)
        ) {
            content()
        }
    }
}
