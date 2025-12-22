package com.fitness.app.ui.screens.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.dto.AvailableSlotDTO
import com.fitness.app.data.dto.ReservationDTO
import com.fitness.app.data.repository.ReservationRepository
import com.fitness.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ReservationsUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val availableSlots: List<AvailableSlotDTO> = emptyList(),
    val myReservations: List<ReservationDTO> = emptyList(),
    val isSlotsLoading: Boolean = false,
    val isReservationsLoading: Boolean = false,
    val slotsError: String? = null,
    val reservationsError: String? = null,
    val showBookingDialog: Boolean = false,
    val showCancelDialog: Boolean = false,
    val selectedSlot: AvailableSlotDTO? = null,
    val selectedReservation: ReservationDTO? = null,
    val isBooking: Boolean = false,
    val isCancelling: Boolean = false
)

@HiltViewModel
class ReservationsViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReservationsUiState())
    val uiState: StateFlow<ReservationsUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun loadData() {
        loadAvailableSlots()
        loadMyReservations()
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        loadAvailableSlots()
    }

    private fun loadAvailableSlots() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSlotsLoading = true, slotsError = null) }

            val date = _uiState.value.selectedDate
            val start = date.format(dateFormatter)
            val end = date.plusDays(1).format(dateFormatter)

            when (val result = reservationRepository.getAvailableSlots(start, end)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSlotsLoading = false,
                            availableSlots = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSlotsLoading = false,
                            slotsError = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun loadMyReservations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isReservationsLoading = true, reservationsError = null) }

            when (val result = reservationRepository.getUpcomingReservations()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isReservationsLoading = false,
                            myReservations = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isReservationsLoading = false,
                            reservationsError = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun bookSlot(slot: AvailableSlotDTO) {
        _uiState.update {
            it.copy(
                showBookingDialog = true,
                selectedSlot = slot
            )
        }
    }

    fun dismissBookingDialog() {
        _uiState.update {
            it.copy(
                showBookingDialog = false,
                selectedSlot = null
            )
        }
    }

    fun confirmBooking() {
        val slot = _uiState.value.selectedSlot ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true) }

            when (val result = reservationRepository.createReservation(slot.id, null)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isBooking = false,
                            showBookingDialog = false,
                            selectedSlot = null
                        )
                    }
                    loadData()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isBooking = false,
                            slotsError = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun cancelReservation(reservation: ReservationDTO) {
        _uiState.update {
            it.copy(
                showCancelDialog = true,
                selectedReservation = reservation
            )
        }
    }

    fun dismissCancelDialog() {
        _uiState.update {
            it.copy(
                showCancelDialog = false,
                selectedReservation = null
            )
        }
    }

    fun confirmCancel() {
        val reservation = _uiState.value.selectedReservation ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true) }

            when (val result = reservationRepository.cancelReservation(reservation.id)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            showCancelDialog = false,
                            selectedReservation = null
                        )
                    }
                    loadMyReservations()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            reservationsError = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }
}
