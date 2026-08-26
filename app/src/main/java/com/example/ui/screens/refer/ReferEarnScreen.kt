package com.example.ui.screens.refer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.ReferralConfig
import com.example.data.model.ReferralStatus
import com.example.data.model.SafeReferralDisplayItem
import com.example.ui.components.AppCard
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.AppSectionHeader
import com.example.ui.components.AppSmallActionButton
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferEarnScreen(
    viewModel: ReferEarnViewModel,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val referralSummary by viewModel.referralSummary.collectAsState()
    val user by viewModel.currentUser.collectAsState()

    val codeToShare = referralSummary?.referralCode ?: user?.referralCode ?: "PLAY8921"

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Refer & Earn",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.TextNavy
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("refer_earn_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColors.TextNavy
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.BackgroundLight)
            )
        },
        containerColor = AppColors.BackgroundLight,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("refer_earn_scroll_view"),
            contentPadding = PaddingValues(bottom = AppSpacing.xxxl)
        ) {
            // 1. Hero Promo Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.screenHorizontal, vertical = AppSpacing.sm)
                        .clip(RoundedCornerShape(AppRadius.card))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AppColors.Primary, AppColors.AccentPurple)
                            )
                        )
                        .padding(AppSpacing.lg)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Invite Friends & Win",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(AppSpacing.xs))
                                Text(
                                    text = "Give friends +${ReferralConfig.referredUserReward} bonus coins, get +${ReferralConfig.referrerReward} NestCoins when they complete ${ReferralConfig.requiredGameSessionsCount} game rounds!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }

                            Spacer(modifier = Modifier.width(AppSpacing.md))

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.GoldCoin.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Stars,
                                    contentDescription = null,
                                    tint = AppColors.GoldCoin,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Share Code Card
            item {
                AppSectionHeader(title = "Your Unique Referral Code")

                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.screenHorizontal),
                    backgroundColor = AppColors.SurfaceLight
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Share this code with your friends",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )

                        Spacer(modifier = Modifier.height(AppSpacing.sm))

                        // Large Code Display Box
                        Surface(
                            shape = RoundedCornerShape(AppRadius.card),
                            color = AppColors.PrimaryLight.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, AppColors.Primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("referral_code_display_box")
                        ) {
                            Text(
                                text = codeToShare,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 4.sp,
                                    color = AppColors.Primary
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = AppSpacing.md)
                            )
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.md))

                        // Action Buttons: Copy Code & Share Invite
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            AppPrimaryButton(
                                text = "Copy Code",
                                onClick = {
                                    copyToClipboard(context, codeToShare)
                                    Toast.makeText(context, "Referral code copied!", Toast.LENGTH_SHORT).show()
                                },
                                icon = Icons.Filled.ContentCopy,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copy_referral_code_btn")
                            )

                            AppPrimaryButton(
                                text = "Share Invite",
                                onClick = {
                                    shareInvite(context, codeToShare)
                                },
                                icon = Icons.Filled.Share,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_invite_btn")
                            )
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.xs))

                        Text(
                            text = ReferralConfig.generateInviteLink(codeToShare),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = AppSpacing.xs)
                        )
                    }
                }
            }

            // 3. Referral Statistics Grid
            item {
                Spacer(modifier = Modifier.height(AppSpacing.lg))
                AppSectionHeader(title = "Referral Overview")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    ReferralStatBox(
                        title = "Invited",
                        value = "${referralSummary?.totalFriendsReferred ?: 0}",
                        icon = Icons.Filled.People,
                        iconTint = AppColors.Primary,
                        modifier = Modifier.weight(1f).testTag("stat_invited_friends")
                    )

                    ReferralStatBox(
                        title = "Qualified",
                        value = "${referralSummary?.qualifiedReferrals ?: 0}",
                        icon = Icons.Filled.CheckCircle,
                        iconTint = AppColors.SuccessGreen,
                        modifier = Modifier.weight(1f).testTag("stat_qualified_friends")
                    )

                    ReferralStatBox(
                        title = "Coins Earned",
                        value = "+${referralSummary?.totalCoinsEarned ?: 0}",
                        icon = Icons.Filled.MonetizationOn,
                        iconTint = AppColors.GoldCoinDark,
                        modifier = Modifier.weight(1f).testTag("stat_referral_coins_earned")
                    )
                }
            }

            // 4. "Have a Referral Code?" Section (Shown if user has not yet entered one)
            if (referralSummary?.hasAppliedReferralCode == false) {
                item {
                    Spacer(modifier = Modifier.height(AppSpacing.lg))
                    AppSectionHeader(title = "Have a Friend's Invite Code?")

                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.screenHorizontal),
                        backgroundColor = AppColors.SurfaceLight
                    ) {
                        Column {
                            Text(
                                text = "Enter an invite code to earn +${ReferralConfig.referredUserReward} NestCoins on your initial gameplay qualification.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )

                            Spacer(modifier = Modifier.height(AppSpacing.sm))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                            ) {
                                OutlinedTextField(
                                    value = uiState.inputCode,
                                    onValueChange = { viewModel.onCodeInputChanged(it) },
                                    placeholder = { Text("e.g. PARTH123", fontSize = 13.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(AppRadius.small),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppColors.Primary,
                                        unfocusedBorderColor = AppColors.SurfaceBorder
                                    ),
                                    enabled = !uiState.isApplying,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_referral_code_field")
                                )

                                AppSmallActionButton(
                                    text = if (uiState.isApplying) "Applying..." else "Apply",
                                    onClick = { viewModel.applyReferralCode() },
                                    enabled = !uiState.isApplying && uiState.inputCode.isNotBlank(),
                                    modifier = Modifier.testTag("submit_referral_code_btn")
                                )
                            }
                        }
                    }
                }
            }

            // 5. How Referral Works (Step-by-Step Guide)
            item {
                Spacer(modifier = Modifier.height(AppSpacing.lg))
                AppSectionHeader(title = "How Referrals Work")

                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.screenHorizontal),
                    backgroundColor = AppColors.SurfaceLight
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                        ReferralStepRow(
                            stepNumber = 1,
                            title = "Share Your Link",
                            description = "Send your referral code or invite link to friends via WhatsApp, Telegram, SMS, or Social.",
                            icon = Icons.Filled.PersonAdd
                        )

                        ReferralStepRow(
                            stepNumber = 2,
                            title = "Friend Plays ${ReferralConfig.requiredGameSessionsCount} Games",
                            description = "Your friend signs up and plays any ${ReferralConfig.requiredGameSessionsCount} game rounds in the app.",
                            icon = Icons.Filled.Gamepad
                        )

                        ReferralStepRow(
                            stepNumber = 3,
                            title = "Both Get Rewarded",
                            description = "You receive +${ReferralConfig.referrerReward} NestCoins and your friend receives +${ReferralConfig.referredUserReward} NestCoins immediately.",
                            icon = Icons.Filled.Star
                        )
                    }
                }
            }

            // 6. Referral Activity & History List
            item {
                Spacer(modifier = Modifier.height(AppSpacing.lg))
                AppSectionHeader(title = "Your Referrals History")
            }

            val referralsList = referralSummary?.recentReferrals ?: emptyList()
            if (referralsList.isEmpty()) {
                item {
                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.screenHorizontal),
                        backgroundColor = AppColors.SurfaceLight
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppSpacing.lg)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.People,
                                contentDescription = null,
                                tint = AppColors.TextMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.sm))
                            Text(
                                text = "No friends invited yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AppColors.TextNavy
                            )
                            Text(
                                text = "Share your invite code with friends to start earning NestCoins together!",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = AppSpacing.md)
                            )
                        }
                    }
                }
            } else {
                items(referralsList) { item ->
                    ReferralHistoryItemCard(item = item)
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                }
            }
        }
    }
}

