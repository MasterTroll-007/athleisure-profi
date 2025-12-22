package com.fitness.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.dto.ClientDTO
import com.fitness.app.data.dto.ReservationDTO
import com.fitness.app.data.repository.AdminRepository
import com.fitness.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminClientDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val client: ClientDTO? = null,
    val reservations: List<ReservationDTO> = emptyList(),
    val isAdjusting: Boolean = false
)

@HiltViewModel
class AdminClientDetailViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminClientDetailUiState())
    val uiState: StateFlow<AdminClientDetailUiState> = _uiState.asStateFlow()

    fun loadClient(clientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load client
            when (val clientResult = adminRepository.getClient(clientId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(client = clientResult.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = clientResult.message) }
                    return@launch
                }
                is Result.Loading -> {}
            }

            // Load reservations
            when (val reservationsResult = adminRepository.getClientReservations(clientId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            reservations = reservationsResult.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = reservationsResult.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun adjustCredits(clientId: String, amount: Int, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAdjusting = true) }

            when (val result = adminRepository.adjustCredits(clientId, amount, reason)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isAdjusting = false) }
                    // Reload client to get updated balance
                    loadClient(clientId)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isAdjusting = false,
                            error = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }
}
