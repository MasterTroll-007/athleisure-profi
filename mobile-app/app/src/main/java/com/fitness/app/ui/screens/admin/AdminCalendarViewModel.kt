package com.fitness.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.dto.CreateSlotRequest
import com.fitness.app.data.dto.SlotDTO
import com.fitness.app.data.repository.AdminRepository
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

data class AdminCalendarUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedWeekStart: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
    val slots: List<SlotDTO> = emptyList(),
    val isUnlocking: Boolean = false,
    val isCreating: Boolean = false
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

    fun unlockWeek() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUnlocking = true) }

            val weekStart = _uiState.value.selectedWeekStart.format(dateFormatter)

            when (val result = adminRepository.unlockWeek(weekStart)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isUnlocking = false) }
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
            val request = CreateSlotRequest(
                date = dateStr,
                startTime = startTime,
                endTime = endTime,
                status = "UNLOCKED"
            )

            when (val result = adminRepository.createSlot(request)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isCreating = false) }
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

    fun deleteSlot(slotId: String) {
        viewModelScope.launch {
            when (val result = adminRepository.deleteSlot(slotId)) {
                is Result.Success -> {
                    loadSlots()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is Result.Loading -> {}
            }
        }
    }
}
