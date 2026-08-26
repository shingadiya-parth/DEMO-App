package com.example.ui.screens.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CoinTransaction
import com.example.data.model.TransactionType
import com.example.data.repository.EarningsSummary
import com.example.data.repository.TransactionFilter
import com.example.domain.engine.CoinConversionHelper
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit,
    onNavigateToEarn: () -> Unit = {},
    onNavigateToRewards: () -> Unit = {},
    snackbarHostState: SnackbarHostState
) {
    val summary by viewModel.liveSummary.collectAsState()
    val transactions by viewModel.liveTransactions.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Coin Wallet & Ledger",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.TextNavy
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("wallet_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColors.TextNavy
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = AppColors.BackgroundLight,
        modifier = Modifier.testTag("wallet_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            // Main Authoritative Balance Card
            item {
                WalletHeroCard(
                    summary = summary,
                    onNavigateToEarn = onNavigateToEarn,
                    onNavigateToRewards = onNavigateToRewards
                )
            }

            // Lifetime Earnings & Spend Summary Row
            item {
                LifetimeStatsRow(
                    lifetimeEarned = summary.lifetimeEarned,
                    lifetimeSpent = summary.lifetimeSpent
                )
            }

            // Ledger Integrity Banner
            item {
                LedgerSecurityCard()
            }

            // Filter Chips Section Header
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Transaction History",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.TextNavy
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    TransactionFilterChips(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { viewModel.setFilter(it) }
                    )
                }
            }

            // Transaction History Items
            if (transactions.isEmpty()) {
                item {
                    EmptyTransactionsState(selectedFilter = selectedFilter)
                }
            } else {
                items(
                    items = transactions,
                    key = { it.transactionId }
                ) { transaction ->
                    EnhancedTransactionCard(transaction = transaction)
                }
            }

            item {
                Spacer(modifier = Modifier.height(AppSpacing.xl))
            }
        }
    }
}

@Composable
private fun WalletHeroCard(
    summary: EarningsSummary,
    onNavigateToEarn: () -> Unit,
    onNavigateToRewards: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wallet_balance_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(AppColors.NavyCard, AppColors.Primary)
                    )
                )
                .padding(AppSpacing.lg)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL WALLET BALANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Audited Ledger",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                // Big Coins display
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.MonetizationOn,
                        contentDescription = "Coin Balance",
                        tint = AppColors.GoldCoin,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(
                        text = CoinConversionHelper.formatCoins(summary.balance),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 34.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Coins",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Rupee Equivalent
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AppColors.GoldCoin.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Estimated Value: ${summary.currencyEstimate} (${summary.rateExplanation})",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = AppColors.GoldLight,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onNavigateToEarn() }
                            .testTag("wallet_earn_more_button"),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Earn Coins",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onNavigateToRewards() }
                            .testTag("wallet_redeem_rewards_button"),
                        color = AppColors.GoldCoin
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CardGiftcard,
                                contentDescription = null,
                                tint = AppColors.TextNavy,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Redeem Cash",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = AppColors.TextNavy
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LifetimeStatsRow(
    lifetimeEarned: Long,
    lifetimeSpent: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        // Lifetime Earned
        Card(
            modifier = Modifier
                .weight(1f)
                .testTag("lifetime_earned_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(AppSpacing.md)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Total Earned",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.TextSecondary
                    )
                    Icon(
                        imageVector = Icons.Filled.AddCircle,
                        contentDescription = null,
                        tint = AppColors.EmeraldGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "+${CoinConversionHelper.formatCoins(lifetimeEarned)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppColors.EmeraldGreen
                    )
                )

                Text(
                    text = "≈ ${CoinConversionHelper.getCurrencyEstimate(lifetimeEarned)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }

        // Lifetime Spent
        Card(
            modifier = Modifier
                .weight(1f)
                .testTag("lifetime_spent_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(AppSpacing.md)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Total Redeemed",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.TextSecondary
                    )
                    Icon(
                        imageVector = Icons.Filled.RemoveCircle,
                        contentDescription = null,
                        tint = AppColors.CoralRed,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "-${CoinConversionHelper.formatCoins(lifetimeSpent)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = AppColors.CoralRed
                    )
                )

                Text(
                    text = "≈ ${CoinConversionHelper.getCurrencyEstimate(lifetimeSpent)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun LedgerSecurityCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Primary.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Single Source of Truth: All balance changes are cryptographically recorded in the immutable database ledger.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextNavy
            )
        }
    }
}

