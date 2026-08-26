package com.example.ui.screens.rewards

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RedemptionRequest
import com.example.data.model.RedemptionReward
import com.example.data.model.RedemptionStatus
import com.example.data.model.RewardCategory
import com.example.domain.engine.CoinConversionHelper
import com.example.services.ads.AdPlacement
import com.example.ui.components.AdBannerContainer
import com.example.ui.components.AppCard
import com.example.ui.components.AppEmptyState
import com.example.ui.components.AppHeroCard
import com.example.ui.components.AppOutlineButton
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.AppProgressBar
import com.example.ui.components.AppRewardCard
import com.example.ui.components.AppSectionHeader
import com.example.ui.components.AppSmallActionButton
import com.example.ui.components.HeroCoinBalance
import com.example.ui.components.IllustrationPlaceholder
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppElevation
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RewardsScreen(
    viewModel: RewardsViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val balance by viewModel.currentBalance.collectAsState()
    val history by viewModel.redemptionHistory.collectAsState()

    val catalog = viewModel.getCatalog()
    val categories = listOf(
        null to "All",
        RewardCategory.GIFT_CARDS to "Gift Cards",
        RewardCategory.UPI_REWARDS to "UPI Rewards",
        RewardCategory.GAMING_VOUCHERS to "Gaming Vouchers",
        RewardCategory.DIGITAL_REWARDS to "Digital Passes"
    )

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BackgroundLight)
            .testTag("rewards_screen_content"),
        contentPadding = PaddingValues(bottom = AppSpacing.xxxl)
    ) {
        // 1. Hero Balance Card
        item {
            AppHeroCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.screenHorizontal)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Authoritative Redeemable Balance",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                        Surface(
                            shape = RoundedCornerShape(AppRadius.small),
                            color = AppColors.PrimaryLight
                        ) {
                            Text(
                                text = "700 Coins = ₹1.00",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AppColors.PrimaryDark,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xxs)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    HeroCoinBalance(balance = balance)
                }
            }
        }

        // 2. Navigation Tab Row: Catalog vs My Redemptions
        item {
            TabRow(
                selectedTabIndex = if (uiState.activeTab == RewardsTab.CATALOG) 0 else 1,
                containerColor = AppColors.BackgroundLight,
                contentColor = AppColors.Primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(
                            tabPositions[if (uiState.activeTab == RewardsTab.CATALOG) 0 else 1]
                        ),
                        color = AppColors.Primary
                    )
                },
                modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal)
            ) {
                Tab(
                    selected = uiState.activeTab == RewardsTab.CATALOG,
                    onClick = { viewModel.selectTab(RewardsTab.CATALOG) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Redeem, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Available Rewards", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_available_rewards")
                )
                Tab(
                    selected = uiState.activeTab == RewardsTab.MY_REDEMPTIONS,
                    onClick = { viewModel.selectTab(RewardsTab.MY_REDEMPTIONS) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("My Redemptions (${history.size})", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_my_redemptions")
                )
            }
        }

        // CONTENT BASED ON ACTIVE TAB
        if (uiState.activeTab == RewardsTab.CATALOG) {
            // Category Filter Chips
            item {
                Spacer(modifier = Modifier.height(AppSpacing.md))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = AppSpacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    items(categories) { (category, label) ->
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.Primary,
                                selectedLabelColor = Color.White,
                                containerColor = AppColors.SurfaceLight,
                                labelColor = AppColors.TextNavy
                            ),
                            shape = RoundedCornerShape(AppRadius.pill),
                            modifier = Modifier.testTag("filter_chip_$label")
                        )
                    }
                }
            }

            // Reward Catalog Items
            item {
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                AppSectionHeader(
                    title = if (uiState.selectedCategory != null) uiState.selectedCategory!!.title else "All Rewards Catalog"
                )
            }

            if (catalog.isEmpty()) {
                item {
                    AppEmptyState(
                        title = "No Rewards In This Category",
                        description = "Please select another category or check back soon for new inventory."
                    )
                }
            } else {
                items(catalog, key = { it.rewardId }) { reward ->
                    Box(modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal, vertical = AppSpacing.xs)) {
                        AppRewardCard(
                            reward = reward,
                            userBalance = balance,
                            onRedeemClick = { viewModel.openConfirmationModal(reward) },
                            onCardClick = { viewModel.openRewardDetail(reward) }
                        )
                    }
                }
            }
        } else {
            // MY REDEMPTIONS TAB
            item {
                Spacer(modifier = Modifier.height(AppSpacing.md))
                AppSectionHeader(
                    title = "Your Redemption Requests (${history.size})"
                )
            }

            if (history.isEmpty()) {
                item {
                    AppEmptyState(
                        title = "No Redemptions Yet",
                        description = "When you redeem your NestCoins for vouchers or UPI transfers, your tracking status will appear here."
                    )
                }
            } else {
                items(history, key = { it.redemptionId }) { redemption ->
                    Box(modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal, vertical = AppSpacing.xs)) {
                        RedemptionHistoryCard(
                            redemption = redemption,
                            onClick = { viewModel.openRedemptionDetail(redemption) }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            AdBannerContainer(placement = AdPlacement.BANNER_HOME)
        }
    }

    // 1. REWARD DETAIL DIALOG
    if (uiState.selectedRewardDetail != null) {
        val detailReward = uiState.selectedRewardDetail!!
        RewardDetailDialog(
            reward = detailReward,
            userBalance = balance,
            onClose = { viewModel.closeRewardDetail() },
            onRedeemNow = { viewModel.openConfirmationModal(detailReward) }
        )
    }

    // 2. REDEMPTION CONFIRMATION MODAL
    if (uiState.selectedRewardConfirm != null) {
        val confirmReward = uiState.selectedRewardConfirm!!
        RedemptionConfirmationDialog(
            reward = confirmReward,
            currentBalance = balance,
            destinationInput = uiState.destinationAccountInput,
            isSubmitting = uiState.isSubmitting,
            onDestinationChange = { viewModel.updateDestinationInput(it) },
            onConfirm = { viewModel.submitRedemption() },
            onDismiss = { viewModel.closeConfirmationModal() }
        )
    }

    // 3. REDEMPTION DETAIL DIALOG (FOR HISTORY)
    if (uiState.selectedRedemptionDetail != null) {
        val detailReq = uiState.selectedRedemptionDetail!!
        RedemptionRequestDetailDialog(
            redemption = detailReq,
            onClose = { viewModel.closeRedemptionDetail() }
        )
    }

    // 4. SUBMISSION SUCCESS CELEBRATION MODAL
    if (uiState.successRedemption != null) {
        val successReq = uiState.successRedemption!!
        RedemptionSuccessDialog(
            redemption = successReq,
            onViewHistory = {
                viewModel.closeSuccessDialog()
                viewModel.selectTab(RewardsTab.MY_REDEMPTIONS)
            },
            onDismiss = { viewModel.closeSuccessDialog() }
        )
    }
}

/**
 * Card for an individual Redemption Request in the History list.
 */
@Composable
private fun RedemptionHistoryCard(
    redemption: RedemptionRequest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(redemption.createdAt))

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("redemption_item_${redemption.redemptionId}"),
        shape = RoundedCornerShape(AppRadius.card),
        elevation = AppElevation.card,
        contentPadding = AppSpacing.cardPadding,
        backgroundColor = AppColors.SurfaceLight
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = redemption.rewardNameSnapshot,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.TextNavy
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "To: ${redemption.destinationAccount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(AppSpacing.sm))

                RedemptionStatusBadge(status = redemption.status)
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))
            HorizontalDivider(color = AppColors.SurfaceBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(AppSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${CoinConversionHelper.formatCoins(redemption.requiredCoinsSnapshot)} coins deducted",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.TextNavy
                )

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextMuted
                )
            }
        }
    }
}

