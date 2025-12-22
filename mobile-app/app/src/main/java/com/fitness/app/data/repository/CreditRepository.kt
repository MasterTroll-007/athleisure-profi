package com.fitness.app.data.repository

import com.fitness.app.data.api.ApiService
import com.fitness.app.data.dto.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getCreditBalance(): Result<CreditBalanceResponse> {
        return try {
            val response = apiService.getCreditBalance()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get credit balance")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getCreditHistory(): Result<List<CreditTransactionDTO>> {
        return try {
            val response = apiService.getCreditHistory()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get credit history")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getCreditPackages(): Result<List<CreditPackageDTO>> {
        return try {
            val response = apiService.getCreditPackages()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get credit packages")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun purchaseCredits(packageId: String): Result<PurchaseCreditsResponse> {
        return try {
            val response = apiService.purchaseCredits(PurchaseCreditsRequest(packageId))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to initiate purchase")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