@Composable
private fun TransactionFilterChips(
    selectedFilter: TransactionFilter,
    onFilterSelected: (TransactionFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        FilterChip(
            selected = selectedFilter == TransactionFilter.ALL,
            onClick = { onFilterSelected(TransactionFilter.ALL) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AppColors.Primary,
                selectedLabelColor = Color.White
            ),
            modifier = Modifier.testTag("filter_all")
        )

        FilterChip(
            selected = selectedFilter == TransactionFilter.EARNED,
            onClick = { onFilterSelected(TransactionFilter.EARNED) },
            label = { Text("Earned (+)") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AppColors.EmeraldGreen,
                selectedLabelColor = Color.White
            ),
            modifier = Modifier.testTag("filter_earned")
        )

        FilterChip(
            selected = selectedFilter == TransactionFilter.SPENT,
            onClick = { onFilterSelected(TransactionFilter.SPENT) },
            label = { Text("Redeemed (-)") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AppColors.CoralRed,
                selectedLabelColor = Color.White
            ),
            modifier = Modifier.testTag("filter_spent")
        )
    }
}

@Composable
private fun EnhancedTransactionCard(transaction: CoinTransaction) {
    val isCredit = transaction.amount >= 0
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(transaction.createdAt) { dateFormat.format(Date(transaction.createdAt)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.transactionId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on Transaction Type
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCredit) AppColors.EmeraldLight.copy(alpha = 0.4f)
                        else AppColors.CoralLight.copy(alpha = 0.4f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (transaction.type) {
                        TransactionType.GAME_REWARD -> Icons.Filled.Gamepad
                        TransactionType.SPIN_REWARD -> Icons.Filled.Refresh
                        TransactionType.SCRATCH_REWARD -> Icons.Filled.Stars
                        TransactionType.PUZZLE_REWARD -> Icons.Filled.PlayCircleOutline
                        TransactionType.COIN_TOSS_REWARD -> Icons.Filled.MonetizationOn
                        TransactionType.TIC_TAC_TOE_REWARD -> Icons.Filled.Gamepad
                        TransactionType.BUBBLE_POP_REWARD -> Icons.Filled.Gamepad
                        TransactionType.DAILY_BONUS -> Icons.Filled.MonetizationOn
                        TransactionType.REFERRAL_REWARD -> Icons.Filled.TrendingUp
                        TransactionType.AD_REWARD -> Icons.Filled.PlayCircleOutline
                        TransactionType.GIVEAWAY_REWARD -> Icons.Filled.CardGiftcard
                        TransactionType.REDEMPTION_DEDUCTION -> Icons.Filled.CardGiftcard
                        TransactionType.ADMIN_ADJUSTMENT -> Icons.Filled.Security
                        TransactionType.REVERSAL -> Icons.Filled.History
                    },
                    contentDescription = transaction.type.displayName,
                    tint = if (isCredit) AppColors.EmeraldGreen else AppColors.CoralRed,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(AppSpacing.sm))

            // Transaction Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.type.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.TextNavy
                )
                Text(
                    text = transaction.metadata ?: "Source: ${transaction.source}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(AppSpacing.xs))

            // Amount & Balance After
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = if (isCredit) "+${CoinConversionHelper.formatCoins(transaction.amount)}"
                    else "-${CoinConversionHelper.formatCoins(kotlin.math.abs(transaction.amount))}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isCredit) AppColors.EmeraldGreen else AppColors.CoralRed
                    )
                )
                Text(
                    text = "Bal: ${CoinConversionHelper.formatCoins(transaction.balanceAfter)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun EmptyTransactionsState(selectedFilter: TransactionFilter) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.xxl, horizontal = AppSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = AppColors.TextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(AppSpacing.md))
            Text(
                text = when (selectedFilter) {
                    TransactionFilter.ALL -> "No Transactions Yet"
                    TransactionFilter.EARNED -> "No Coins Earned Yet"
                    TransactionFilter.SPENT -> "No Redemptions Made Yet"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = AppColors.TextNavy
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            Text(
                text = "Play games and complete daily activities to see your ledger fill up!",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
    }
}
