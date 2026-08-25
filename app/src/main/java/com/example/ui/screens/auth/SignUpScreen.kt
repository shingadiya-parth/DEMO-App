package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserAccount
import com.example.ui.components.AppCard
import com.example.ui.components.AppPrimaryButton
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppRadius
import com.example.ui.theme.AppSpacing

@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onSignUpSuccess: (UserAccount) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.signUpState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.BackgroundLight)
            .verticalScroll(scrollState)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Branding Header
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(AppColors.SuccessGreenLight, AppColors.PrimaryLight)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Stars,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.md))

        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = AppColors.TextNavy
        )

        Text(
            text = "Join thousands of players earning coins every day",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        // Form Card
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = AppColors.SurfaceLight
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Error Alert
                if (state.errorMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                AppColors.ErrorRedLight,
                                RoundedCornerShape(AppRadius.small)
                            )
                            .padding(AppSpacing.sm)
                            .testTag("signup_error_banner"),
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
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                }

                // Name Input
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onSignUpNameChange,
                    label = { Text("Full Name") },
                    placeholder = { Text("Alex Morgan") },
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
                        .testTag("signup_name_input")
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Email Input
                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onSignUpEmailChange,
                    label = { Text("Email Address") },
                    placeholder = { Text("you@example.com") },
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
                        .testTag("signup_email_input")
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Password Input
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onSignUpPasswordChange,
                    label = { Text("Password (min 6 chars)") },
                    placeholder = { Text("••••••••") },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = AppColors.Primary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.toggleSignUpPasswordVisibility() }) {
                            Icon(
                                imageVector = if (state.isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle password visibility",
                                tint = AppColors.TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = AppColors.SurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_password_input")
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Confirm Password Input
                OutlinedTextField(
                    value = state.confirmPassword,
                    onValueChange = viewModel::onSignUpConfirmPasswordChange,
                    label = { Text("Confirm Password") },
                    placeholder = { Text("••••••••") },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = AppColors.Primary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.toggleSignUpConfirmVisibility() }) {
                            Icon(
                                imageVector = if (state.isConfirmVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle confirm password visibility",
                                tint = AppColors.TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (state.isConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = AppColors.SurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_confirm_password_input")
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Optional Referral Code
                OutlinedTextField(
                    value = state.referralCode,
                    onValueChange = viewModel::onSignUpReferralCodeChange,
                    label = { Text("Referral Code (Optional)") },
                    placeholder = { Text("PLAY1234") },
                    leadingIcon = {
                        Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = AppColors.GoldCoin)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = AppColors.SurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_referral_input")
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Terms Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onTermsAcceptedChange(!state.termsAccepted) }
                        .padding(vertical = AppSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.termsAccepted,
                        onCheckedChange = viewModel::onTermsAcceptedChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = AppColors.Primary,
                            uncheckedColor = AppColors.TextMuted
                        ),
                        modifier = Modifier.testTag("signup_terms_checkbox")
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                    Text(
                        text = "I accept the Terms of Service & Privacy Policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextNavy
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Submit Button
                AppPrimaryButton(
                    text = "Create Account",
                    onClick = { viewModel.signUp(onSignUpSuccess) },
                    isLoading = state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_submit_button")
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        // Navigate to Login Link
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Already have an account?",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            TextButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.testTag("navigate_login_button")
            ) {
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.Primary
                )
            }
        }
    }
}
