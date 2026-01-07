package com.fitness.app.ui.screens.reservations

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.R
import com.fitness.app.data.dto.AvailableSlotDTO
import com.fitness.app.data.dto.ClientDTO
import com.fitness.app.data.dto.CreateSlotRequest
import com.fitness.app.data.dto.ReservationDTO
import com.fitness.app.data.dto.SlotDTO
import com.fitness.app.data.dto.UpdateSlotRequest
import com.fitness.app.data.repository.AdminRepository
import com.fitness.app.data.repository.AuthRepository
import com.fitness.app.data.repository.ReservationRepository
import com.fitness.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class ReservationsUiState(
    val selectedWeekStart: LocalDate = LocalDate.now(),
    // User mode data
    val availableSlots: List<AvailableSlotDTO> = emptyList(),
    val myReservations: List<ReservationDTO> = emptyList(),
    // Admin mode data
    val adminSlots: List<SlotDTO> = emptyList(),
    val clients: List<ClientDTO> = emptyList(),
    val isLoadingClients: Boolean = false,
    // Infinite scroll data
    val loadedDays: List<LocalDate> = emptyList(),
    val userSlotsByDate: Map<LocalDate, List<AvailableSlotDTO>> = emptyMap(),
    val adminSlotsByDate: Map<LocalDate, List<SlotDTO>> = emptyMap(),
    val reservationsByDate: Map<LocalDate, List<ReservationDTO>> = emptyMap(),
    val minLoadedDate: LocalDate? = null,
    val maxLoadedDate: LocalDate? = null,
    val isLoadingPast: Boolean = false,
    val isLoadingFuture: Boolean = false,
    // Common state
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAdmin: Boolean = false,
    // User dialogs
    val showBookingDialog: Boolean = false,
    val showCancelDialog: Boolean = false,
    val selectedSlot: AvailableSlotDTO? = null,
    val selectedReservation: ReservationDTO? = null,
    val isBooking: Boolean = false,
    val isCancelling: Boolean = false,
    // Admin state
    val isUnlocking: Boolean = false,
    val isCreating: Boolean = false,
    val isProcessing: Boolean = false,
    val selectedAdminSlot: SlotDTO? = null,
    // Snackbar
    @StringRes val snackbarMessageResId: Int? = null,
    val snackbarMessage: String? = null,
    val isSnackbarError: Boolean = false
)

