package com.fitness.app.ui.screens.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.dto.PurchasedPlanDTO
import com.fitness.app.data.dto.TrainingPlanDTO
import com.fitness.app.data.repository.PlanRepository
import com.fitness.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlansUiState(
    val plans: List<TrainingPlanDTO> = emptyList(),
    val myPlans: List<PurchasedPlanDTO> = emptyList(),
    val isPlansLoading: Boolean = false,
    val isMyPlansLoading: Boolean = false,
    val isPurchasing: Boolean = false,
    val plansError: String? = null,
    val myPlansError: String? = null,
    val showPurchaseDialog: Boolean = false,
    val selectedPlan: TrainingPlanDTO? = null
)

@HiltViewModel
class PlansViewModel @Inject constructor(
    private val planRepository: PlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlansUiState())
    val uiState: StateFlow<PlansUiState> = _uiState.asStateFlow()

    fun loadData() {
        loadPlans()
        loadMyPlans()
    }

    fun loadPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPlansLoading = true, plansError = null) }

            when (val result = planRepository.getPlans()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isPlansLoading = false,
                            plans = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isPlansLoading = false,
                            plansError = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun loadMyPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isMyPlansLoading = true, myPlansError = null) }

            when (val result = planRepository.getMyPlans()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isMyPlansLoading = false,
                            myPlans = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isMyPlansLoading = false,
                            myPlansError = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun purchasePlan(plan: TrainingPlanDTO) {
        _uiState.update {
            it.copy(
                showPurchaseDialog = true,
                selectedPlan = plan
            )
        }
    }

    fun dismissPurchaseDialog() {
        _uiState.update {
            it.copy(
                showPurchaseDialog = false,
                selectedPlan = null
            )
        }
    }

    fun confirmPurchase() {
        val plan = _uiState.value.selectedPlan ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true) }

            when (val result = planRepository.purchasePlan(plan.id)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isPurchasing = false,
                            showPurchaseDialog = false,
                            selectedPlan = null
                        )
                    }
                    loadMyPlans()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isPurchasing = false,
                            plansError = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }
}
