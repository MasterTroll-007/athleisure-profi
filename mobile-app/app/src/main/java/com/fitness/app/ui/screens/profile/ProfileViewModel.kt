package com.fitness.app.ui.screens.profile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.R
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
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val logoutSuccess: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorResId = null) }

            when (val result = authRepository.getProfile()) {
                is Result.Success -> {
                    val user = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            email = user.email,
                            firstName = user.firstName,
                            lastName = user.lastName,
                            phone = user.phone
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

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(logoutSuccess = true) }
        }
    }
}
