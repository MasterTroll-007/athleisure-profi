package com.fitness.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.dto.AdminCreateReservationRequest
import com.fitness.app.data.dto.ClientDTO
import com.fitness.app.data.dto.ReservationDTO
import com.fitness.app.data.dto.SlotDTO
import com.fitness.app.data.repository.AdminRepository
import com.fitness.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class AdminReservationsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val reservations: List<ReservationDTO> = emptyList(),
    val slots: List<SlotDTO> = emptyList(),
    val clients: List<ClientDTO> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val isLoadingClients: Boolean = false,
    val isCreating: Boolean = false,
    val isCancelling: Boolean = false,
    val snackbarMessage: String? = null,
    val filterStatus: String? = null
)

@HiltViewModel
class AdminReservationsViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminReservationsUiState())
    val uiState: StateFlow<AdminReservationsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        loadReservations()
    }

    fun loadReservations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val selectedDate = _uiState.value.selectedDate
            val startDate = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
            val endDate = startDate.plusDays(6)

            val startStr = startDate.format(dateFormatter)
            val endStr = endDate.format(dateFormatter)

            when (val result = adminRepository.getAdminReservations(startStr, endStr)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            reservations = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun loadSlotsForDate(date: LocalDate) {
        viewModelScope.launch {
            val dateStr = date.format(dateFormatter)

            when (val result = adminRepository.getSlots(dateStr, dateStr)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(slots = result.data.filter { slot -> slot.status == "unlocked" })
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(snackbarMessage = result.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun setSelectedDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        loadReservations()
    }

    fun searchClients(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce

            _uiState.update { it.copy(isLoadingClients = true) }

            when (val result = adminRepository.searchClients(query)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingClients = false,
                            clients = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingClients = false,
                            snackbarMessage = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearClients() {
        _uiState.update { it.copy(clients = emptyList()) }
    }

    fun createReservation(
        userId: String,
        date: String,
        startTime: String,
        endTime: String,
        blockId: String,
        deductCredits: Boolean,
        note: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }

            val request = AdminCreateReservationRequest(
                userId = userId,
                date = date,
                startTime = startTime,
                endTime = endTime,
                blockId = blockId,
                deductCredits = deductCredits,
                note = note
            )

            when (val result = adminRepository.createAdminReservation(request)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            snackbarMessage = "Reservation created successfully"
                        )
                    }
                    loadReservations()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            snackbarMessage = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun cancelReservation(reservationId: String, refundCredits: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true) }

            when (val result = adminRepository.cancelAdminReservation(reservationId, refundCredits)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            snackbarMessage = result.data
                        )
                    }
                    loadReservations()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            snackbarMessage = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun updateNote(reservationId: String, note: String?) {
        viewModelScope.launch {
            when (val result = adminRepository.updateReservationNote(reservationId, note)) {
                is Result.Success -> {
                    _uiState.update { it.copy(snackbarMessage = "Note updated") }
                    loadReservations()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(snackbarMessage = result.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun setFilterStatus(status: String?) {
        _uiState.update { it.copy(filterStatus = status) }
    }

    fun getFilteredReservations(): List<ReservationDTO> {
        val state = _uiState.value
        return if (state.filterStatus == null) {
            state.reservations
        } else {
            state.reservations.filter { it.status == state.filterStatus }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
