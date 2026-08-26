package com.example.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.DailyBonusConfig
import com.example.data.model.GameDefinition
import com.example.domain.engine.CoinConversionHelper
import com.example.services.ads.AdPlacement
import com.example.ui.components.AdBannerContainer
import com.example.ui.components.AppCard
import com.example.ui.components.AppHeroCard
import com.example.ui.components.AppProgressBar
import com.example.ui.components.AppSectionHeader
import com.example.ui.components.AppSmallActionButton
import com.example.ui.components.IllustrationPlaceholder
import com.example.ui.components.RewardResultDialog
import com.example.ui.navigation.Screen
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppElevation
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing

/**
 * Step 5 — Production Home Screen with Authoritative Daily Bonus,
 * Dynamic Coin Balance & Rupee Value, Next Reward Goal, Quick Play, and In-App Earn.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateTo: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState()
    val balance by viewModel.currentBalance.collectAsState()
    val streak by viewModel.currentStreak.collectAsState()
    val dailyBonusState by viewModel.dailyBonusState.collectAsState()
    val rewardSuccessDialog by viewModel.rewardSuccessDialog.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val greeting = viewModel.getTimeGreeting()
    val displayName = user?.displayName ?: "Player"
    val featuredGames = viewModel.getFeaturedGames()

    // Refresh bonus status on launch
    LaunchedEffect(Unit) {
        viewModel.refreshDailyBonusStatus()
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BackgroundLight)
            .testTag("home_screen_content"),
        contentPadding = PaddingValues(bottom = AppSpacing.xxxl)
    ) {
        // 1. HOME HEADER (Time-based Greeting, Name, Profile avatar, Notification)
        item {
            HomeHeaderSection(
                greeting = greeting,
                userName = displayName,
                onProfileClick = { onNavigateTo(Screen.Profile.route) },
                onNotificationClick = { onNavigateTo(Screen.Wallet.route) }
            )
        }

        // 2. PROMINENT COIN BALANCE CARD (🪙 Current NestCoins, ≈ ₹ Rupee conversion, View Wallet action)
        item {
            HomeBalanceHeroCard(
                balance = balance,
                onViewWallet = { onNavigateTo(Screen.Wallet.route) },
                onPlayGames = { onNavigateTo(Screen.Play.route) },
                onRedeem = { onNavigateTo(Screen.Rewards.route) }
            )
        }

        // 3. REWARD PROGRESS CARD (Dynamic calculation toward next reward goal)
        item {
            HomeRewardProgressCard(
                currentCoins = balance,
                targetCoins = viewModel.nextRewardGoal.targetCoins,
                rewardName = viewModel.nextRewardGoal.rewardName,
                onRedeemClick = { onNavigateTo(Screen.Rewards.route) }
            )
        }

        // 4. DAILY BONUS SECTION (Claim 100 Coins, Real-time Authoritative State)
        item {
            HomeDailyBonusCard(
                state = dailyBonusState,
                streakDays = streak?.currentStreak ?: 0,
                onClaim = { viewModel.claimDailyBonus() }
            )
        }

        // 5. QUICK PLAY SECTION (Spin & Win, Scratch & Reveal, Puzzle, Coin Toss)
        item {
            Spacer(modifier = Modifier.height(AppSpacing.md))
            AppSectionHeader(
                title = "Quick Play",
                actionText = "View All Games",
                onActionClick = { onNavigateTo(Screen.Play.route) }
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                contentPadding = PaddingValues(horizontal = AppSpacing.screenHorizontal)
            ) {
                items(featuredGames, key = { it.gameId }) { game ->
                    HomeQuickGameCard(
                        game = game,
                        onPlayClick = {
                            when (game.gameId) {
                                "spin_win" -> onNavigateTo(Screen.Spin.route)
                                "scratch_card", "scratch_reveal" -> onNavigateTo(Screen.Scratch.route)
                                "puzzle", "puzzles" -> onNavigateTo(Screen.Puzzle.route)
                                "coin_toss" -> onNavigateTo(Screen.CoinToss.route)
                                "tictactoe" -> onNavigateTo(Screen.TicTacToe.route)
                                "bubble_pop" -> onNavigateTo(Screen.BubblePop.route)
                                else -> onNavigateTo(Screen.Play.route)
                            }
                        }
                    )
                }
            }
        }

        // Ad Banner Slot
        item {
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            AdBannerContainer(placement = AdPlacement.BANNER_HOME)
        }

        // 6. EARN MORE SECTION (Only In-App Activities: Daily Bonus, Refer & Earn, Giveaways)
        item {
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            AppSectionHeader(
                title = "Earn More",
                actionText = "View All",
                onActionClick = { onNavigateTo(Screen.Earn.route) }
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                EarnActivityBanner(
                    title = "Daily Bonus 🎁",
                    subtitle = "Claim your +100 free coins every calendar day",
                    rewardBadge = "+100 Coins",
                    icon = Icons.Filled.CardGiftcard,
                    color = AppColors.AccentPurpleDark,
                    bgColor = AppColors.AccentPurpleLight,
                    onClick = {
                        if (dailyBonusState is DailyBonusUiState.Available) {
                            viewModel.claimDailyBonus()
                        } else {
                            onNavigateTo(Screen.Earn.route)
                        }
                    }
                )

                EarnActivityBanner(
                    title = "Refer & Earn",
                    subtitle = "Share invite code to earn +500 Coins per friend",
                    rewardBadge = "+500 Coins",
                    icon = Icons.Filled.Group,
                    color = AppColors.Primary,
                    bgColor = AppColors.PrimaryLight,
                    onClick = { onNavigateTo(Screen.ReferEarn.route) }
                )

                EarnActivityBanner(
                    title = "Daily Community Giveaway",
                    subtitle = "Free 1,000 Coin pot dropped every 24 hours",
                    rewardBadge = "+1000 Coins",
                    icon = Icons.Filled.Whatshot,
                    color = AppColors.GoldCoinDark,
                    bgColor = AppColors.GoldCoinLight,
                    onClick = { onNavigateTo(Screen.Earn.route) }
                )
            }
        }

        // 7. CURRENT REWARD GOAL SECTION
        item {
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            AppSectionHeader(
                title = "Your Next Reward",
                actionText = "Catalog",
                onActionClick = { onNavigateTo(Screen.Rewards.route) }
            )

            Box(modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal)) {
                RewardGoalOverviewCard(
                    currentCoins = balance,
                    targetCoins = viewModel.nextRewardGoal.targetCoins,
                    rewardTitle = viewModel.nextRewardGoal.rewardName,
                    rewardValue = "₹${viewModel.nextRewardGoal.rewardValueInr}",
                    onRedeemClick = { onNavigateTo(Screen.Rewards.route) }
                )
            }
        }
    }

    // Success Reward Modal Dialog
    rewardSuccessDialog?.let { success ->
        RewardResultDialog(
            coinsEarned = success.coinsEarned,
            newBalance = success.newBalance,
            title = "Daily Bonus Claimed!",
            subtitle = "Your daily bonus of +${success.coinsEarned} NestCoins has been added to your ledger.",
            onDismiss = { viewModel.dismissSuccessDialog() }
        )
    }
}

/**
 * 1. Home Header Section with time-based greeting, user display name, profile avatar, and notification icon.
 */
