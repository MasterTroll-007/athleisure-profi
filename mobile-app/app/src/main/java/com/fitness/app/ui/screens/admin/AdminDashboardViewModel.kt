package com.fitness.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class AdminDashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val totalClients: Int = 0,
    val weeklyReservations: Int = 0,
    val weeklyRevenue: Int = 0,
    val todayReservations: List<ReservationDTO> = emptyList()
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load stats
            when (val statsResult = adminRepository.getDashboardStats()) {
                is Result.Success -> {
                    val stats = statsResult.data
                    _uiState.update {
                        it.copy(
                            totalClients = stats.totalClients,
                            weeklyReservations = stats.weeklyReservations,
                            weeklyRevenue = stats.weeklyRevenue
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = statsResult.message) }
                    return@launch
                }
                is Result.Loading -> {}
            }

            // Load today's reservations
            when (val todayResult = adminRepository.getTodayReservations()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            todayReservations = todayResult.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = todayResult.message) }
                }
                is Result.Loading -> {}
            }
        }
    }
}
