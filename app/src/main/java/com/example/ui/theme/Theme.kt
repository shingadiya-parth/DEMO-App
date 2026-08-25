package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = AppColors.Primary,
    onPrimary = AppColors.TextOnPrimary,
    primaryContainer = AppColors.PrimaryLight,
    onPrimaryContainer = AppColors.PrimaryDark,

    secondary = AppColors.AccentPurple,
    onSecondary = AppColors.TextOnPrimary,
    secondaryContainer = AppColors.AccentPurpleLight,
    onSecondaryContainer = AppColors.AccentPurpleDark,

    tertiary = AppColors.GoldCoin,
    onTertiary = AppColors.TextNavy,
    tertiaryContainer = AppColors.GoldCoinLight,
    onTertiaryContainer = AppColors.GoldCoinDark,

    background = AppColors.BackgroundLight,
    onBackground = AppColors.TextNavy,

    surface = AppColors.SurfaceLight,
    onSurface = AppColors.TextNavy,
    surfaceVariant = AppColors.SurfaceVariant,
    onSurfaceVariant = AppColors.TextSecondary,

    outline = AppColors.SurfaceBorder,
    outlineVariant = AppColors.TextMuted.copy(alpha = 0.2f),

    error = AppColors.ErrorRed,
    onError = AppColors.TextOnPrimary,
    errorContainer = AppColors.ErrorRedLight,
    onErrorContainer = AppColors.ErrorRed
)

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.Primary,
    onPrimary = AppColors.TextOnPrimary,
    primaryContainer = AppColors.DarkSurfaceVariant,
    onPrimaryContainer = AppColors.PrimaryLight,

    secondary = AppColors.AccentPurple,
    onSecondary = AppColors.TextOnPrimary,
    secondaryContainer = AppColors.DarkSurfaceVariant,
    onSecondaryContainer = AppColors.AccentPurpleLight,

    tertiary = AppColors.GoldCoin,
    onTertiary = AppColors.DarkBackground,
    tertiaryContainer = AppColors.DarkSurfaceVariant,
    onTertiaryContainer = AppColors.GoldCoinLight,

    background = AppColors.DarkBackground,
    onBackground = AppColors.DarkTextPrimary,

    surface = AppColors.DarkSurface,
    onSurface = AppColors.DarkTextPrimary,
    surfaceVariant = AppColors.DarkSurfaceVariant,
    onSurfaceVariant = AppColors.DarkTextSecondary,

    outline = AppColors.DarkSurfaceBorder,
    outlineVariant = AppColors.DarkSurfaceBorder,

    error = AppColors.ErrorRed,
    onError = AppColors.TextOnPrimary,
    errorContainer = AppColors.DarkSurfaceVariant,
    onErrorContainer = AppColors.ErrorRed
)

@Composable
fun PlayRewardsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

// Backwards compatibility aliases if needed
val BrandPrimary = AppColors.Primary
val BrandSecondary = AppColors.AccentPurple
val CoinGold = AppColors.GoldCoin
val CoinGoldDark = AppColors.GoldCoinDark
val SuccessGreen = AppColors.SuccessGreen