@Composable
private fun HomeHeaderSection(
    greeting: String,
    userName: String,
    onProfileClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenHorizontal, vertical = AppSpacing.md)
            .testTag("home_header_section"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Greeting & User's Display Name
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onProfileClick() }
                .testTag("home_user_profile_header")
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
            Text(
                text = "$userName 👋",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.TextNavy
                ),
                maxLines = 1
            )
        }

        // Right: Profile Avatar & Notification / Ledger Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppColors.SurfaceLight)
                    .testTag("home_notification_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Notifications & Ledger",
                    tint = AppColors.TextNavy,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(AppColors.Primary, AppColors.AccentPurple)
                        )
                    )
                    .clickable { onProfileClick() }
                    .testTag("home_avatar_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * 2. Prominent Coin Balance Card (🪙 Current NestCoins, Approximate Rupee Value, View Wallet action).
 */
@Composable
private fun HomeBalanceHeroCard(
    balance: Long,
    onViewWallet: () -> Unit,
    onPlayGames: () -> Unit,
    onRedeem: () -> Unit
) {
    AppHeroCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenHorizontal)
            .testTag("home_balance_card")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current NestCoins",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextSecondary
                    )
                )

                // "View Wallet" small action link
                Surface(
                    onClick = onViewWallet,
                    shape = RoundedCornerShape(AppRadius.pill),
                    color = AppColors.PrimaryLight.copy(alpha = 0.7f),
                    modifier = Modifier.testTag("view_wallet_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View Wallet",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppColors.PrimaryDark
                            )
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = AppColors.PrimaryDark,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // 🪙 Current Coin Display
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(AppColors.GoldCoin, AppColors.GoldCoinDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MonetizationOn,
                        contentDescription = "NestCoins",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(AppSpacing.md))

                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = CoinConversionHelper.formatCoins(balance),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = AppColors.TextNavy
                            ),
                            modifier = Modifier.testTag("home_balance_text")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NestCoins",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppColors.GoldCoinDark
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Approximate Rupee Value (700 coins = ₹1)
                    Text(
                        text = "≈ ${CoinConversionHelper.getCurrencyEstimate(balance)} (Rate: 700 Coins = ₹1)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextSecondary
                        ),
                        modifier = Modifier.testTag("home_rupee_value_text")
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // Quick Actions: Play Games & Redeem
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                Button(
                    onClick = onPlayGames,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("home_play_btn"),
                    shape = RoundedCornerShape(AppRadius.button),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Gamepad,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Play Games",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = onRedeem,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("home_redeem_btn"),
                    shape = RoundedCornerShape(AppRadius.button),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.SurfaceLight,
                        contentColor = AppColors.Primary
                    ),
                    border = BorderStroke(1.5.dp, AppColors.Primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CardGiftcard,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Redeem",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

/**
 * 3. Reward Progress Card (Progress toward next available reward).
 */
@Composable
private fun HomeRewardProgressCard(
    currentCoins: Long,
    targetCoins: Long,
    rewardName: String,
    onRedeemClick: () -> Unit
) {
    val progress = (currentCoins.toFloat() / targetCoins.toFloat()).coerceIn(0f, 1f)
    val remainingCoins = (targetCoins - currentCoins).coerceAtLeast(0L)
    val isRewardAvailable = currentCoins >= targetCoins

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenHorizontal, vertical = AppSpacing.xs)
            .testTag("home_reward_progress_card"),
        backgroundColor = AppColors.SurfaceLight
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Next Reward",
                        style = MaterialTheme.typography.labelSmall.copy(color = AppColors.TextSecondary)
                    )
                    Text(
                        text = "₹10 Google Play Voucher",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.TextNavy
                    )
                }

                Surface(
                    shape = RoundedCornerShape(AppRadius.small),
                    color = if (isRewardAvailable) AppColors.SuccessGreenLight else AppColors.PrimaryLight
                ) {
                    Text(
                        text = if (isRewardAvailable) "Reward Available" else "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isRewardAvailable) AppColors.SuccessGreenDark else AppColors.PrimaryDark,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // Dynamic progress bar
            AppProgressBar(
                progress = progress,
                progressColor = if (isRewardAvailable) AppColors.SuccessGreen else AppColors.Primary,
                modifier = Modifier.fillMaxWidth().testTag("home_reward_progress_bar")
            )

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${CoinConversionHelper.formatCoins(currentCoins)} / ${CoinConversionHelper.formatCoins(targetCoins)} coins",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextSecondary
                    ),
                    modifier = Modifier.testTag("home_reward_coins_text")
                )

                if (isRewardAvailable) {
                    AppSmallActionButton(
                        text = "Redeem Now",
                        onClick = onRedeemClick,
                        backgroundColor = AppColors.SuccessGreen,
                        modifier = Modifier.testTag("reward_available_redeem_btn")
                    )
                } else {
                    Text(
                        text = "${CoinConversionHelper.formatCoins(remainingCoins)} coins remaining",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextMuted
                        ),
                        modifier = Modifier.testTag("home_reward_remaining_text")
                    )
                }
            }
        }
    }
}

