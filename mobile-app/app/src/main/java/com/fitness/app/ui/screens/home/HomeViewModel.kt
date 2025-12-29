package com.fitness.app.ui.screens.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.R
import com.fitness.app.data.dto.CreditTransactionDTO
import com.fitness.app.data.dto.ReservationDTO
import com.fitness.app.data.repository.AuthRepository
import com.fitness.app.data.repository.CreditRepository
import com.fitness.app.data.repository.ReservationRepository
import com.fitness.app.data.repository.Result
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
    val recentTransactions: List<CreditTransactionDTO> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val creditRepository: CreditRepository,
    private val reservationRepository: ReservationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorResId = null) }

            // Load user profile first (required for auth check)
            when (val profileResult = authRepository.getProfile()) {
                is Result.Success -> {
                    val user = profileResult.data
                    _uiState.update {
                        it.copy(
                            userName = user.firstName ?: user.email.substringBefore("@"),
                            isAdmin = user.role == "admin"
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

            // Load remaining data in parallel for faster loading
            val balanceDeferred = async { creditRepository.getCreditBalance() }
            val reservationsDeferred = async { reservationRepository.getUpcomingReservations() }
            val historyDeferred = async { creditRepository.getCreditHistory() }

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

            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
