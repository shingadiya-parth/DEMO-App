package com.example.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.CoinConversionHelper
import com.example.ui.components.AppCard
import com.example.ui.components.AppOutlineButton
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.AppSectionHeader
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val balance by viewModel.currentBalance.collectAsState()
    val earned by viewModel.lifetimeEarned.collectAsState()
    val spent by viewModel.lifetimeSpent.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    val avatarIcon = when (user?.avatar) {
        "avatar_2" -> Icons.Filled.Face
        "avatar_3" -> Icons.Filled.SentimentVerySatisfied
        "avatar_4" -> Icons.Filled.EmojiEmotions
        "avatar_5" -> Icons.Filled.SportsEsports
        "avatar_6" -> Icons.Filled.Star
        else -> Icons.Filled.AccountCircle
    }

    val memberSinceFormatted = user?.accountCreationDate?.let {
        SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(it))
    } ?: "Recent"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BackgroundLight)
            .testTag("profile_screen_content"),
        contentPadding = PaddingValues(bottom = AppSpacing.xxxl)
    ) {
        // Back Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AppColors.TextNavy
                    )
                }
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Text(
                    text = "My Profile",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.TextNavy
                )
            }
        }

        // Hero Profile Card
        item {
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenHorizontal),
                backgroundColor = AppColors.SurfaceLight
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(AppColors.PrimaryLight, AppColors.AccentPurpleLight)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = avatarIcon,
                            contentDescription = "Avatar",
                            tint = AppColors.Primary,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    Text(
                        text = user?.displayName ?: "Player",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.TextNavy
                    )

                    Text(
                        text = user?.email ?: "player@playrewards.local",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                    // Status & Country Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(AppRadius.pill),
                            color = AppColors.SuccessGreenLight
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = AppColors.SuccessGreenDark,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = user?.accountStatus?.name ?: "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.SuccessGreenDark
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(AppSpacing.sm))

                        Surface(
                            shape = RoundedCornerShape(AppRadius.pill),
                            color = AppColors.BackgroundLight
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Flag,
                                    contentDescription = null,
                                    tint = AppColors.TextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = user?.country ?: "IN",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.TextNavy
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    AppOutlineButton(
                        text = "Edit Profile",
                        onClick = { viewModel.openEditProfile() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_edit_profile_button")
                    )
                }
            }
        }

        // Stats Grid: Current Balance, Lifetime Earned, Total Spent
        item {
            Spacer(modifier = Modifier.height(AppSpacing.md))
            AppSectionHeader(title = "Wallet & Activity Overview")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                // Balance
                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Balance",
                    value = CoinConversionHelper.formatCoins(balance),
                    subtitle = "≈ ${CoinConversionHelper.getCurrencyEstimate(balance)}",
                    icon = Icons.Filled.MonetizationOn,
                    iconTint = AppColors.GoldCoin,
                    backgroundColor = AppColors.GoldCoinLight
                )

                // Lifetime Earned
                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Earned",
                    value = CoinConversionHelper.formatCoins(earned),
                    subtitle = "Lifetime",
                    icon = Icons.Filled.TrendingUp,
                    iconTint = AppColors.SuccessGreenDark,
                    backgroundColor = AppColors.SuccessGreenLight
                )

                // Total Redeemed
                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Redeemed",
                    value = CoinConversionHelper.formatCoins(spent),
                    subtitle = "Spent",
                    icon = Icons.Filled.Redeem,
                    iconTint = AppColors.AccentPurple,
                    backgroundColor = AppColors.AccentPurpleLight
                )
            }
        }

        // Referral Code Card
        item {
            Spacer(modifier = Modifier.height(AppSpacing.md))
            AppSectionHeader(title = "Referral Code")

            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenHorizontal),
                backgroundColor = AppColors.SurfaceLight
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AppColors.GoldCoinLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CardGiftcard,
                                contentDescription = null,
                                tint = AppColors.GoldCoinDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(AppSpacing.md))

                        Column {
                            Text(
                                text = "Your Referral Code",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                            Text(
                                text = user?.referralCode ?: "PLAY1234",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = AppColors.TextNavy
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val code = user?.referralCode ?: ""
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Referral Code", code))
                        },
                        modifier = Modifier.testTag("copy_referral_code_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy code",
                            tint = AppColors.Primary
                        )
                    }
                }
            }
        }

        // Account Details & Security Note
        item {
            Spacer(modifier = Modifier.height(AppSpacing.md))
            AppSectionHeader(title = "Account Details")

            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenHorizontal),
                backgroundColor = AppColors.SurfaceLight
            ) {
                Column {
                    AccountDetailRow(
                        icon = Icons.Filled.CalendarMonth,
                        label = "Member Since",
                        value = memberSinceFormatted
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    AccountDetailRow(
                        icon = Icons.Filled.Lock,
                        label = "Account ID",
                        value = user?.userId ?: "N/A"
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = AppColors.SuccessGreenDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Text(
                            text = "Authoritative double-entry ledger security active.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }
        }
    }

    if (uiState.isEditing) {
        EditProfileDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.dismissEditProfile() }
        )
    }
}

@Composable
private fun ProfileStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier,
        backgroundColor = AppColors.SurfaceLight,
        contentPadding = AppSpacing.sm
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                color = AppColors.TextNavy
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = AppColors.TextSecondary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = AppColors.TextMuted
            )
        }
    }
}

@Composable
private fun AccountDetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.TextNavy
        )
    }
}
