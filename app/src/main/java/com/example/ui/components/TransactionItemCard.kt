package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CoinTransaction
import com.example.data.model.TransactionType
import com.example.domain.engine.CoinConversionHelper
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionItemCard(
    transaction: CoinTransaction,
    modifier: Modifier = Modifier
) {
    val isCredit = transaction.amount >= 0
    val amountPrefix = if (isCredit) "+" else "-"
    val absAmount = kotlin.math.abs(transaction.amount)
    val amountColor = if (isCredit) AppColors.SuccessGreenDark else AppColors.ErrorRed
    val formattedDate = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(transaction.createdAt))

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tx_item_${transaction.transactionId}"),
        shape = RoundedCornerShape(AppRadius.small),
        backgroundColor = AppColors.SurfaceLight,
        contentPadding = AppSpacing.compactCardPadding
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
                // Icon representing the transaction source
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCredit) AppColors.SuccessGreenLight else AppColors.ErrorRedLight
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (transaction.type) {
                            TransactionType.GAME_REWARD -> Icons.Filled.Gamepad
                            TransactionType.DAILY_BONUS -> Icons.Filled.Star
                            TransactionType.SPIN_REWARD -> Icons.Filled.Casino
                            TransactionType.SCRATCH_REWARD -> Icons.Filled.Extension
                            TransactionType.PUZZLE_REWARD -> Icons.Filled.Gamepad
                            TransactionType.REFERRAL_REWARD -> Icons.Filled.TrendingUp
                            TransactionType.AD_REWARD -> Icons.Filled.OndemandVideo
                            TransactionType.GIVEAWAY_REWARD -> Icons.Filled.CardGiftcard
                            TransactionType.REDEMPTION_DEDUCTION -> Icons.Filled.Redeem
                            TransactionType.ADMIN_ADJUSTMENT -> Icons.Filled.Security
                            TransactionType.REVERSAL -> Icons.Filled.History
                        },
                        contentDescription = transaction.type.name,
                        tint = if (isCredit) AppColors.SuccessGreenDark else AppColors.ErrorRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(AppSpacing.md))

                Column {
                    Text(
                        text = transaction.type.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = AppColors.TextNavy
                    )
                    Text(
                        text = "$formattedDate • ${transaction.status.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }

            // Amount Column
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${CoinConversionHelper.formatCoins(absAmount)} Coins",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                )
                Text(
                    text = "Bal: ${CoinConversionHelper.formatCoins(transaction.balanceAfter)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = AppColors.TextMuted
                )
            }
        }
    }
}
