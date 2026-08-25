package com.example.ui.screens.play

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameCategory
import com.example.data.model.GameDefinition
import com.example.services.ads.AdPlacement
import com.example.ui.components.AdBannerContainer
import com.example.ui.components.AppCard
import com.example.ui.components.AppGameCard
import com.example.ui.components.AppSectionHeader
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing

@Composable
fun PlayScreen(
    viewModel: PlayViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val todayStats by viewModel.todayStats.collectAsState()

    val filteredGames = if (uiState.selectedCategory != null) {
        viewModel.games.filter { it.category == uiState.selectedCategory }
    } else {
        viewModel.games
    }

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
            .testTag("play_screen_content"),
        contentPadding = PaddingValues(bottom = AppSpacing.xxxl)
    ) {
        // Section Description Banner
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
                            imageVector = Icons.Filled.SportsEsports,
                            contentDescription = null,
                            tint = AppColors.Primary
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        Text(
                            text = "Play & Earn Arena",
                            style = MaterialTheme.typography.titleLarge,
                            color = AppColors.TextNavy
                        )
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Text(
                        text = "8 original in-app games. Play, achieve targets, and earn verified coins directly to your wallet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }

        // Category Filter Chips
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = AppSpacing.screenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text("All Games (${viewModel.games.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.Primary,
                            selectedLabelColor = Color.White,
                            containerColor = AppColors.SurfaceLight,
                            labelColor = AppColors.TextNavy
                        ),
                        shape = RoundedCornerShape(AppRadius.pill)
                    )
                }
                items(GameCategory.entries) { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category.title) },
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

        // Ad Banner Slot
        item {
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            AdBannerContainer(placement = AdPlacement.BANNER_GAMES)
        }

        // All 8 In-App Games List
        items(filteredGames, key = { it.gameId }) { game ->
            val stat = todayStats.find { it.gameId == game.gameId }
            val playsDone = stat?.playsCount ?: 0
            val playsRemaining = (game.maxDailyPlays - playsDone).coerceAtLeast(0)

            Box(modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal, vertical = AppSpacing.xs)) {
                AppGameCard(
                    game = game,
                    playsRemaining = playsRemaining,
                    onPlayClick = { viewModel.openGamePreview(game) }
                )
            }
        }
    }
}