@Composable
fun ReferralStatBox(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier,
        backgroundColor = AppColors.SurfaceLight
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = AppColors.TextNavy
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
fun ReferralStepRow(
    stepNumber: Int,
    title: String,
    description: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AppColors.PrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$stepNumber",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )
            )
        }

        Spacer(modifier = Modifier.width(AppSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = AppColors.TextNavy
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
fun ReferralHistoryItemCard(item: SafeReferralDisplayItem) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenHorizontal),
        backgroundColor = AppColors.SurfaceLight
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when (item.status) {
                                    ReferralStatus.QUALIFIED, ReferralStatus.REWARDED -> AppColors.SuccessGreenLight
                                    ReferralStatus.QUALIFYING -> AppColors.PrimaryLight
                                    ReferralStatus.PENDING -> AppColors.GoldCoinLight
                                    else -> AppColors.SurfaceBorder
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (item.status) {
                                ReferralStatus.QUALIFIED, ReferralStatus.REWARDED -> Icons.Filled.CheckCircle
                                ReferralStatus.QUALIFYING -> Icons.Filled.Gamepad
                                ReferralStatus.PENDING -> Icons.Filled.HourglassEmpty
                                else -> Icons.Filled.Info
                            },
                            contentDescription = null,
                            tint = when (item.status) {
                                ReferralStatus.QUALIFIED, ReferralStatus.REWARDED -> AppColors.SuccessGreen
                                ReferralStatus.QUALIFYING -> AppColors.Primary
                                ReferralStatus.PENDING -> AppColors.GoldCoinDark
                                else -> AppColors.TextMuted
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(AppSpacing.md))

                    Column {
                        Text(
                            text = item.friendLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.TextNavy
                        )
                        Text(
                            text = "Status: ${item.status.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(AppRadius.small),
                    color = when (item.status) {
                        ReferralStatus.REWARDED -> AppColors.SuccessGreenLight
                        ReferralStatus.QUALIFIED -> AppColors.GoldCoinLight
                        else -> AppColors.BackgroundLight
                    }
                ) {
                    Text(
                        text = if (item.status == ReferralStatus.REWARDED) "+${item.rewardCoins} Coins" else "${item.progress}/${item.target} Games",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (item.status == ReferralStatus.REWARDED) AppColors.SuccessGreen else AppColors.Primary
                        ),
                        modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xxs)
                    )
                }
            }

            // Progress Bar if still qualifying
            if (item.status == ReferralStatus.QUALIFYING || item.status == ReferralStatus.PENDING) {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                LinearProgressIndicator(
                    progress = { (item.progress.toFloat() / item.target.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AppColors.Primary,
                    trackColor = AppColors.SurfaceBorder
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("PlayRewards Referral Code", code)
    clipboard.setPrimaryClip(clip)
}

private fun shareInvite(context: Context, code: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Join PlayRewards and Earn Coins!")
        putExtra(Intent.EXTRA_TEXT, ReferralConfig.generateShareMessage(code))
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Referral Code"))
}
