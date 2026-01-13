package com.fitness.app.data.repository

import com.fitness.app.data.api.ApiService
import com.fitness.app.data.dto.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditRepository @Inject constructor(
    private val apiService: ApiService
) : BaseRepository() {

    suspend fun getCreditBalance(): Result<CreditBalanceResponse> {
        return safeApiCall(fallbackError = "Failed to get credit balance") {
            apiService.getCreditBalance()
        }
    }

    suspend fun getCreditHistory(): Result<List<CreditTransactionDTO>> {
        return safeApiCall(fallbackError = "Failed to get credit history") {
            apiService.getCreditHistory()
        }
    }

    suspend fun getCreditPackages(): Result<List<CreditPackageDTO>> {
        return safeApiCall(fallbackError = "Failed to get credit packages") {
            apiService.getCreditPackages()
        }
    }

    suspend fun purchaseCredits(packageId: String): Result<PurchaseCreditsResponse> {
        return safeApiCall(fallbackError = "Failed to initiate purchase") {
            apiService.purchaseCredits(PurchaseCreditsRequest(packageId))
        }
    }
}
