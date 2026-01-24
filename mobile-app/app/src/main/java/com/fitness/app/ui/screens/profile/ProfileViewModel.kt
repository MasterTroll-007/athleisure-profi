package com.fitness.app.ui.screens.profile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.R
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

data class ProfileUiState(
    val isLoading: Boolean = true,
    @StringRes val errorResId: Int? = null,
    val email: String = "",
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val role: String = "client",
    val emailRemindersEnabled: Boolean = true,
    val reminderHoursBefore: Int = 24,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val logoutSuccess: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val isBiometricEnabled: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorResId = null) }

            // Load biometric state
            val biometricAvailable = biometricAuthManager.isBiometricAvailable()
            var biometricEnabled = biometricAuthManager.isBiometricLoginEnabled()

            // If biometrics were enabled but are no longer available (user removed fingerprints),
            // automatically disable biometric login
            if (biometricEnabled && !biometricAvailable) {
                biometricAuthManager.clearBiometricCredentials()
                biometricEnabled = false
            }

            // Also check if biometric is enabled but no credentials are saved
            if (biometricEnabled) {
                val hasCredentials = biometricAuthManager.getBiometricCredentials() != null
                if (!hasCredentials) {
                    biometricAuthManager.setBiometricLoginEnabled(false)
                    biometricEnabled = false
                }
            }

            when (val result = authRepository.getProfile()) {
                is Result.Success -> {
                    val user = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            email = user.email,
                            firstName = user.firstName,
                            lastName = user.lastName,
                            phone = user.phone,
                            role = user.role,
                            isBiometricAvailable = biometricAvailable,
                            isBiometricEnabled = biometricEnabled,
                            emailRemindersEnabled = user.emailRemindersEnabled,
                            reminderHoursBefore = user.reminderHoursBefore
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorResId = R.string.error_load_profile
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                // When enabling, we need credentials to be saved first
                // This should only be possible if user has logged in with "Remember Me"
                // and saved credentials during the biometric setup dialog
                val hasCredentials = biometricAuthManager.getBiometricCredentials() != null
                if (hasCredentials) {
                    biometricAuthManager.setBiometricLoginEnabled(true)
                    _uiState.update { it.copy(isBiometricEnabled = true) }
                }
                // If no credentials, the toggle won't change - user needs to log out and log back in with "Remember Me"
            } else {
                // Disabling clears the credentials for security
                biometricAuthManager.clearBiometricCredentials()
                _uiState.update { it.copy(isBiometricEnabled = false) }
            }
        }
    }

    fun updateProfile(firstName: String?, lastName: String?, phone: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            when (val result = authRepository.updateProfile(
                firstName = firstName,
                lastName = lastName,
                phone = phone,
                locale = null,
                theme = null
            )) {
                is Result.Success -> {
                    val user = result.data
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true,
                            firstName = user.firstName,
                            lastName = user.lastName,
                            phone = user.phone
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorResId = R.string.error_server
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            when (val result = authRepository.changePassword(currentPassword, newPassword)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorResId = R.string.error_server
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun updateReminderSettings(emailRemindersEnabled: Boolean, reminderHoursBefore: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            when (val result = authRepository.updateProfile(
                firstName = null,
                lastName = null,
                phone = null,
                locale = null,
                theme = null,
                emailRemindersEnabled = emailRemindersEnabled,
                reminderHoursBefore = reminderHoursBefore
            )) {
                is Result.Success -> {
                    val user = result.data
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true,
                            emailRemindersEnabled = user.emailRemindersEnabled,
                            reminderHoursBefore = user.reminderHoursBefore
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorResId = R.string.error_server
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Clear biometric credentials on logout for security
            biometricAuthManager.clearBiometricCredentials()
            authRepository.logout()
            _uiState.update { it.copy(logoutSuccess = true) }
        }
    }
}
