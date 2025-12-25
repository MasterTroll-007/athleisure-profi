package com.fitness.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.dto.ClientDTO
import com.fitness.app.data.dto.CreateSlotRequest
import com.fitness.app.data.dto.SlotDTO
import com.fitness.app.data.dto.UpdateSlotRequest
import com.fitness.app.data.repository.AdminRepository
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

data class AdminCalendarUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedWeekStart: LocalDate = LocalDate.now(),
    val slots: List<SlotDTO> = emptyList(),
    val isUnlocking: Boolean = false,
    val isCreating: Boolean = false,
    val isProcessing: Boolean = false,
    val snackbarMessage: String? = null,
    val clients: List<ClientDTO> = emptyList(),
    val isLoadingClients: Boolean = false
)

@HiltViewModel
class AdminCalendarViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminCalendarUiState())
    val uiState: StateFlow<AdminCalendarUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun loadSlots() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val start = _uiState.value.selectedWeekStart.format(dateFormatter)
            val end = _uiState.value.selectedWeekStart.plusDays(7).format(dateFormatter)

            when (val result = adminRepository.getSlots(start, end)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            slots = result.data
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

    fun previousWeek() {
        _uiState.update {
            it.copy(selectedWeekStart = it.selectedWeekStart.minusWeeks(1))
        }
    }

    fun nextWeek() {
        _uiState.update {
            it.copy(selectedWeekStart = it.selectedWeekStart.plusWeeks(1))
        }
    }

    fun previousDays(days: Int) {
        _uiState.update {
            it.copy(selectedWeekStart = it.selectedWeekStart.minusDays(days.toLong()))
        }
    }

    fun nextDays(days: Int) {
        _uiState.update {
            it.copy(selectedWeekStart = it.selectedWeekStart.plusDays(days.toLong()))
        }
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
                    loadSlots()
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
                    loadSlots()
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
                            loadSlots()
                        }
                        is Result.Error -> {
                            _uiState.update {
                                it.copy(
                                    isCreating = false,
                                    snackbarMessage = "Slot created but failed to assign user: ${assignResult.message}"
                                )
                            }
                            loadSlots()
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
                    loadSlots()
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
                    loadSlots()
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

            // Use empty string or special value to clear assignment
            val request = UpdateSlotRequest(assignedUserId = "")

            when (val result = adminRepository.updateSlot(slotId, request)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            snackbarMessage = "User unassigned"
                        )
                    }
                    loadSlots()
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
                    loadSlots()
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
                    loadSlots()
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
                    loadSlots()
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

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
