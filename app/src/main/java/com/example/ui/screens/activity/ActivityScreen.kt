package com.example.ui.screens.activity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityType
import com.example.data.model.UserActivityRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    onNavigateBack: () -> Unit,
    onExploreGames: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val activities by viewModel.activities.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Activity History",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("activity_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActivityCategory.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCategory(category) },
                        label = {
                            Text(
                                text = when (category) {
                                    ActivityCategory.ALL -> "All Activities"
                                    ActivityCategory.GAMES -> "Games & Wins"
                                    ActivityCategory.REWARDS -> "Rewards & Bonuses"
                                    ActivityCategory.REFERRALS -> "Referrals"
                                    ActivityCategory.REDEMPTIONS -> "Redemptions"
                                },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("chip_category_${category.name.lowercase()}")
                    )
                }
            }

            if (activities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(88.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.HistoryEdu,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                        Text(
                            text = "No Activities Found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedCategory == ActivityCategory.ALL) {
                                "Your milestones, game wins, bonuses, and redemptions will appear here."
                            } else {
                                "No activity records found under ${selectedCategory.name.lowercase()}."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Button(
                            onClick = onExploreGames,
                            modifier = Modifier.testTag("activity_empty_explore_button")
                        ) {
                            Text("Play Games to Earn")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = activities, key = { it.activityId }) { activity ->
                        ActivityFeedCard(activity = activity)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityFeedCard(
    activity: UserActivityRecord,
    modifier: Modifier = Modifier
) {
    val icon = getActivityIcon(activity.activityType)
    val iconTint = getActivityColor(activity.activityType)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("activity_card_${activity.activityId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    activity.result?.let { resultText ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                resultText.startsWith("+") -> Color(0xFFE8F5E9)
                                resultText.startsWith("-") -> Color(0xFFFFEBEE)
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            }
                        ) {
                            Text(
                                text = resultText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    resultText.startsWith("+") -> Color(0xFF2E7D32)
                                    resultText.startsWith("-") -> Color(0xFFC62828)
                                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatActivityTimestamp(activity.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun getActivityIcon(type: ActivityType): ImageVector {
    return when (type) {
        ActivityType.SPIN_COMPLETED -> Icons.Default.Refresh
        ActivityType.SCRATCH_COMPLETED -> Icons.Default.CardGiftcard
        ActivityType.PUZZLE_COMPLETED -> Icons.Default.Psychology
        ActivityType.COIN_TOSS_COMPLETED -> Icons.Default.MonetizationOn
        ActivityType.TIC_TAC_TOE_COMPLETED -> Icons.Default.GridView
        ActivityType.BUBBLE_POP_COMPLETED -> Icons.Default.BubbleChart
        ActivityType.DAILY_BONUS_CLAIMED -> Icons.Default.CardGiftcard
        ActivityType.AD_REWARD_CLAIMED -> Icons.Default.PlayCircle
        ActivityType.REFERRAL_JOINED,
        ActivityType.REFERRAL_QUALIFIED,
        ActivityType.REFERRAL_BONUS_CREDITED -> Icons.Default.People
        ActivityType.REDEMPTION_REQUESTED,
        ActivityType.REDEMPTION_PROCESSING,
        ActivityType.REDEMPTION_FULFILLED,
        ActivityType.REDEMPTION_REJECTED,
        ActivityType.REDEMPTION_REFUNDED -> Icons.Default.ShoppingBag
        ActivityType.ACCOUNT_CREATED,
        ActivityType.PROFILE_UPDATED -> Icons.Default.AccountCircle
    }
}

private fun getActivityColor(type: ActivityType): Color {
    return when (type) {
        ActivityType.SPIN_COMPLETED,
        ActivityType.SCRATCH_COMPLETED,
        ActivityType.PUZZLE_COMPLETED,
        ActivityType.COIN_TOSS_COMPLETED,
        ActivityType.TIC_TAC_TOE_COMPLETED,
        ActivityType.BUBBLE_POP_COMPLETED -> Color(0xFF1976D2) // Blue
        ActivityType.DAILY_BONUS_CLAIMED -> Color(0xFFF57C00) // Orange
        ActivityType.AD_REWARD_CLAIMED -> Color(0xFF0097A7) // Teal
        ActivityType.REFERRAL_JOINED,
        ActivityType.REFERRAL_QUALIFIED,
        ActivityType.REFERRAL_BONUS_CREDITED -> Color(0xFF7B1FA2) // Purple
        ActivityType.REDEMPTION_REQUESTED,
        ActivityType.REDEMPTION_PROCESSING,
        ActivityType.REDEMPTION_FULFILLED,
        ActivityType.REDEMPTION_REJECTED,
        ActivityType.REDEMPTION_REFUNDED -> Color(0xFFC2185B) // Pink
        ActivityType.ACCOUNT_CREATED,
        ActivityType.PROFILE_UPDATED -> Color(0xFF455A64) // Slate
    }
}

private fun formatActivityTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000L -> "Just now"
        diff < 3600_000L -> "${diff / 60_000L}m ago"
        diff < 86400_000L -> "${diff / 3600_000L}h ago"
        diff < 172800_000L -> "Yesterday"
        else -> {
            val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
