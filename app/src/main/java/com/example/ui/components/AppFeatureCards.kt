package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameDefinition
import com.example.data.model.GameDifficulty
import com.example.data.model.RedemptionReward
import com.example.domain.engine.CoinConversionHelper
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppElevation
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing

/**
 * Reusable Game Card Component used across Home and Play screens.
 */
@Composable
fun AppGameCard(
    game: GameDefinition,
    playsRemaining: Int,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlayClick() }
            .testTag("app_game_card_${game.gameId}"),
        shape = RoundedCornerShape(AppRadius.card),
        elevation = AppElevation.card,
        contentPadding = AppSpacing.cardPadding
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
                IllustrationPlaceholder(
                    gameId = game.gameId,
                    size = 52.dp
                )

                Spacer(modifier = Modifier.width(AppSpacing.md))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = game.gameName,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                            color = AppColors.TextNavy
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Surface(
                            shape = RoundedCornerShape(AppRadius.small),
                            color = when (game.difficulty) {
                                GameDifficulty.EASY -> AppColors.SuccessGreenLight
                                GameDifficulty.MEDIUM -> AppColors.GoldCoinLight
                                GameDifficulty.HARD -> AppColors.ActionOrangeLight
                            }
                        ) {
                            Text(
                                text = game.difficulty.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = when (game.difficulty) {
                                        GameDifficulty.EASY -> AppColors.SuccessGreenDark
                                        GameDifficulty.MEDIUM -> AppColors.GoldCoinDark
                                        GameDifficulty.HARD -> AppColors.ActionOrangeDark
                                    }
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = game.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AvTimer,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = AppColors.TextMuted
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$playsRemaining plays remaining today",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = AppColors.TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(AppSpacing.sm))

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(AppRadius.small),
                    color = AppColors.GoldCoinLight
                ) {
                    Text(
                        text = "+${game.baseRewardCoins} Coins",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = AppColors.GoldCoinDark
                        ),
                        modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xxs)
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                AppSmallActionButton(
                    text = "Play",
                    onClick = onPlayClick,
                    icon = Icons.Filled.PlayArrow,
                    modifier = Modifier.testTag("play_game_btn_${game.gameId}")
                )
            }
        }
    }
}

/**
 * Reusable Reward Voucher Card Component used on Rewards screen.
 */
@Composable
fun AppRewardCard(
    reward: RedemptionReward,
    userBalance: Long,
    onRedeemClick: () -> Unit,
    onCardClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val canAfford = userBalance >= reward.requiredCoins
    val progress = CoinConversionHelper.calculateProgressTowards(userBalance, reward.requiredCoins)
    val remainingCoins = (reward.requiredCoins - userBalance).coerceAtLeast(0L)
    val isStockAvailable = reward.stockStatus == com.example.data.model.RewardStockStatus.AVAILABLE ||
            reward.stockStatus == com.example.data.model.RewardStockStatus.LOW_STOCK

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onCardClick != null) { onCardClick?.invoke() }
            .testTag("reward_card_${reward.rewardId}"),
        shape = RoundedCornerShape(AppRadius.card),
        elevation = AppElevation.card,
        contentPadding = AppSpacing.cardPadding
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IllustrationPlaceholder(gameId = "reward_${reward.partnerBrand.lowercase().replace(" ", "_")}", size = 44.dp)
                    Spacer(modifier = Modifier.width(AppSpacing.md))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = reward.rewardName,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                color = AppColors.TextNavy
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = reward.partnerBrand,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                            if (reward.stockStatus == com.example.data.model.RewardStockStatus.LOW_STOCK) {
                                Spacer(modifier = Modifier.width(AppSpacing.xs))
                                Surface(
                                    shape = RoundedCornerShape(AppRadius.small),
                                    color = AppColors.ActionOrangeLight
                                ) {
                                    Text(
                                        text = reward.stockStatus.badgeText,
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
                    }
                }

                Spacer(modifier = Modifier.width(AppSpacing.sm))

                Surface(
                    shape = RoundedCornerShape(AppRadius.small),
                    color = AppColors.PrimaryLight
                ) {
                    Text(
                        text = "₹${reward.rewardValueInr.toInt()}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = AppColors.PrimaryDark
                        ),
                        modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            Text(
                text = reward.description,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Progress bar and remaining coins display
            AppProgressBar(
                progress = progress,
                progressColor = if (canAfford) AppColors.SuccessGreen else AppColors.Primary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (canAfford) {
                        "${CoinConversionHelper.formatCoins(reward.requiredCoins)} / ${CoinConversionHelper.formatCoins(reward.requiredCoins)} coins"
                    } else {
                        "${CoinConversionHelper.formatCoins(userBalance)} / ${CoinConversionHelper.formatCoins(reward.requiredCoins)} coins"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary
                )

                Text(
                    text = if (canAfford) "Goal Reached!" else "${CoinConversionHelper.formatCoins(remainingCoins)} coins remaining",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (canAfford) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (canAfford) AppColors.SuccessGreenDark else AppColors.TextMuted
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${CoinConversionHelper.formatCoins(reward.requiredCoins)} NestCoins",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.TextNavy
                )

                if (!isStockAvailable) {
                    AppSmallActionButton(
                        text = "Out of Stock",
                        onClick = {},
                        enabled = false,
                        icon = Icons.Filled.Lock,
                        modifier = Modifier.testTag("redeem_btn_outofstock_${reward.rewardId}")
                    )
                } else if (canAfford) {
                    AppSmallActionButton(
                        text = "Redeem",
                        onClick = onRedeemClick,
                        backgroundColor = AppColors.SuccessGreen,
                        modifier = Modifier.testTag("redeem_btn_${reward.rewardId}")
                    )
                } else {
                    AppSmallActionButton(
                        text = "Need ${CoinConversionHelper.formatCoins(remainingCoins)}",
                        onClick = { onCardClick?.invoke() },
                        enabled = true,
                        icon = Icons.Filled.Lock,
                        backgroundColor = AppColors.PrimaryLight,
                        contentColor = AppColors.PrimaryDark,
                        modifier = Modifier.testTag("redeem_btn_locked_${reward.rewardId}")
                    )
                }
            }
        }
    }
}
