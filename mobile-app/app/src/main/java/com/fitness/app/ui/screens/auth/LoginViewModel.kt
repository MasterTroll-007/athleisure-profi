package com.fitness.app.ui.screens.auth

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.local.BiometricAuthManager
import com.fitness.app.data.repository.AuthRepository
import com.fitness.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val rememberMe: Boolean = false,
    val showBiometricButton: Boolean = false,
    val savedEmail: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        checkBiometricAvailability()
    }

    private fun checkBiometricAvailability() {
        viewModelScope.launch {
            val biometricAvailable = biometricAuthManager.isBiometricAvailable()
            val biometricEnabled = biometricAuthManager.isBiometricLoginEnabled()
            val credentials = biometricAuthManager.getBiometricCredentials()

            _uiState.update {
                it.copy(
                    showBiometricButton = biometricAvailable && biometricEnabled && credentials != null,
                    savedEmail = credentials?.first
                )
            }
        }
    }

    fun setRememberMe(remember: Boolean) {
        _uiState.update { it.copy(rememberMe = remember) }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all fields") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = authRepository.login(email.trim(), password, _uiState.value.rememberMe)) {
                is Result.Success -> {
                    // Save credentials for biometric login if remember me is enabled
                    if (_uiState.value.rememberMe && biometricAuthManager.isBiometricAvailable()) {
                        biometricAuthManager.saveBiometricCredentials(email.trim(), password)
                        biometricAuthManager.setBiometricLoginEnabled(true)
                    }
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Result.Loading -> {
                    // Already handled
                }
            }
        }
    }

    fun loginWithBiometric(activity: FragmentActivity) {
        viewModelScope.launch {
            val credentials = biometricAuthManager.getBiometricCredentials()
            if (credentials == null) {
                _uiState.update { it.copy(error = "No saved credentials found") }
                return@launch
            }

            biometricAuthManager.showBiometricPrompt(
                activity = activity,
                onSuccess = {
                    // Biometric authentication succeeded, proceed with login
                    viewModelScope.launch {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                        when (val result = authRepository.login(credentials.first, credentials.second, rememberMe = true)) {
                            is Result.Success -> {
                                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                            }
                            is Result.Error -> {
                                _uiState.update { it.copy(isLoading = false, error = result.message) }
                            }
                            is Result.Loading -> {}
                        }
                    }
                },
                onError = { errorMessage ->
                    _uiState.update { it.copy(error = errorMessage) }
                },
                onFailed = {
                    _uiState.update { it.copy(error = "Biometric authentication failed") }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
