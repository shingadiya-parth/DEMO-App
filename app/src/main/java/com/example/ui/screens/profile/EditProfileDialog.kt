package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppPrimaryButton
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing

private val AVATAR_OPTIONS = listOf(
    Pair("avatar_1", Icons.Filled.AccountCircle),
    Pair("avatar_2", Icons.Filled.Face),
    Pair("avatar_3", Icons.Filled.SentimentVerySatisfied),
    Pair("avatar_4", Icons.Filled.EmojiEmotions),
    Pair("avatar_5", Icons.Filled.SportsEsports),
    Pair("avatar_6", Icons.Filled.Star)
)

@Composable
fun EditProfileDialog(
    viewModel: ProfileViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(AppRadius.largeCard),
        containerColor = AppColors.SurfaceLight,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.TextNavy
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_profile_dialog")
            ) {
                if (state.errorMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                AppColors.ErrorRedLight,
                                RoundedCornerShape(AppRadius.small)
                            )
                            .padding(AppSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = AppColors.ErrorRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Text(
                            text = state.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.ErrorRed
                        )
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                }

                Text(
                    text = "Choose Avatar:",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(AppSpacing.xs))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AVATAR_OPTIONS.forEach { (avatarKey, icon) ->
                        val isSelected = state.editAvatar == avatarKey
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) AppColors.PrimaryLight else AppColors.BackgroundLight)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) AppColors.Primary else AppColors.SurfaceBorder,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.onAvatarChange(avatarKey) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = avatarKey,
                                tint = if (isSelected) AppColors.Primary else AppColors.TextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Display Name
                OutlinedTextField(
                    value = state.editDisplayName,
                    onValueChange = viewModel::onDisplayNameChange,
                    label = { Text("Display Name") },
                    leadingIcon = {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = AppColors.Primary)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = AppColors.SurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_display_name_input")
                )

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                // Country Code
                OutlinedTextField(
                    value = state.editCountry,
                    onValueChange = viewModel::onCountryChange,
                    label = { Text("Country Code (e.g. IN, US)") },
                    leadingIcon = {
                        Icon(Icons.Filled.Flag, contentDescription = null, tint = AppColors.Primary)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = AppColors.SurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_country_input")
                )

                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = "🔒 Balance, email, referral code and lifetime stats are protected and cannot be modified directly.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = AppColors.TextMuted
                )
            }
        },
        confirmButton = {
            AppPrimaryButton(
                text = "Save Changes",
                onClick = { viewModel.saveProfile() },
                isLoading = state.isSaving,
                modifier = Modifier.testTag("save_profile_button")
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppColors.TextSecondary)
            }
        }
    )
}
