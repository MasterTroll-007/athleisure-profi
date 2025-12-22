package com.fitness.app.ui.screens.credits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.dto.CreditPackageDTO
import com.fitness.app.data.dto.CreditTransactionDTO
import com.fitness.app.data.repository.CreditRepository
import com.fitness.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreditsUiState(
    val balance: Int = 0,
    val packages: List<CreditPackageDTO> = emptyList(),
    val transactions: List<CreditTransactionDTO> = emptyList(),
    val isBalanceLoading: Boolean = false,
    val isPackagesLoading: Boolean = false,
    val isHistoryLoading: Boolean = false,
    val isPurchasing: Boolean = false,
    val packagesError: String? = null,
    val historyError: String? = null,
    val paymentUrl: String? = null
)

@HiltViewModel
class CreditsViewModel @Inject constructor(
    private val creditRepository: CreditRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditsUiState())
    val uiState: StateFlow<CreditsUiState> = _uiState.asStateFlow()

    fun loadData() {
        loadBalance()
        loadPackages()
        loadHistory()
    }

    private fun loadBalance() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBalanceLoading = true) }

            when (val result = creditRepository.getCreditBalance()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isBalanceLoading = false,
                            balance = result.data.balance
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isBalanceLoading = false) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun loadPackages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPackagesLoading = true, packagesError = null) }

            when (val result = creditRepository.getCreditPackages()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isPackagesLoading = false,
                            packages = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isPackagesLoading = false,
                            packagesError = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isHistoryLoading = true, historyError = null) }

            when (val result = creditRepository.getCreditHistory()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isHistoryLoading = false,
                            transactions = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isHistoryLoading = false,
                            historyError = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun purchasePackage(pkg: CreditPackageDTO) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true) }

            when (val result = creditRepository.purchaseCredits(pkg.id)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isPurchasing = false,
                            paymentUrl = result.data.paymentUrl
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isPurchasing = false,
                            packagesError = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearPaymentUrl() {
        _uiState.update { it.copy(paymentUrl = null) }
    }
}
