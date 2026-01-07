package com.fitness.app.ui.screens.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.R
import com.fitness.app.data.dto.CreditTransactionDTO
import com.fitness.app.data.dto.ReservationDTO
import com.fitness.app.data.dto.SlotDTO
import com.fitness.app.data.repository.AdminRepository
import com.fitness.app.data.repository.AuthRepository
import com.fitness.app.data.repository.CreditRepository
import com.fitness.app.data.repository.ReservationRepository
import com.fitness.app.data.repository.Result
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    @StringRes val errorResId: Int? = null,
    val userName: String = "",
    val isAdmin: Boolean = false,
    val creditBalance: Int = 0,
    val nextReservation: ReservationDTO? = null,
    val recentTransactions: List<CreditTransactionDTO> = emptyList(),
    // Admin-specific state
    val todaySlots: List<SlotDTO> = emptyList(),
    val tomorrowSlots: List<SlotDTO> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val creditRepository: CreditRepository,
    private val reservationRepository: ReservationRepository,
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorResId = null) }

            var isAdminUser = false

            // Load user profile first (required for auth check)
            when (val profileResult = authRepository.getProfile()) {
                is Result.Success -> {
                    val user = profileResult.data
                    isAdminUser = user.role == "admin"
                    _uiState.update {
                        it.copy(
                            userName = user.firstName ?: user.email.substringBefore("@"),
                            isAdmin = isAdminUser
                        )
                    }
                }
                is Result.Error -> {
                    // Profile load failed - likely invalid token, force logout
                    authRepository.logout()
                    return@launch
                }
                is Result.Loading -> {}
            }

            if (isAdminUser) {
                // Admin: Load slots for today and tomorrow
                loadAdminData()
            } else {
                // Client: Load credits and reservations
                loadClientData()
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadClientData() {
        // Load remaining data in parallel for faster loading
        val balanceDeferred = viewModelScope.async { creditRepository.getCreditBalance() }
        val reservationsDeferred = viewModelScope.async { reservationRepository.getUpcomingReservations() }
        val historyDeferred = viewModelScope.async { creditRepository.getCreditHistory() }

        // Await all results
        when (val balanceResult = balanceDeferred.await()) {
            is Result.Success -> {
                _uiState.update { it.copy(creditBalance = balanceResult.data.balance) }
            }
            is Result.Error -> { /* Non-critical */ }
            is Result.Loading -> {}
        }

        when (val reservationsResult = reservationsDeferred.await()) {
            is Result.Success -> {
                _uiState.update { it.copy(nextReservation = reservationsResult.data.firstOrNull()) }
            }
            is Result.Error -> { /* Non-critical */ }
            is Result.Loading -> {}
        }

        when (val historyResult = historyDeferred.await()) {
            is Result.Success -> {
                _uiState.update { it.copy(recentTransactions = historyResult.data) }
            }
            is Result.Error -> { /* Non-critical */ }
            is Result.Loading -> {}
        }
    }

    private suspend fun loadAdminData() {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        when (val result = adminRepository.getSlots(
            today.format(formatter),
            tomorrow.format(formatter)
        )) {
            is Result.Success -> {
                val allSlots = result.data
                val todayStr = today.format(formatter)
                val tomorrowStr = tomorrow.format(formatter)

                _uiState.update {
                    it.copy(
                        todaySlots = allSlots.filter { slot -> slot.date == todayStr }
                            .sortedBy { slot -> slot.startTime },
                        tomorrowSlots = allSlots.filter { slot -> slot.date == tomorrowStr }
                            .sortedBy { slot -> slot.startTime }
                    )
                }
            }
            is Result.Error -> { /* Non-critical */ }
            is Result.Loading -> {}
        }
    }
}
