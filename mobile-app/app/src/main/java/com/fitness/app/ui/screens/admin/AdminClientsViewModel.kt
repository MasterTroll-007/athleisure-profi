package com.fitness.app.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.dto.ClientDTO
import com.fitness.app.data.repository.AdminRepository
import com.fitness.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminClientsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val clients: List<ClientDTO> = emptyList(),
    val isSearching: Boolean = false
)

@HiltViewModel
class AdminClientsViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminClientsUiState())
    val uiState: StateFlow<AdminClientsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun loadClients() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = adminRepository.getClients()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            clients = result.data.content
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

    fun searchClients(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce

            _uiState.update { it.copy(isSearching = true) }

            when (val result = adminRepository.searchClients(query)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            clients = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            error = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }
}