/**
 * Status Badge with distinct colors.
 */
@Composable
private fun RedemptionStatusBadge(status: RedemptionStatus) {
    val bgColor: Color
    val textColor: Color
    when (status) {
        RedemptionStatus.PENDING -> {
            bgColor = AppColors.GoldCoinLight
            textColor = AppColors.GoldCoinDark
        }
        RedemptionStatus.PROCESSING -> {
            bgColor = AppColors.PrimaryLight
            textColor = AppColors.PrimaryDark
        }
        RedemptionStatus.APPROVED -> {
            bgColor = AppColors.SuccessGreenLight
            textColor = AppColors.SuccessGreenDark
        }
        RedemptionStatus.FULFILLED -> {
            bgColor = AppColors.SuccessGreen
            textColor = Color.White
        }
        RedemptionStatus.REJECTED -> {
            bgColor = AppColors.ActionOrangeLight
            textColor = AppColors.ActionOrangeDark
        }
        RedemptionStatus.CANCELLED -> {
            bgColor = AppColors.SurfaceVariant
            textColor = AppColors.TextSecondary
        }
        RedemptionStatus.REFUNDED -> {
            bgColor = AppColors.AccentPurpleLight
            textColor = AppColors.AccentPurpleDark
        }
    }

    Surface(
        shape = RoundedCornerShape(AppRadius.small),
        color = bgColor
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xxs)
        )
    }
}

/**
 * Detailed Reward View Modal.
 */
