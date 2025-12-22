package com.fitness.app.data.repository

import com.fitness.app.data.api.ApiService
import com.fitness.app.data.dto.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getPlans(): Result<List<TrainingPlanDTO>> {
        return try {
            val response = apiService.getPlans()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get plans")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getMyPlans(): Result<List<PurchasedPlanDTO>> {
        return try {
            val response = apiService.getMyPlans()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get my plans")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun purchasePlan(planId: String): Result<PurchasedPlanDTO> {
        return try {
            val response = apiService.purchasePlan(planId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to purchase plan")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
