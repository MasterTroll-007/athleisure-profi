package com.fitness.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.dto.AdminCreditPackageDTO
import com.fitness.app.data.dto.CreateCreditPackageRequest
import com.fitness.app.data.dto.UpdateCreditPackageRequest
import com.fitness.app.data.repository.AdminRepository
import com.fitness.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminPricingUiState(
    val packages: List<AdminCreditPackageDTO> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val editingPackage: AdminCreditPackageDTO? = null,
    val isSubmitting: Boolean = false,
    val successMessage: String? = null
)

@HiltViewModel
class AdminPricingViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPricingUiState())
    val uiState: StateFlow<AdminPricingUiState> = _uiState.asStateFlow()

    fun loadPricing() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = adminRepository.getAdminPackages()) {
                is Result.Success -> {
                    _uiState.update { it.copy(packages = result.data, isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                }
                else -> {}
            }
        }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true, editingPackage = null) }
    }

    fun showEditDialog(pkg: AdminCreditPackageDTO) {
        _uiState.update { it.copy(showCreateDialog = true, editingPackage = pkg) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showCreateDialog = false, editingPackage = null, error = null) }
    }

    fun createPackage(
        nameCs: String,
        nameEn: String?,
        description: String?,
        credits: Int,
        priceCzk: Double,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            // Get next sort order
            val nextSortOrder = (_uiState.value.packages.maxOfOrNull { it.sortOrder } ?: 0) + 1
            val request = CreateCreditPackageRequest(
                nameCs = nameCs,
                nameEn = nameEn,
                description = description,
                credits = credits,
                bonusCredits = 0,
                priceCzk = priceCzk,
                isActive = isActive,
                sortOrder = nextSortOrder
            )
            when (val result = adminRepository.createPackage(request)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            showCreateDialog = false,
                            successMessage = "Balíček vytvořen"
                        )
                    }
                    loadPricing()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, isSubmitting = false) }
                }
                else -> {}
            }
        }
    }

    fun updatePackage(
        id: String,
        nameCs: String,
        nameEn: String?,
        description: String?,
        credits: Int,
        priceCzk: Double,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            // Keep existing sortOrder
            val existingPkg = _uiState.value.packages.find { it.id == id }
            val request = UpdateCreditPackageRequest(
                nameCs = nameCs,
                nameEn = nameEn,
                description = description,
                credits = credits,
                bonusCredits = 0,
                priceCzk = priceCzk,
                isActive = isActive,
                sortOrder = existingPkg?.sortOrder ?: 0
            )
            when (val result = adminRepository.updatePackage(id, request)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            showCreateDialog = false,
                            editingPackage = null,
                            successMessage = "Balíček upraven"
                        )
                    }
                    loadPricing()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, isSubmitting = false) }
                }
                else -> {}
            }
        }
    }

    fun deletePackage(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = adminRepository.deletePackage(id)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Balíček smazán") }
                    loadPricing()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                }
                else -> {}
            }
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