@Composable
private fun RewardDetailDialog(
    reward: RedemptionReward,
    userBalance: Long,
    onClose: () -> Unit,
    onRedeemNow: () -> Unit
) {
    val canAfford = userBalance >= reward.requiredCoins
    val coinsRemaining = (reward.requiredCoins - userBalance).coerceAtLeast(0L)
    val progress = CoinConversionHelper.calculateProgressTowards(userBalance, reward.requiredCoins)

    AlertDialog(
        onDismissRequest = onClose,
        shape = RoundedCornerShape(AppRadius.card),
        containerColor = AppColors.SurfaceLight,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IllustrationPlaceholder(
                    gameId = "reward_${reward.partnerBrand.lowercase().replace(" ", "_")}",
                    size = 38.dp
                )
                Spacer(modifier = Modifier.width(AppSpacing.md))
                Column {
                    Text(
                        text = reward.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.TextNavy
                    )
                    Text(
                        text = "Value: ₹${reward.value.toInt()} (${reward.partnerBrand})",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.PrimaryDark
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = reward.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Balance & Requirement Box
                Surface(
                    shape = RoundedCornerShape(AppRadius.card),
                    color = AppColors.BackgroundLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Required Coins:", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                            Text("${CoinConversionHelper.formatCoins(reward.requiredCoins)} NestCoins", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = AppColors.TextNavy)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Your Current Balance:", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                            Text("${CoinConversionHelper.formatCoins(userBalance)} NestCoins", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = AppColors.PrimaryDark)
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                        AppProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(4.dp))

                        if (!canAfford) {
                            Text(
                                text = "You need ${CoinConversionHelper.formatCoins(coinsRemaining)} more NestCoins to unlock this reward.",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.ActionOrangeDark
                            )
                        } else {
                            Text(
                                text = "You have sufficient coins for this redemption!",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AppColors.SuccessGreenDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))

                Text(
                    text = "Important Terms:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.TextNavy
                )
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                reward.termsAndConditions.take(3).forEach { term ->
                    Text(
                        text = "• $term",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        },
        confirmButton = {
            if (canAfford) {
                AppPrimaryButton(
                    text = "Redeem Now",
                    onClick = onRedeemNow,
                    modifier = Modifier.testTag("detail_redeem_now_btn")
                )
            } else {
                AppSmallActionButton(
                    text = "Not Enough Coins",
                    onClick = {},
                    enabled = false,
                    icon = Icons.Filled.Lock,
                    modifier = Modifier.testTag("detail_not_enough_coins_btn")
                )
            }
        },
        dismissButton = {
            AppOutlineButton(
                text = "Close",
                onClick = onClose
            )
        }
    )
}

/**
 * Secure Redemption Confirmation Dialog with exact deduction breakdown.
 */
@Composable
private fun RedemptionConfirmationDialog(
    reward: RedemptionReward,
    currentBalance: Long,
    destinationInput: String,
    isSubmitting: Boolean,
    onDestinationChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val afterDeductionBalance = (currentBalance - reward.requiredCoins).coerceAtLeast(0L)
    val destinationLabel = if (reward.category == RewardCategory.UPI_REWARDS) "UPI ID (e.g. mobile@upi / name@okaxis)" else "Delivery Email Address"

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        shape = RoundedCornerShape(AppRadius.card),
        containerColor = AppColors.SurfaceLight,
        title = {
            Text(
                text = "Redeem ${reward.name}?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = AppColors.TextNavy
            )
        },
        text = {
            Column {
                Text(
                    text = "${CoinConversionHelper.formatCoins(reward.requiredCoins)} NestCoins will be deducted if the redemption request is accepted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Deduction Breakdown Card
                Surface(
                    shape = RoundedCornerShape(AppRadius.card),
                    color = AppColors.BackgroundLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Current balance:", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                            Text("${CoinConversionHelper.formatCoins(currentBalance)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = AppColors.TextNavy)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Coins deducted:", style = MaterialTheme.typography.bodySmall, color = AppColors.ActionOrangeDark)
                            Text("-${CoinConversionHelper.formatCoins(reward.requiredCoins)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = AppColors.ActionOrangeDark)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = AppColors.SurfaceBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("After deduction:", style = MaterialTheme.typography.bodySmall, color = AppColors.TextNavy)
                            Text("${CoinConversionHelper.formatCoins(afterDeductionBalance)} NestCoins", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = AppColors.SuccessGreenDark)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Delivery Input
                OutlinedTextField(
                    value = destinationInput,
                    onValueChange = onDestinationChange,
                    label = { Text(destinationLabel) },
                    placeholder = {
                        Text(if (reward.category == RewardCategory.UPI_REWARDS) "username@okhdfcbank" else "yourname@gmail.com")
                    },
                    singleLine = true,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(AppRadius.small),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = AppColors.SurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("redemption_destination_input")
                )

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                Text(
                    text = "⚠️ Request enters PENDING status for manual security review before delivery.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextMuted
                )
            }
        },
        confirmButton = {
            AppPrimaryButton(
                text = if (isSubmitting) "Processing..." else "Confirm Redemption",
                onClick = onConfirm,
                enabled = !isSubmitting && destinationInput.isNotBlank(),
                modifier = Modifier.testTag("confirm_redemption_btn")
            )
        },
        dismissButton = {
            if (!isSubmitting) {
                AppOutlineButton(
                    text = "Cancel",
                    onClick = onDismiss
                )
            }
        }
    )
}

/**
 * Historical Redemption Request Detail Modal.
 */
@Composable
private fun RedemptionRequestDetailDialog(
    redemption: RedemptionRequest,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onClose,
        shape = RoundedCornerShape(AppRadius.card),
        containerColor = AppColors.SurfaceLight,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Redemption Details",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.TextNavy
                )
                RedemptionStatusBadge(status = redemption.status)
            }
        },
        text = {
            Column {
                Text(
                    text = redemption.rewardNameSnapshot,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.PrimaryDark
                )
                Text(
                    text = "Reward Value: ₹${redemption.rewardValueSnapshot.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                Surface(
                    shape = RoundedCornerShape(AppRadius.card),
                    color = AppColors.BackgroundLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppSpacing.md)) {
                        DetailRow("Reference ID:", redemption.redemptionId.take(16)) {
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Redemption ID", redemption.redemptionId))
                                    Toast.makeText(context, "ID copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy ID", modifier = Modifier.size(14.dp), tint = AppColors.Primary)
                            }
                        }

                        DetailRow("Coins Deducted:", "${CoinConversionHelper.formatCoins(redemption.requiredCoinsSnapshot)} Coins")
                        DetailRow("Destination:", redemption.destinationAccount)
                        DetailRow("Requested At:", dateFormat.format(Date(redemption.createdAt)))

                        redemption.transactionId?.let { txId ->
                            DetailRow("Transaction Ref:", txId.take(14))
                        }

                        if (redemption.processedAt != null) {
                            DetailRow("Processed At:", dateFormat.format(Date(redemption.processedAt)))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Status Context Note
                if (redemption.status == RedemptionStatus.REFUNDED) {
                    Surface(
                        shape = RoundedCornerShape(AppRadius.small),
                        color = AppColors.AccentPurpleLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(AppSpacing.sm)) {
                            Text(
                                text = "Coins Returned to Wallet",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AppColors.AccentPurpleDark
                            )
                            Text(
                                text = "+${CoinConversionHelper.formatCoins(redemption.requiredCoinsSnapshot)} NestCoins have been refunded to your wallet balance.",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                } else if (redemption.status == RedemptionStatus.REJECTED && !redemption.failureReason.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(AppRadius.small),
                        color = AppColors.ActionOrangeLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(AppSpacing.sm)) {
                            Text(
                                text = "Rejection Reason:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AppColors.ActionOrangeDark
                            )
                            Text(
                                text = redemption.failureReason,
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                } else {
                    Text(
                        text = redemption.status.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextMuted
                    )
                }
            }
        },
        confirmButton = {
            AppPrimaryButton(
                text = "Close",
                onClick = onClose
            )
        }
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppColors.TextNavy)
            trailingContent?.invoke()
        }
    }
}

/**
 * Success Dialog displayed immediately after a redemption request is recorded.
 */
@Composable
private fun RedemptionSuccessDialog(
    redemption: RedemptionRequest,
    onViewHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(AppRadius.card),
        containerColor = AppColors.SurfaceLight,
        icon = {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = AppColors.SuccessGreen,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Request Submitted!",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = AppColors.TextNavy
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Your redemption request for ${redemption.rewardNameSnapshot} (₹${redemption.rewardValueSnapshot.toInt()}) has been queued.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                Surface(
                    shape = RoundedCornerShape(AppRadius.small),
                    color = AppColors.GoldCoinLight
                ) {
                    Text(
                        text = "Status: PENDING SECURITY REVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppColors.GoldCoinDark
                        ),
                        modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                Text(
                    text = "${CoinConversionHelper.formatCoins(redemption.requiredCoinsSnapshot)} coins have been deducted from your wallet.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextMuted
                )
            }
        },
        confirmButton = {
            AppPrimaryButton(
                text = "View My Redemptions",
                onClick = onViewHistory,
                modifier = Modifier.testTag("success_view_history_btn")
            )
        },
        dismissButton = {
            AppOutlineButton(
                text = "Done",
                onClick = onDismiss
            )
        }
    )
}
