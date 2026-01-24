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
    val availableSlots: List<SlotDTO> = emptyList(),
    val clients: List<ClientDTO> = emptyList(),
    val isLoadingClients: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val filterStatus: String? = null,
    val searchQuery: String = "",
    val selectedReservation: ReservationDTO? = null,
    val selectedClient: ClientDTO? = null,
    val selectedSlot: SlotDTO? = null,
    val isCreating: Boolean = false,
    val isCancelling: Boolean = false,
    val snackbarMessage: String? = null,
    val isSnackbarError: Boolean = false
)

@HiltViewModel
class AdminReservationsViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminReservationsUiState())
    val uiState: StateFlow<AdminReservationsUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private var searchJob: Job? = null

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
                    val filteredReservations = filterReservations(result.data)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            reservations = filteredReservations
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

    fun loadAvailableSlots() {
        viewModelScope.launch {
            val selectedDate = _uiState.value.selectedDate
            val start = selectedDate.format(dateFormatter)
            val end = selectedDate.plusDays(7).format(dateFormatter)

            when (val result = adminRepository.getSlots(start, end)) {
                is Result.Success -> {
                    // Filter only unlocked slots without assigned users
                    val available = result.data.filter {
                        it.status.uppercase() == "UNLOCKED" && it.assignedUserId == null
                    }
                    _uiState.update { it.copy(availableSlots = available) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            snackbarMessage = "Failed to load slots: ${result.message}",
                            isSnackbarError = true
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun loadClients() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingClients = true) }

            when (val result = adminRepository.getClients()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingClients = false,
                            clients = result.data.content
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingClients = false,
                            snackbarMessage = "Failed to load clients",
                            isSnackbarError = true
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun searchClients(query: String) {
        if (query.length < 2) {
            if (query.isEmpty()) loadClients()
            return
        }

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
                            snackbarMessage = "Search failed",
                            isSnackbarError = true
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun setSelectedDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        loadReservations()
    }

    fun setFilterStatus(status: String?) {
        _uiState.update { it.copy(filterStatus = status) }
        // Re-filter existing reservations
        viewModelScope.launch {
            val selectedDate = _uiState.value.selectedDate
            val startDate = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
            val endDate = startDate.plusDays(6)

            when (val result = adminRepository.getAdminReservations(
                startDate.format(dateFormatter),
                endDate.format(dateFormatter)
            )) {
                is Result.Success -> {
                    val filtered = filterReservations(result.data)
                    _uiState.update { it.copy(reservations = filtered) }
                }
                else -> {}
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        // Re-filter existing reservations
        viewModelScope.launch {
            val selectedDate = _uiState.value.selectedDate
            val startDate = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
            val endDate = startDate.plusDays(6)

            when (val result = adminRepository.getAdminReservations(
                startDate.format(dateFormatter),
                endDate.format(dateFormatter)
            )) {
                is Result.Success -> {
                    val filtered = filterReservations(result.data)
                    _uiState.update { it.copy(reservations = filtered) }
                }
                else -> {}
            }
        }
    }

    private fun filterReservations(reservations: List<ReservationDTO>): List<ReservationDTO> {
        val state = _uiState.value
        return reservations.filter { reservation ->
            val matchesStatus = state.filterStatus == null ||
                reservation.status.equals(state.filterStatus, ignoreCase = true)
            val matchesSearch = state.searchQuery.isEmpty() ||
                reservation.clientName?.contains(state.searchQuery, ignoreCase = true) == true ||
                reservation.clientEmail?.contains(state.searchQuery, ignoreCase = true) == true
            matchesStatus && matchesSearch
        }.sortedByDescending { "${it.date} ${it.startTime}" }
    }

    fun selectReservation(reservation: ReservationDTO) {
        _uiState.update { it.copy(selectedReservation = reservation) }
    }

    fun clearSelectedReservation() {
        _uiState.update { it.copy(selectedReservation = null) }
    }

    fun selectClient(client: ClientDTO) {
        _uiState.update { it.copy(selectedClient = client) }
    }

    fun clearSelectedClient() {
        _uiState.update { it.copy(selectedClient = null) }
    }

    fun selectSlot(slot: SlotDTO) {
        _uiState.update { it.copy(selectedSlot = slot) }
    }

    fun clearSelectedSlot() {
        _uiState.update { it.copy(selectedSlot = null) }
    }

    fun createReservation(
        clientId: String,
        slot: SlotDTO,
        deductCredits: Boolean,
        note: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }

            val request = AdminCreateReservationRequest(
                userId = clientId,
                date = slot.date,
                startTime = slot.startTime,
                endTime = slot.endTime,
                blockId = slot.id,
                deductCredits = deductCredits,
                note = note
            )

            when (val result = adminRepository.createAdminReservation(request)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            selectedClient = null,
                            selectedSlot = null,
                            snackbarMessage = "Reservation created successfully",
                            isSnackbarError = false
                        )
                    }
                    loadReservations()
                    loadAvailableSlots()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            snackbarMessage = "Failed to create reservation: ${result.message}",
                            isSnackbarError = true
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
                            selectedReservation = null,
                            snackbarMessage = "Reservation cancelled",
                            isSnackbarError = false
                        )
                    }
                    loadReservations()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            snackbarMessage = "Failed to cancel: ${result.message}",
                            isSnackbarError = true
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun updateReservationNote(reservationId: String, note: String) {
        viewModelScope.launch {
            when (val result = adminRepository.updateReservationNote(reservationId, note)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedReservation = null,
                            snackbarMessage = "Note updated",
                            isSnackbarError = false
                        )
                    }
                    loadReservations()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            snackbarMessage = "Failed to update note: ${result.message}",
                            isSnackbarError = true
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
