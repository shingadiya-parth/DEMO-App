package com.example.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserAccount
import com.example.data.repository.AuthException
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val referralCode: String = "",
    val termsAccepted: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isConfirmVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

data class ForgotPasswordUiState(
    val email: String = "",
    val resetToken: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val step: ResetStep = ResetStep.REQUEST_TOKEN,
    val generatedTokenForDev: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

enum class ResetStep {
    REQUEST_TOKEN,
    ENTER_NEW_PASSWORD
}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _signUpState = MutableStateFlow(SignUpUiState())
    val signUpState: StateFlow<SignUpUiState> = _signUpState.asStateFlow()

    private val _forgotPasswordState = MutableStateFlow(ForgotPasswordUiState())
    val forgotPasswordState: StateFlow<ForgotPasswordUiState> = _forgotPasswordState.asStateFlow()

    // --- Login Handlers ---
    fun onLoginEmailChange(value: String) {
        _loginState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onLoginPasswordChange(value: String) {
        _loginState.update { it.copy(password = value, errorMessage = null) }
    }

    fun toggleLoginPasswordVisibility() {
        _loginState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun login(onSuccess: (UserAccount) -> Unit) {
        val state = _loginState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _loginState.update { it.copy(errorMessage = "Please enter both email and password.") }
            return
        }

        _loginState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.login(state.email, state.password)
            result.onSuccess { user ->
                _loginState.update { it.copy(isLoading = false, isSuccess = true, errorMessage = null) }
                onSuccess(user)
            }.onFailure { error ->
                _loginState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Login failed. Please check your credentials."
                    )
                }
            }
        }
    }

    // --- Sign Up Handlers ---
    fun onSignUpNameChange(value: String) {
        _signUpState.update { it.copy(name = value, errorMessage = null) }
    }

    fun onSignUpEmailChange(value: String) {
        _signUpState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onSignUpPasswordChange(value: String) {
        _signUpState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onSignUpConfirmPasswordChange(value: String) {
        _signUpState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun onSignUpReferralCodeChange(value: String) {
        _signUpState.update { it.copy(referralCode = value.uppercase(), errorMessage = null) }
    }

    fun onTermsAcceptedChange(accepted: Boolean) {
        _signUpState.update { it.copy(termsAccepted = accepted, errorMessage = null) }
    }

    fun toggleSignUpPasswordVisibility() {
        _signUpState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleSignUpConfirmVisibility() {
        _signUpState.update { it.copy(isConfirmVisible = !it.isConfirmVisible) }
    }

    fun signUp(onSuccess: (UserAccount) -> Unit) {
        val state = _signUpState.value
        if (!state.termsAccepted) {
            _signUpState.update { it.copy(errorMessage = "You must accept the Terms and Privacy Policy.") }
            return
        }

        _signUpState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.signUp(
                name = state.name,
                email = state.email,
                password = state.password,
                confirmPassword = state.confirmPassword,
                referralCode = state.referralCode.ifBlank { null }
            )
            result.onSuccess { user ->
                _signUpState.update { it.copy(isLoading = false, isSuccess = true, errorMessage = null) }
                onSuccess(user)
            }.onFailure { error ->
                _signUpState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to create account."
                    )
                }
            }
        }
    }

    // --- Forgot Password Handlers ---
    fun onForgotEmailChange(value: String) {
        _forgotPasswordState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onForgotTokenChange(value: String) {
        _forgotPasswordState.update { it.copy(resetToken = value, errorMessage = null) }
    }

    fun onForgotNewPasswordChange(value: String) {
        _forgotPasswordState.update { it.copy(newPassword = value, errorMessage = null) }
    }

    fun onForgotConfirmPasswordChange(value: String) {
        _forgotPasswordState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun requestPasswordReset() {
        val state = _forgotPasswordState.value
        if (state.email.isBlank()) {
            _forgotPasswordState.update { it.copy(errorMessage = "Please enter your email.") }
            return
        }

        _forgotPasswordState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.requestPasswordReset(state.email)
            result.onSuccess { token ->
                _forgotPasswordState.update {
                    it.copy(
                        isLoading = false,
                        step = ResetStep.ENTER_NEW_PASSWORD,
                        generatedTokenForDev = token,
                        successMessage = "Reset code generated: $token. Enter it below with your new password.",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _forgotPasswordState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Could not find account with this email."
                    )
                }
            }
        }
    }

    fun completePasswordReset(onSuccess: () -> Unit) {
        val state = _forgotPasswordState.value
        _forgotPasswordState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.resetPassword(
                email = state.email,
                token = state.resetToken,
                newPassword = state.newPassword,
                confirmPassword = state.confirmPassword
            )
            result.onSuccess {
                _forgotPasswordState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Password reset successfully! You can now sign in.",
                        errorMessage = null
                    )
                }
                onSuccess()
            }.onFailure { error ->
                _forgotPasswordState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Password reset failed."
                    )
                }
            }
        }
    }

    fun resetForgotPasswordDialog() {
        _forgotPasswordState.value = ForgotPasswordUiState()
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository) as T
        }
    }
}