/**
 * 4. Daily Bonus Card with Authoritative States (AVAILABLE, LOADING, SUCCESS, ALREADY CLAIMED, ERROR).
 */
@Composable
private fun HomeDailyBonusCard(
    state: DailyBonusUiState,
    streakDays: Int,
    onClaim: () -> Unit
) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.screenHorizontal, vertical = AppSpacing.xs)
            .testTag("home_daily_bonus_card"),
        backgroundColor = AppColors.AccentPurpleLight.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, AppColors.AccentPurple.copy(alpha = 0.2f))
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
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(AppColors.AccentPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎁",
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(AppSpacing.md))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = DailyBonusConfig.BONUS_TITLE,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppColors.AccentPurpleDark
                            )
                        )
                        if (streakDays > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(AppRadius.small),
                                color = AppColors.ActionOrangeLight
                            ) {
                                Text(
                                    text = "🔥 ${streakDays}d Streak",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.ActionOrangeDark
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = when (state) {
                            is DailyBonusUiState.AlreadyClaimed -> "Today's bonus already claimed. Come back tomorrow."
                            is DailyBonusUiState.Error -> state.message
                            else -> DailyBonusConfig.BONUS_SUBTITLE
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.width(AppSpacing.sm))

            // Daily Bonus Action Button States
            when (state) {
                is DailyBonusUiState.Available -> {
                    Button(
                        onClick = onClaim,
                        shape = RoundedCornerShape(AppRadius.button),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.AccentPurple
                        ),
                        contentPadding = PaddingValues(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                        modifier = Modifier.testTag("claim_daily_bonus_btn")
                    ) {
                        Text(
                            text = "CLAIM ${DailyBonusConfig.BONUS_AMOUNT_COINS} COINS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                    }
                }
                is DailyBonusUiState.Loading -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(AppRadius.button),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.AccentPurple.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.testTag("claim_daily_bonus_loading")
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Claiming...",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
                is DailyBonusUiState.AlreadyClaimed, is DailyBonusUiState.Success -> {
                    Surface(
                        shape = RoundedCornerShape(AppRadius.pill),
                        color = AppColors.SuccessGreenLight,
                        modifier = Modifier.testTag("daily_bonus_claimed_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = AppColors.SuccessGreenDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Claimed Today",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.SuccessGreenDark
                                )
                            )
                        }
                    }
                }
                is DailyBonusUiState.Error -> {
                    Button(
                        onClick = onClaim,
                        shape = RoundedCornerShape(AppRadius.button),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.CoralRed),
                        modifier = Modifier.testTag("claim_daily_bonus_retry")
                    ) {
                        Text("Retry", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

/**
 * 5. Quick Play Game Card.
 */
@Composable
private fun HomeQuickGameCard(
    game: GameDefinition,
    onPlayClick: () -> Unit
) {
    AppCard(
        modifier = Modifier
            .width(175.dp)
            .clickable { onPlayClick() }
            .testTag("home_quick_game_${game.gameId}"),
        shape = RoundedCornerShape(AppRadius.card),
        contentPadding = AppSpacing.compactCardPadding
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IllustrationPlaceholder(gameId = game.gameId, size = 40.dp)

                Surface(
                    shape = RoundedCornerShape(AppRadius.small),
                    color = AppColors.GoldCoinLight
                ) {
                    Text(
                        text = "+${game.baseRewardCoins} Coins",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppColors.GoldCoinDark
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            Text(
                text = game.gameName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )

            Text(
                text = game.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = AppColors.TextSecondary,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            AppSmallActionButton(
                text = "Play",
                onClick = onPlayClick,
                icon = Icons.Filled.PlayArrow,
                modifier = Modifier.fillMaxWidth().testTag("quick_play_btn_${game.gameId}")
            )
        }
    }
}

/**
 * 6. Earn Activity Banner (In-App Activities Only).
 */
@Composable
private fun EarnActivityBanner(
    title: String,
    subtitle: String,
    rewardBadge: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("earn_banner_${title.replace(" ", "_").lowercase()}"),
        backgroundColor = AppColors.SurfaceLight,
        contentPadding = AppSpacing.compactCardPadding
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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(AppSpacing.md))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.TextNavy
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(AppRadius.small),
                color = bgColor
            ) {
                Text(
                    text = rewardBadge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = color,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * 7. Reward Goal Overview Card.
 */
@Composable
private fun RewardGoalOverviewCard(
    currentCoins: Long,
    targetCoins: Long,
    rewardTitle: String,
    rewardValue: String,
    onRedeemClick: () -> Unit
) {
    val progress = (currentCoins.toFloat() / targetCoins.toFloat()).coerceIn(0f, 1f)
    val isAchieved = currentCoins >= targetCoins

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_reward_goal_card"),
        backgroundColor = AppColors.SurfaceLight
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AppColors.PrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Redeem,
                            contentDescription = null,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(AppSpacing.md))
                    Column {
                        Text(
                            text = rewardTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppColors.TextNavy
                        )
                        Text(
                            text = "$rewardValue Voucher",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(AppRadius.small),
                    color = AppColors.PrimaryLight
                ) {
                    Text(
                        text = rewardValue,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = AppColors.PrimaryDark
                        ),
                        modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            AppProgressBar(
                progress = progress,
                progressColor = if (isAchieved) AppColors.SuccessGreen else AppColors.Primary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${CoinConversionHelper.formatCoins(currentCoins)} / ${CoinConversionHelper.formatCoins(targetCoins)} coins",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.TextNavy
                )

                if (isAchieved) {
                    AppSmallActionButton(
                        text = "Redeem",
                        onClick = onRedeemClick,
                        backgroundColor = AppColors.SuccessGreen,
                        modifier = Modifier.testTag("home_goal_redeem_btn")
                    )
                } else {
                    Text(
                        text = "${CoinConversionHelper.formatCoins((targetCoins - currentCoins).coerceAtLeast(0L))} coins left",
                        style = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextSecondary)
                    )
                }
            }
        }
    }
}
