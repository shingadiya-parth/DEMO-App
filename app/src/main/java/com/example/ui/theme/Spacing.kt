package com.example.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized Spacing, Radius, and Elevation Tokens.
 * Consistent 8dp base grid system for padding, margins, corners, and elevations.
 */
object AppSpacing {
    val none: Dp = 0.dp
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
    val screenHorizontal: Dp = 16.dp
    val cardPadding: Dp = 16.dp
    val compactCardPadding: Dp = 12.dp
    val sectionSpacing: Dp = 20.dp
}

object AppRadius {
    val small: Dp = 8.dp       // Small tags, chips, sub-elements
    val button: Dp = 14.dp      // Standard buttons
    val pill: Dp = 999.dp       // Capsule/Pill buttons & chips
    val card: Dp = 20.dp        // Standard card corner radius
    val largeCard: Dp = 24.dp   // Hero cards & prominent feature cards
    val bottomSheet: Dp = 28.dp // Bottom sheets & dialogs
}

object AppElevation {
    val none: Dp = 0.dp
    val subtle: Dp = 1.dp
    val card: Dp = 2.dp
    val raised: Dp = 4.dp
    val modal: Dp = 8.dp
}
