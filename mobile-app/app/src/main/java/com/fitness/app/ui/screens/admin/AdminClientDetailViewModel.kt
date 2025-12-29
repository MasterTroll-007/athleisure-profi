package com.fitness.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.dto.ClientDTO
import com.fitness.app.data.dto.ClientNoteDTO
import com.fitness.app.data.dto.ReservationDTO
import com.fitness.app.data.repository.AdminRepository
import com.fitness.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminClientDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val client: ClientDTO? = null,
    val reservations: List<ReservationDTO> = emptyList(),
    val notes: List<ClientNoteDTO> = emptyList(),
    val isAdjusting: Boolean = false,
    val showAddNoteDialog: Boolean = false,
    val isSubmittingNote: Boolean = false,
    val successMessage: String? = null
)

@HiltViewModel
class AdminClientDetailViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminClientDetailUiState())
    val uiState: StateFlow<AdminClientDetailUiState> = _uiState.asStateFlow()

    private var currentClientId: String? = null

    fun loadClient(clientId: String) {
        currentClientId = clientId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load client
            when (val clientResult = adminRepository.getClient(clientId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(client = clientResult.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = clientResult.message) }
                    return@launch
                }
                is Result.Loading -> {}
            }

            // Load reservations
            when (val reservationsResult = adminRepository.getClientReservations(clientId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(reservations = reservationsResult.data) }
                }
                is Result.Error -> {
                    // Don't fail completely for reservations error
                }
                is Result.Loading -> {}
            }

            // Load notes
            when (val notesResult = adminRepository.getClientNotes(clientId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(notes = notesResult.data, isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun adjustCredits(clientId: String, amount: Int, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAdjusting = true) }

            when (val result = adminRepository.adjustCredits(clientId, amount, reason)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isAdjusting = false, successMessage = "Kredity upraveny") }
                    // Reload client to get updated balance
                    loadClient(clientId)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isAdjusting = false,
                            error = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun showAddNoteDialog() {
        _uiState.update { it.copy(showAddNoteDialog = true) }
    }

    fun dismissAddNoteDialog() {
        _uiState.update { it.copy(showAddNoteDialog = false) }
    }

    fun createNote(content: String) {
        val clientId = currentClientId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingNote = true) }

            when (val result = adminRepository.createClientNote(clientId, content)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingNote = false,
                            showAddNoteDialog = false,
                            successMessage = "Poznámka přidána"
                        )
                    }
                    loadClient(clientId)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingNote = false,
                            error = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun deleteNote(noteId: String) {
        val clientId = currentClientId ?: return
        viewModelScope.launch {
            when (val result = adminRepository.deleteClientNote(noteId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(successMessage = "Poznámka smazána") }
                    loadClient(clientId)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
