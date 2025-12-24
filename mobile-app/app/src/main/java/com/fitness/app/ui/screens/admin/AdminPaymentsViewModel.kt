package com.fitness.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.dto.GopayPaymentDTO
import com.fitness.app.data.repository.AdminRepository
import com.fitness.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminPaymentsUiState(
    val payments: List<GopayPaymentDTO> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFilter: PaymentFilter = PaymentFilter.ALL
)

enum class PaymentFilter {
    ALL, PAID, PENDING, CANCELLED
}

@HiltViewModel
class AdminPaymentsViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPaymentsUiState())
    val uiState: StateFlow<AdminPaymentsUiState> = _uiState.asStateFlow()

    private var allPayments: List<GopayPaymentDTO> = emptyList()

    fun loadPayments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = adminRepository.getPayments()) {
                is Result.Success -> {
                    allPayments = result.data
                    applyFilter(_uiState.value.selectedFilter)
                    _uiState.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                }
                else -> {}
            }
        }
    }

    fun setFilter(filter: PaymentFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        applyFilter(filter)
    }

    private fun applyFilter(filter: PaymentFilter) {
        val filtered = when (filter) {
            PaymentFilter.ALL -> allPayments
            PaymentFilter.PAID -> allPayments.filter { it.state.equals("PAID", ignoreCase = true) }
            PaymentFilter.PENDING -> allPayments.filter { it.state.equals("CREATED", ignoreCase = true) || it.state.equals("PENDING", ignoreCase = true) }
            PaymentFilter.CANCELLED -> allPayments.filter { it.state.equals("CANCELED", ignoreCase = true) || it.state.equals("TIMEOUTED", ignoreCase = true) }
        }
        _uiState.update { it.copy(payments = filtered) }
    }
}
