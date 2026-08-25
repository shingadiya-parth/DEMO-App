package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppPrimaryButton
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing

@Composable
fun ForgotPasswordDialog(
    viewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.forgotPasswordState.collectAsState()
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            viewModel.resetForgotPasswordDialog()
            onDismiss()
        },
        shape = RoundedCornerShape(AppRadius.largeCard),
        containerColor = AppColors.SurfaceLight,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Key,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                Text(
                    text = if (state.step == ResetStep.REQUEST_TOKEN) "Forgot Password" else "Reset Password",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppColors.TextNavy
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("forgot_password_dialog")
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

                if (state.successMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                AppColors.SuccessGreenLight,
                                RoundedCornerShape(AppRadius.small)
                            )
                            .padding(AppSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = AppColors.SuccessGreenDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                        Text(
                            text = state.successMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.SuccessGreenDark
                        )
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                }

                if (state.step == ResetStep.REQUEST_TOKEN) {
                    Text(
                        text = "Enter your registered email address and we'll verify your account to reset your password.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.md))

                    OutlinedTextField(
                        value = state.email,
                        onValueChange = viewModel::onForgotEmailChange,
                        label = { Text("Email Address") },
                        leadingIcon = {
                            Icon(Icons.Filled.Email, contentDescription = null, tint = AppColors.Primary)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Primary,
                            unfocusedBorderColor = AppColors.SurfaceBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_email_input")
                    )
                } else {
                    Text(
                        text = "Enter the 6-digit reset code and your new password below:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    OutlinedTextField(
                        value = state.resetToken,
                        onValueChange = viewModel::onForgotTokenChange,
                        label = { Text("6-Digit Reset Code") },
                        leadingIcon = {
                            Icon(Icons.Filled.Key, contentDescription = null, tint = AppColors.Primary)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Primary,
                            unfocusedBorderColor = AppColors.SurfaceBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_token_input")
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    OutlinedTextField(
                        value = state.newPassword,
                        onValueChange = viewModel::onForgotNewPasswordChange,
                        label = { Text("New Password (min 6 chars)") },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = AppColors.Primary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                                Icon(
                                    imageVector = if (isNewPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Primary,
                            unfocusedBorderColor = AppColors.SurfaceBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_password_input")
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.sm))

                    OutlinedTextField(
                        value = state.confirmPassword,
                        onValueChange = viewModel::onForgotConfirmPasswordChange,
                        label = { Text("Confirm New Password") },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = AppColors.Primary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (isConfirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Primary,
                            unfocusedBorderColor = AppColors.SurfaceBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_new_password_input")
                    )
                }
            }
        },
        confirmButton = {
            if (state.step == ResetStep.REQUEST_TOKEN) {
                AppPrimaryButton(
                    text = "Request Code",
                    onClick = { viewModel.requestPasswordReset() },
                    isLoading = state.isLoading,
                    modifier = Modifier.testTag("request_reset_code_button")
                )
            } else {
                AppPrimaryButton(
                    text = "Reset Password",
                    onClick = {
                        viewModel.completePasswordReset {
                            onDismiss()
                        }
                    },
                    isLoading = state.isLoading,
                    modifier = Modifier.testTag("submit_reset_password_button")
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.resetForgotPasswordDialog()
                    onDismiss()
                }
            ) {
                Text("Cancel", color = AppColors.TextSecondary)
            }
        }
    )
}