@HiltViewModel
class ReservationsViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository,
    private val adminRepository: AdminRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReservationsUiState())
    val uiState: StateFlow<ReservationsUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        // Observe user role changes
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(isAdmin = user?.role == "admin") }
            }
        }
    }

    companion object {
        private const val DAYS_BACK_LIMIT = 7
        private const val DAYS_FORWARD_LIMIT = 21
    }

    fun loadData() {
        if (_uiState.value.isAdmin) {
            loadAdminSlots()
        } else {
            loadSlots()
            loadMyReservations()
        }
    }

    // ============== INFINITE SCROLL FUNCTIONS ==============

    fun loadInitialRange() {
        val today = LocalDate.now()
        val startDate = today.minusDays(DAYS_BACK_LIMIT.toLong())
        val endDate = today.plusDays(DAYS_FORWARD_LIMIT.toLong())

        // Generate list of all days in range
        val days = generateSequence(startDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(endDate) }
            .toList()

        _uiState.update {
            it.copy(
                loadedDays = days,
                minLoadedDate = startDate,
                maxLoadedDate = endDate
            )
        }

        loadSlotsForRange(startDate, endDate)
    }

    fun loadMorePast() {
        val state = _uiState.value
        val minDate = state.minLoadedDate ?: return
        val today = LocalDate.now()
        val absoluteMin = today.minusDays(DAYS_BACK_LIMIT.toLong())

        // Don't load beyond the limit
        if (!minDate.isAfter(absoluteMin) || state.isLoadingPast) return

        val newMinDate = minDate.minusDays(7).let {
            if (it.isBefore(absoluteMin)) absoluteMin else it
        }

        if (newMinDate >= minDate) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPast = true) }

            val newDays = generateSequence(newMinDate) { it.plusDays(1) }
                .takeWhile { it.isBefore(minDate) }
                .toList()

            loadSlotsForRangeAndMerge(newMinDate, minDate.minusDays(1), prepend = true, newDays = newDays)
        }
    }

    fun loadMoreFuture() {
        val state = _uiState.value
        val maxDate = state.maxLoadedDate ?: return
        val today = LocalDate.now()
        val absoluteMax = today.plusDays(DAYS_FORWARD_LIMIT.toLong())

        // Don't load beyond the limit
        if (!maxDate.isBefore(absoluteMax) || state.isLoadingFuture) return

        val newMaxDate = maxDate.plusDays(7).let {
            if (it.isAfter(absoluteMax)) absoluteMax else it
        }

        if (newMaxDate <= maxDate) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFuture = true) }

            val newDays = generateSequence(maxDate.plusDays(1)) { it.plusDays(1) }
                .takeWhile { !it.isAfter(newMaxDate) }
                .toList()

            loadSlotsForRangeAndMerge(maxDate.plusDays(1), newMaxDate, prepend = false, newDays = newDays)
        }
    }

    private fun loadSlotsForRange(startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val start = startDate.format(dateFormatter)
            val end = endDate.format(dateFormatter)

            if (_uiState.value.isAdmin) {
                when (val result = adminRepository.getSlots(start, end)) {
                    is Result.Success -> {
                        val slotsByDate = result.data.groupBy { LocalDate.parse(it.date) }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                adminSlotsByDate = slotsByDate,
                                adminSlots = result.data
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                    is Result.Loading -> {}
                }
            } else {
                // Load both available slots and reservations
                val slotsResult = reservationRepository.getAvailableSlots(start, end)
                val reservationsResult = reservationRepository.getUpcomingReservations()

                when (slotsResult) {
                    is Result.Success -> {
                        val slotsByDate = slotsResult.data.groupBy { LocalDate.parse(it.date) }
                        val reservationsByDate = when (reservationsResult) {
                            is Result.Success -> reservationsResult.data.groupBy { LocalDate.parse(it.date) }
                            else -> emptyMap()
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                userSlotsByDate = slotsByDate,
                                reservationsByDate = reservationsByDate,
                                availableSlots = slotsResult.data,
                                myReservations = (reservationsResult as? Result.Success)?.data ?: emptyList()
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = slotsResult.message) }
                    }
                    is Result.Loading -> {}
                }
            }
        }
    }

    private suspend fun loadSlotsForRangeAndMerge(
        startDate: LocalDate,
        endDate: LocalDate,
        prepend: Boolean,
        newDays: List<LocalDate>
    ) {
        val start = startDate.format(dateFormatter)
        val end = endDate.format(dateFormatter)

        if (_uiState.value.isAdmin) {
            when (val result = adminRepository.getSlots(start, end)) {
                is Result.Success -> {
                    val newSlotsByDate = result.data.groupBy { LocalDate.parse(it.date) }
                    _uiState.update { state ->
                        val updatedDays = if (prepend) newDays + state.loadedDays else state.loadedDays + newDays
                        val updatedSlots = state.adminSlotsByDate + newSlotsByDate
                        state.copy(
                            loadedDays = updatedDays,
                            adminSlotsByDate = updatedSlots,
                            adminSlots = updatedSlots.values.flatten(),
                            minLoadedDate = if (prepend) startDate else state.minLoadedDate,
                            maxLoadedDate = if (!prepend) endDate else state.maxLoadedDate,
                            isLoadingPast = false,
                            isLoadingFuture = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingPast = false, isLoadingFuture = false) }
                }
                is Result.Loading -> {}
            }
        } else {
            when (val result = reservationRepository.getAvailableSlots(start, end)) {
                is Result.Success -> {
                    val newSlotsByDate = result.data.groupBy { LocalDate.parse(it.date) }
                    _uiState.update { state ->
                        val updatedDays = if (prepend) newDays + state.loadedDays else state.loadedDays + newDays
                        val updatedSlots = state.userSlotsByDate + newSlotsByDate
                        state.copy(
                            loadedDays = updatedDays,
                            userSlotsByDate = updatedSlots,
                            availableSlots = updatedSlots.values.flatten(),
                            minLoadedDate = if (prepend) startDate else state.minLoadedDate,
                            maxLoadedDate = if (!prepend) endDate else state.maxLoadedDate,
                            isLoadingPast = false,
                            isLoadingFuture = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingPast = false, isLoadingFuture = false) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun previousDays(days: Int) {
        _uiState.update { it.copy(selectedWeekStart = it.selectedWeekStart.minusDays(days.toLong())) }
        loadData()
    }

    fun nextDays(days: Int) {
        _uiState.update { it.copy(selectedWeekStart = it.selectedWeekStart.plusDays(days.toLong())) }
        loadData()
    }

    fun setViewMode(days: Int) {
        val newStart = if (days == 5) {
            // For week view (Mon-Fri), start from Monday of current week
            LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        } else {
            // For 1 or 3 day view, start from today
            LocalDate.now()
        }
        _uiState.update { it.copy(selectedWeekStart = newStart) }
        loadData()
    }

    // ============== USER MODE FUNCTIONS ==============

    private fun loadSlots() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val start = _uiState.value.selectedWeekStart.format(dateFormatter)
            val end = _uiState.value.selectedWeekStart.plusDays(7).format(dateFormatter)

            when (val result = reservationRepository.getAvailableSlots(start, end)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            availableSlots = result.data
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

    private fun loadMyReservations() {
        viewModelScope.launch {
            when (val result = reservationRepository.getUpcomingReservations()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(myReservations = result.data)
                    }
                }
                is Result.Error -> {}
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

            when (val result = reservationRepository.createReservation(slot)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isBooking = false,
                            showBookingDialog = false,
                            selectedSlot = null,
                            snackbarMessageResId = R.string.booking_success,
                            isSnackbarError = false
                        )
                    }
                    loadData()
                }
                is Result.Error -> {
                    val errorResId = parseErrorToResId(result.message)
                    _uiState.update {
                        it.copy(
                            isBooking = false,
                            showBookingDialog = false,
                            selectedSlot = null,
                            snackbarMessageResId = errorResId,
                            isSnackbarError = true
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
                            selectedReservation = null,
                            snackbarMessageResId = R.string.cancel_success,
                            isSnackbarError = false
                        )
                    }
                    loadData()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            showCancelDialog = false,
                            selectedReservation = null,
                            snackbarMessageResId = R.string.error_cancel_failed,
                            isSnackbarError = true
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    // ============== ADMIN MODE FUNCTIONS ==============

    private fun loadAdminSlots() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val start = _uiState.value.selectedWeekStart.format(dateFormatter)
            val end = _uiState.value.selectedWeekStart.plusDays(7).format(dateFormatter)

            when (val result = adminRepository.getSlots(start, end)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            adminSlots = result.data
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

    fun selectAdminSlot(slot: SlotDTO) {
        _uiState.update { it.copy(selectedAdminSlot = slot) }
    }

    fun clearSelectedAdminSlot() {
        _uiState.update { it.copy(selectedAdminSlot = null) }
    }

    fun unlockWeek() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUnlocking = true) }

            val weekStart = _uiState.value.selectedWeekStart.format(dateFormatter)

            when (val result = adminRepository.unlockWeek(weekStart)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isUnlocking = false,
                            snackbarMessage = "Week unlocked successfully"
                        )
                    }
                    loadData()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isUnlocking = false,
                            error = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun createSlot(date: LocalDate, startTime: String, endTime: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }

            val dateStr = date.format(dateFormatter)
            val startParts = startTime.split(":")
            val endParts = endTime.split(":")
            val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
            val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
            val durationMinutes = endMinutes - startMinutes

            val request = CreateSlotRequest(
                date = dateStr,
                startTime = startTime,
                durationMinutes = durationMinutes
            )

            when (val result = adminRepository.createSlot(request)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            snackbarMessage = "Slot created"
                        )
                    }
                    loadData()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            error = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun createSlotWithUser(date: LocalDate, startTime: String, endTime: String, userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }

            val dateStr = date.format(dateFormatter)
            val startParts = startTime.split(":")
            val endParts = endTime.split(":")
            val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
            val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
            val durationMinutes = endMinutes - startMinutes

            val request = CreateSlotRequest(
                date = dateStr,
                startTime = startTime,
                durationMinutes = durationMinutes
            )

            when (val createResult = adminRepository.createSlot(request)) {
                is Result.Success -> {
                    // Now assign the user to the newly created slot
                    val slotId = createResult.data.id
                    val updateRequest = UpdateSlotRequest(assignedUserId = userId)

                    when (val assignResult = adminRepository.updateSlot(slotId, updateRequest)) {
                        is Result.Success -> {
                            _uiState.update {
                                it.copy(
                                    isCreating = false,
                                    snackbarMessage = "Slot created and user assigned"
                                )
                            }
                            loadData()
                        }
                        is Result.Error -> {
                            _uiState.update {
                                it.copy(
                                    isCreating = false,
                                    snackbarMessage = "Slot created but failed to assign user: ${assignResult.message}"
                                )
                            }
                            loadData()
                        }
                        is Result.Loading -> {}
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            error = createResult.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun deleteSlot(slotId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            when (val result = adminRepository.deleteSlot(slotId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            snackbarMessage = "Slot deleted"
                        )
                    }
                    loadData()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            error = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun assignUser(slotId: String, userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            val request = UpdateSlotRequest(assignedUserId = userId)

            when (val result = adminRepository.updateSlot(slotId, request)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            snackbarMessage = "User assigned"
                        )
                    }
                    loadData()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            snackbarMessage = "Failed to assign user: ${result.message}"
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun unassignUser(slotId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            val request = UpdateSlotRequest(assignedUserId = "")

            when (val result = adminRepository.updateSlot(slotId, request)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            snackbarMessage = "User unassigned"
                        )
                    }
                    loadData()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            snackbarMessage = "Failed to unassign user: ${result.message}"
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun lockSlot(slotId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            val request = UpdateSlotRequest(status = "LOCKED")

            when (val result = adminRepository.updateSlot(slotId, request)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            snackbarMessage = "Slot locked"
                        )
                    }
                    loadData()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            snackbarMessage = "Failed to lock slot: ${result.message}"
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun unlockSlot(slotId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            val request = UpdateSlotRequest(status = "UNLOCKED")

            when (val result = adminRepository.updateSlot(slotId, request)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            snackbarMessage = "Slot unlocked"
                        )
                    }
                    loadData()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            snackbarMessage = "Failed to unlock slot: ${result.message}"
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun moveSlot(slotId: String, newDate: String, newStartTime: String, newEndTime: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            val request = UpdateSlotRequest(
                date = newDate,
                startTime = newStartTime,
                endTime = newEndTime
            )

            when (val result = adminRepository.updateSlot(slotId, request)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            snackbarMessage = "Slot moved"
                        )
                    }
                    loadData()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            snackbarMessage = "Failed to move slot: ${result.message}"
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
                            snackbarMessage = "Failed to load clients"
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun searchClients(query: String) {
        viewModelScope.launch {
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
                            snackbarMessage = "Search failed"
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    // ============== COMMON FUNCTIONS ==============

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessageResId = null, snackbarMessage = null) }
    }

    private fun parseErrorToResId(errorMessage: String): Int {
        return when {
            errorMessage.contains("insufficient", ignoreCase = true) ||
            errorMessage.contains("not enough credit", ignoreCase = true) -> R.string.error_insufficient_credits
            errorMessage.contains("not available", ignoreCase = true) ||
            errorMessage.contains("already booked", ignoreCase = true) -> R.string.error_slot_not_available
            errorMessage.contains("network", ignoreCase = true) ||
            errorMessage.contains("connection", ignoreCase = true) -> R.string.error_network
            errorMessage.contains("server", ignoreCase = true) ||
            errorMessage.contains("500", ignoreCase = true) -> R.string.error_server
            else -> R.string.error_booking_failed
        }
    }
}
