package com.fitness.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.dto.CreateTemplateRequest
import com.fitness.app.data.dto.SlotTemplateDTO
import com.fitness.app.data.dto.TemplateSlotDTO
import com.fitness.app.data.dto.UpdateTemplateRequest
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
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class AdminTemplatesUiState(
    val templates: List<SlotTemplateDTO> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showApplyDialog: Boolean = false,
    val selectedTemplate: SlotTemplateDTO? = null,
    val isProcessing: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class AdminTemplatesViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminTemplatesUiState())
    val uiState: StateFlow<AdminTemplatesUiState> = _uiState.asStateFlow()

    fun loadTemplates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = adminRepository.getTemplates()) {
                is Result.Success -> {
                    _uiState.update { it.copy(templates = result.data, isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                }
                else -> {}
            }
        }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true, selectedTemplate = null) }
    }

    fun showEditDialog(template: SlotTemplateDTO) {
        _uiState.update { it.copy(showEditDialog = true, selectedTemplate = template) }
    }

    fun showApplyDialog(template: SlotTemplateDTO) {
        _uiState.update { it.copy(showApplyDialog = true, selectedTemplate = template) }
    }

    fun dismissDialogs() {
        _uiState.update {
            it.copy(
                showCreateDialog = false,
                showEditDialog = false,
                showApplyDialog = false,
                selectedTemplate = null
            )
        }
    }

    fun createTemplate(name: String, slots: List<TemplateSlotDTO>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val request = CreateTemplateRequest(name, slots)
            when (val result = adminRepository.createTemplate(request)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            showCreateDialog = false,
                            snackbarMessage = "Template created"
                        )
                    }
                    loadTemplates()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isProcessing = false, snackbarMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun updateTemplate(id: String, name: String?, slots: List<TemplateSlotDTO>?, isActive: Boolean?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val request = UpdateTemplateRequest(name, slots, isActive)
            when (val result = adminRepository.updateTemplate(id, request)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            showEditDialog = false,
                            selectedTemplate = null,
                            snackbarMessage = "Template updated"
                        )
                    }
                    loadTemplates()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isProcessing = false, snackbarMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            when (val result = adminRepository.deleteTemplate(id)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            snackbarMessage = "Template deleted"
                        )
                    }
                    loadTemplates()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isProcessing = false, snackbarMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun applyTemplate(templateId: String, weekStartDate: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            // Adjust to Monday of the week
            val monday = weekStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            when (val result = adminRepository.applyTemplate(templateId, monday.toString())) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            showApplyDialog = false,
                            selectedTemplate = null,
                            snackbarMessage = "Created ${result.data.createdSlots} slots"
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isProcessing = false, snackbarMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
