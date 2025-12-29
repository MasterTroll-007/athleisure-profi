package com.fitness.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.dto.AdminTrainingPlanDTO
import com.fitness.app.data.dto.CreateTrainingPlanRequest
import com.fitness.app.data.dto.UpdateTrainingPlanRequest
import com.fitness.app.data.repository.AdminRepository
import com.fitness.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class AdminPlansUiState(
    val plans: List<AdminTrainingPlanDTO> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val editingPlan: AdminTrainingPlanDTO? = null,
    val isSubmitting: Boolean = false,
    val successMessage: String? = null,
    val uploadingPlanId: String? = null
)

@HiltViewModel
class AdminPlansViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPlansUiState())
    val uiState: StateFlow<AdminPlansUiState> = _uiState.asStateFlow()

    fun loadPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = adminRepository.getAdminPlans()) {
                is Result.Success -> {
                    _uiState.update { it.copy(plans = result.data, isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                }
                else -> {}
            }
        }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true, editingPlan = null) }
    }

    fun showEditDialog(plan: AdminTrainingPlanDTO) {
        _uiState.update { it.copy(showCreateDialog = true, editingPlan = plan) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showCreateDialog = false, editingPlan = null, error = null) }
    }

    fun createPlan(
        nameCs: String,
        nameEn: String?,
        descriptionCs: String?,
        descriptionEn: String?,
        credits: Int,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            val request = CreateTrainingPlanRequest(
                nameCs = nameCs,
                nameEn = nameEn,
                descriptionCs = descriptionCs,
                descriptionEn = descriptionEn,
                credits = credits,
                isActive = isActive
            )
            when (val result = adminRepository.createPlan(request)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            showCreateDialog = false,
                            successMessage = "Plán vytvořen"
                        )
                    }
                    loadPlans()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, isSubmitting = false) }
                }
                else -> {}
            }
        }
    }

    fun updatePlan(
        id: String,
        nameCs: String,
        nameEn: String?,
        descriptionCs: String?,
        descriptionEn: String?,
        credits: Int,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            val request = UpdateTrainingPlanRequest(
                nameCs = nameCs,
                nameEn = nameEn,
                descriptionCs = descriptionCs,
                descriptionEn = descriptionEn,
                credits = credits,
                isActive = isActive
            )
            when (val result = adminRepository.updatePlan(id, request)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            showCreateDialog = false,
                            editingPlan = null,
                            successMessage = "Plán upraven"
                        )
                    }
                    loadPlans()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, isSubmitting = false) }
                }
                else -> {}
            }
        }
    }

    fun deletePlan(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = adminRepository.deletePlan(id)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Plán smazán") }
                    loadPlans()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                }
                else -> {}
            }
        }
    }

    fun uploadFile(planId: String, file: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(uploadingPlanId = planId, error = null) }
            when (val result = adminRepository.uploadPlanFile(planId, file)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            uploadingPlanId = null,
                            successMessage = "PDF nahráno"
                        )
                    }
                    loadPlans()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, uploadingPlanId = null) }
                }
                else -> {}
            }
        }
    }

    fun deleteFile(planId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(uploadingPlanId = planId, error = null) }
            when (val result = adminRepository.deletePlanFile(planId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            uploadingPlanId = null,
                            successMessage = "PDF smazáno"
                        )
                    }
                    loadPlans()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, uploadingPlanId = null) }
                }
                else -> {}
            }
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
