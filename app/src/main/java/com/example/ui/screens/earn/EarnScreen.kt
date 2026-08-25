package com.example.ui.screens.earn

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.services.ads.AdPlacement
import com.example.ui.components.AdBannerContainer
import com.example.ui.components.AppCard
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.AppSectionHeader
import com.example.ui.components.AppSmallActionButton
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppElevation
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing

@Composable
fun EarnScreen(
    viewModel: EarnViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    var referralInput by remember { mutableStateOf("") }

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
            .testTag("earn_screen_content"),
        contentPadding = PaddingValues(bottom = AppSpacing.xxxl)
    ) {
        // Section Header Info Card
        item {
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.screenHorizontal),
                backgroundColor = AppColors.SurfaceLight
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint = AppColors.Primary
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        Text(
                            text = "In-App Earning Activities",
                            style = MaterialTheme.typography.titleLarge,
                            color = AppColors.TextNavy
                        )
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Text(
                        text = "Earn bonus coins through daily check-in streaks, inviting friends, and community coin giveaways. 100% internal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }

        // 1. Daily Bonus 7-Day Streak Ladder
        item {
            AppSectionHeader(title = "1. Daily Check-in Streak")

            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenHorizontal),
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
                                text = "7-Day Consecutive Bonus",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AppColors.TextNavy
                            )
                            Text(
                                text = "Check in every day to claim increasing coin amounts",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                        }

                        AppSmallActionButton(
                            text = "Claim Day 1",
                            onClick = { viewModel.claimStreak(1) },
                            backgroundColor = AppColors.SuccessGreen,
                            modifier = Modifier.testTag("earn_claim_day_1_btn")
                        )
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    // 7 Day Streak Tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val streakRewards = listOf(50L, 75L, 100L, 150L, 200L, 300L, 500L)
                        streakRewards.forEachIndexed { index, coins ->
                            val day = index + 1
                            Surface(
                                shape = RoundedCornerShape(AppRadius.small),
                                color = if (day == 1) AppColors.Primary else AppColors.SurfaceVariant,
                                modifier = Modifier.weight(1f).padding(2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = AppSpacing.sm),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "D$day",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (day == 1) Color.White else AppColors.TextNavy
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "+$coins",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            color = if (day == 1) AppColors.GoldCoinLight else AppColors.TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Referral System Card
        item {
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            AppSectionHeader(title = "2. Invite Friends (Referral)")

            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenHorizontal),
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.PrimaryLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Group,
                                    contentDescription = null,
                                    tint = AppColors.Primary,
                                    modifier = Modifier.size(22.dp)
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
                                    text = user?.referralCode ?: "PLAY8921",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = AppColors.Primary
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(AppRadius.small),
                            color = AppColors.GoldCoinLight
                        ) {
                            Text(
                                text = "+350 Coins Each",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AppColors.GoldCoinDark,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xxs)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        OutlinedTextField(
                            value = referralInput,
                            onValueChange = { referralInput = it },
                            placeholder = { Text("Enter a Friend's Code", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(AppRadius.small),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppColors.Primary,
                                unfocusedBorderColor = AppColors.SurfaceBorder
                            ),
                            modifier = Modifier.weight(1f).testTag("referral_input_field")
                        )

                        AppSmallActionButton(
                            text = "Apply",
                            onClick = {
                                viewModel.applyReferral(referralInput)
                                referralInput = ""
                            },
                            modifier = Modifier.testTag("apply_referral_btn")
                        )
                    }
                }
            }
        }

        // 3. Community Giveaway Card
        item {
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            AppSectionHeader(title = "3. Community Coin Giveaway")

            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenHorizontal),
                backgroundColor = AppColors.GoldCoinLight.copy(alpha = 0.5f),
                border = null
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
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(AppColors.GoldCoin),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CardGiftcard,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(AppSpacing.md))
                        Column {
                            Text(
                                text = "Daily 1,000 Coin Pot",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AppColors.GoldCoinDark
                            )
                            Text(
                                text = "Free entry for active players every 24h",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                        }
                    }

                    AppSmallActionButton(
                        text = "Enter",
                        onClick = { viewModel.claimStreak(1) },
                        backgroundColor = AppColors.GoldCoinDark,
                        modifier = Modifier.testTag("enter_giveaway_btn")
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(AppSpacing.lg))
            AdBannerContainer(placement = AdPlacement.BANNER_HOME)
        }
    }
}
