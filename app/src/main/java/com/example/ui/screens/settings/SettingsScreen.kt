package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.CoinConversionHelper
import com.example.ui.components.AppCard
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.AppSectionHeader
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing

import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tune

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLoggedOut: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val user by viewModel.currentUser.collectAsState()

    var notificationsEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }

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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BackgroundLight)
            .testTag("settings_screen_content"),
        contentPadding = PaddingValues(bottom = AppSpacing.xxxl)
    ) {
        // Top Back Action & Title
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
                    text = "Settings & Account",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.TextNavy
                )
            }
        }

        // 1. Profile Card Link
        item {
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.screenHorizontal)
                    .clickable { onNavigateToProfile() }
                    .testTag("settings_profile_card"),
                backgroundColor = AppColors.SurfaceLight
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(AppColors.PrimaryLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = avatarIcon,
                                contentDescription = null,
                                tint = AppColors.Primary,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(AppSpacing.md))

                        Column {
                            Text(
                                text = user?.displayName ?: "Player",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                                color = AppColors.TextNavy
                            )
                            Text(
                                text = user?.email ?: "player@playrewards.local",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                            Text(
                                text = "Tap to view full profile & stats →",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    color = AppColors.Primary
                                )
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = AppColors.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 2. Preferences (Notifications, Sound, Vibration)
        item {
            AppSectionHeader(title = "App Preferences")

            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenHorizontal),
                backgroundColor = AppColors.SurfaceLight
            ) {
                Column {
                    SettingsSwitchRow(
                        icon = Icons.Filled.Notifications,
                        title = "Push Notifications",
                        subtitle = "Daily streak reminders & coin drops",
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = AppSpacing.xs),
                        color = AppColors.SurfaceBorder.copy(alpha = 0.5f)
                    )

                    SettingsSwitchRow(
                        icon = Icons.Filled.VolumeUp,
                        title = "Sound Effects",
                        subtitle = "Audio feedback during mini-games",
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = AppSpacing.xs),
                        color = AppColors.SurfaceBorder.copy(alpha = 0.5f)
                    )

                    SettingsSwitchRow(
                        icon = Icons.Filled.Vibration,
                        title = "Haptic Vibration",
                        subtitle = "Tactile feedback on win & claims",
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = AppSpacing.xs),
                        color = AppColors.SurfaceBorder.copy(alpha = 0.5f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToNotifications() }
                            .padding(vertical = AppSpacing.sm)
                            .testTag("settings_notification_center_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.PrimaryLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = null,
                                    tint = AppColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(AppSpacing.md))
                            Column {
                                Text(
                                    text = "Notification Channels",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    ),
                                    color = AppColors.TextNavy
                                )
                                Text(
                                    text = "Manage granular alert categories",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = AppColors.TextSecondary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = AppColors.TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = AppSpacing.xs),
                        color = AppColors.SurfaceBorder.copy(alpha = 0.5f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToActivity() }
                            .padding(vertical = AppSpacing.sm)
                            .testTag("settings_activity_history_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.NavyCard.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = null,
                                    tint = AppColors.NavyCard,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(AppSpacing.md))
                            Column {
                                Text(
                                    text = "Activity Log & Timeline",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    ),
                                    color = AppColors.TextNavy
                                )
                                Text(
                                    text = "View full history of all actions & rewards",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = AppColors.TextSecondary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = AppColors.TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // 3. Economy & Ledger Security Information
        item {
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            AppSectionHeader(title = "Security & Economy")

            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenHorizontal),
                backgroundColor = AppColors.SurfaceLight
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Calculate, contentDescription = null, tint = AppColors.Primary)
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        Text(
                            text = "Authoritative Reward Ledger",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.TextNavy
                        )
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Text(
                        text = "Standard Economy Rate: ${CoinConversionHelper.getRateExplanation()} (700 Coins = ₹1.00). Ledger updates are strictly server-side verified.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }

        // 4. Privacy, Terms, Delete Account, Sign Out
        item {
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            AppSectionHeader(title = "Account & Legal")

            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenHorizontal),
                backgroundColor = AppColors.SurfaceLight
            ) {
                Column {
                    SettingsActionRow(
                        icon = Icons.Filled.Person,
                        title = "View Profile Details",
                        onClick = onNavigateToProfile
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = AppSpacing.xs),
                        color = AppColors.SurfaceBorder.copy(alpha = 0.5f)
                    )

                    SettingsActionRow(
                        icon = Icons.Filled.PrivacyTip,
                        title = "Privacy Policy",
                        onClick = { viewModel.openPrivacyPolicy() }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = AppSpacing.xs),
                        color = AppColors.SurfaceBorder.copy(alpha = 0.5f)
                    )

                    SettingsActionRow(
                        icon = Icons.Filled.Description,
                        title = "Terms of Service",
                        onClick = { viewModel.openTerms() }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = AppSpacing.xs),
                        color = AppColors.SurfaceBorder.copy(alpha = 0.5f)
                    )

                    SettingsActionRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Sign Out",
                        titleColor = AppColors.TextNavy,
                        onClick = { viewModel.openLogoutDialog() },
                        testTag = "settings_logout_row"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = AppSpacing.xs),
                        color = AppColors.SurfaceBorder.copy(alpha = 0.5f)
                    )

                    SettingsActionRow(
                        icon = Icons.Filled.DeleteForever,
                        title = "Delete Account",
                        titleColor = AppColors.ErrorRed,
                        onClick = { viewModel.openDeleteDialog() },
                        testTag = "settings_delete_account_row"
                    )
                }
            }
        }
    }

    // --- Logout Confirmation Dialog ---
    if (uiState.showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLogoutDialog() },
            shape = RoundedCornerShape(AppRadius.largeCard),
            containerColor = AppColors.SurfaceLight,
            title = {
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.TextNavy
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out of PlayRewards? You can sign back in anytime with your credentials.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
            },
            confirmButton = {
                AppPrimaryButton(
                    text = "Sign Out",
                    onClick = { viewModel.logout(onLoggedOut) },
                    isLoading = uiState.isLoggingOut,
                    modifier = Modifier.testTag("confirm_logout_button")
                )
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLogoutDialog() }) {
                    Text("Cancel", color = AppColors.TextSecondary)
                }
            }
        )
    }

    // --- Delete Account Destructive Dialog ---
    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            shape = RoundedCornerShape(AppRadius.largeCard),
            containerColor = AppColors.SurfaceLight,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = AppColors.ErrorRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Text(
                        text = "Delete Account?",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppColors.ErrorRed
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Warning: This action is permanent and cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.ErrorRed
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    Text(
                        text = "Deleting your account will permanently wipe:\n• Your profile & credentials\n• Entire wallet coin balance\n• Full transaction & reward ledger history\n• Mini-game high scores & streak progress\n• Active redemption requests\n• Referral records",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextNavy
                    )
                }
            },
            confirmButton = {
                AppPrimaryButton(
                    text = "Permanently Delete",
                    backgroundColor = AppColors.ErrorRed,
                    onClick = { viewModel.deleteAccount(onLoggedOut) },
                    isLoading = uiState.isDeletingAccount,
                    modifier = Modifier.testTag("confirm_delete_account_button")
                )
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text("Cancel", color = AppColors.TextSecondary)
                }
            }
        )
    }

    // --- Privacy Policy Dialog ---
    if (uiState.showPrivacyPolicyDialog) {
        LegalNoticeDialog(
            title = "Privacy Policy",
            content = "PlayRewards respects your privacy. We collect your display name, email, and on-device gameplay activity solely for providing rewards and game progress. Passwords are securely hashed with unique cryptographic salts and never stored in plaintext. We do not sell your personal information to third parties.",
            onDismiss = { viewModel.dismissPrivacyPolicy() }
        )
    }

    // --- Terms of Service Dialog ---
    if (uiState.showTermsDialog) {
        LegalNoticeDialog(
            title = "Terms of Service",
            content = "By using PlayRewards, you agree to fair gameplay rules. Any attempt to use bots, exploit coin ledger loopholes, automate clicks, or manipulate reward algorithms will result in immediate suspension. Coins have no cash surrender value until redeemed in accordance with redemption policies. 700 Coins = ₹1.00 base conversion rate.",
            onDismiss = { viewModel.dismissTerms() }
        )
    }
}

@Composable
private fun LegalNoticeDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(AppRadius.largeCard),
        containerColor = AppColors.SurfaceLight,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.TextNavy
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AppColors.Primary)
            }
        }
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.md))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = AppColors.TextNavy
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppColors.Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = AppColors.SurfaceBorder
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    titleColor: Color = AppColors.TextNavy,
    onClick: () -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable { onClick() }
            .padding(vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = titleColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.md))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = AppColors.TextMuted,
            modifier = Modifier.size(14.dp)
        )
    }
}
