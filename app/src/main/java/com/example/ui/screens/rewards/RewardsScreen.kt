package com.example.ui.screens.rewards

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RedemptionReward
import com.example.domain.engine.CoinConversionHelper
import com.example.services.ads.AdPlacement
import com.example.ui.components.AdBannerContainer
import com.example.ui.components.AppCard
import com.example.ui.components.AppEmptyState
import com.example.ui.components.AppHeroCard
import com.example.ui.components.AppOutlineButton
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.AppRewardCard
import com.example.ui.components.AppSectionHeader
import com.example.ui.components.HeroCoinBalance
import com.example.ui.components.RewardGoalCard
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppElevation
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing

@Composable
fun RewardsScreen(
    viewModel: RewardsViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val balance by viewModel.currentBalance.collectAsState()
    val history by viewModel.redemptionHistory.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }

    val catalog = viewModel.getCatalog()
    val categories = listOf("All", "Gift Cards", "UPI / Cash", "Gaming")

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
        // 1. Balance Summary & Conversion Card
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
                            text = "Redeemable Wallet Balance",
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

        // 2. Next Goal Milestone Progress Card
        item {
            Box(modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal)) {
                RewardGoalCard(
                    currentCoins = balance,
                    targetCoins = 7000L,
                    rewardTitle = "₹10 Google Play / Amazon Voucher"
                )
            }
        }

        // 3. Category Filter Chips
        item {
            Spacer(modifier = Modifier.height(AppSpacing.md))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = AppSpacing.screenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.Primary,
                            selectedLabelColor = Color.White,
                            containerColor = AppColors.SurfaceLight,
                            labelColor = AppColors.TextNavy
                        ),
                        shape = RoundedCornerShape(AppRadius.pill)
                    )
                }
            }
        }

        // 4. Reward Catalog List
        item {
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            AppSectionHeader(title = "Available Vouchers")
        }

        items(catalog, key = { it.rewardId }) { reward ->
            Box(modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal, vertical = AppSpacing.xs)) {
                AppRewardCard(
                    reward = reward,
                    userBalance = balance,
                    onRedeemClick = { viewModel.openRedeemDialog(reward) }
                )
            }
        }

        // 5. Redemption Requests History
        item {
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            AppSectionHeader(title = "Redemption History (${history.size})")
        }

        if (history.isEmpty()) {
            item {
                AppEmptyState(
                    title = "No Redemptions Yet",
                    description = "When you redeem coins for digital vouchers, your requests will appear here."
                )
            }
        } else {
            items(history, key = { it.id }) { req ->
                Box(modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal, vertical = AppSpacing.xs)) {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = AppColors.SurfaceLight
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = req.rewardName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AppColors.TextNavy
                                )
                                Text(
                                    text = "Destination: ${req.destinationAccount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextSecondary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(AppRadius.small),
                                color = AppColors.SuccessGreenLight
                            ) {
                                Text(
                                    text = req.status.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AppColors.SuccessGreenDark,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xxs)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            AdBannerContainer(placement = AdPlacement.BANNER_HOME)
        }
    }

    // Modal Redemption Dialog
    if (uiState.isRedemptionDialogVisible && uiState.selectedReward != null) {
        val activeReward = uiState.selectedReward!!

        AlertDialog(
            onDismissRequest = { viewModel.closeRedeemDialog() },
            shape = RoundedCornerShape(AppRadius.bottomSheet),
            containerColor = AppColors.SurfaceLight,
            title = {
                Text(
                    text = "Confirm Voucher Redemption",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.TextNavy
                )
            },
            text = {
                Column {
                    Text(
                        text = "Redeeming ${activeReward.rewardName} for ${CoinConversionHelper.formatCoins(activeReward.requiredCoins)} coins (₹${activeReward.rewardValueInr.toInt()}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    OutlinedTextField(
                        value = uiState.destinationAccountInput,
                        onValueChange = { viewModel.updateDestinationInput(it) },
                        label = { Text("Delivery Email / Phone") },
                        placeholder = { Text("user@example.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(AppRadius.small),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Primary,
                            unfocusedBorderColor = AppColors.SurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("redemption_destination_input")
                    )
                }
            },
            confirmButton = {
                AppPrimaryButton(
                    text = "Confirm",
                    onClick = { viewModel.submitRedemption() },
                    modifier = Modifier.testTag("submit_redemption_confirm_button")
                )
            },
            dismissButton = {
                AppOutlineButton(
                    text = "Cancel",
                    onClick = { viewModel.closeRedeemDialog() }
                )
            }
        )
    }
}
