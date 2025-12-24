package com.fitness.app.data.repository

import com.fitness.app.data.api.ApiService
import com.fitness.app.data.dto.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getDashboardStats(): Result<AdminStatsDTO> {
        return try {
            val response = apiService.getAdminStats()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get stats")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getTodayReservations(): Result<List<ReservationDTO>> {
        return try {
            val response = apiService.getTodayReservations()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get reservations")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getClients(page: Int = 0, size: Int = 20): Result<ClientsPageDTO> {
        return try {
            val response = apiService.getClients(page, size)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get clients")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun searchClients(query: String): Result<List<ClientDTO>> {
        return try {
            val response = apiService.searchClients(query)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Search failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getClient(id: String): Result<ClientDTO> {
        return try {
            val response = apiService.getClient(id)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get client")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getClientReservations(id: String): Result<List<ReservationDTO>> {
        return try {
            val response = apiService.getClientReservations(id)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get reservations")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun adjustCredits(userId: String, amount: Int, reason: String): Result<CreditTransactionDTO> {
        return try {
            val response = apiService.adjustCredits(AdjustCreditsRequest(userId, amount, reason))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to adjust credits")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getSlots(start: String, end: String): Result<List<SlotDTO>> {
        return try {
            val response = apiService.getSlots(start, end)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get slots")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun createSlot(request: CreateSlotRequest): Result<SlotDTO> {
        return try {
            val response = apiService.createSlot(request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to create slot")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun updateSlot(id: String, request: UpdateSlotRequest): Result<SlotDTO> {
        return try {
            val response = apiService.updateSlot(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to update slot")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun deleteSlot(id: String): Result<String> {
        return try {
            val response = apiService.deleteSlot(id)
            if (response.isSuccessful) {
                Result.Success(response.body()?.message ?: "Slot deleted")
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to delete slot")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun unlockWeek(weekStartDate: String): Result<UnlockWeekResponse> {
        return try {
            val request = UnlockWeekRequest(weekStartDate)
            val response = apiService.unlockWeek(request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to unlock week")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    // Templates
    suspend fun getTemplates(): Result<List<SlotTemplateDTO>> {
        return try {
            val response = apiService.getTemplates()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get templates")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun createTemplate(request: CreateTemplateRequest): Result<SlotTemplateDTO> {
        return try {
            val response = apiService.createTemplate(request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to create template")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun updateTemplate(id: String, request: UpdateTemplateRequest): Result<SlotTemplateDTO> {
        return try {
            val response = apiService.updateTemplate(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to update template")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun deleteTemplate(id: String): Result<String> {
        return try {
            val response = apiService.deleteTemplate(id)
            if (response.isSuccessful) {
                Result.Success(response.body()?.message ?: "Template deleted")
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to delete template")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun applyTemplate(templateId: String, weekStartDate: String): Result<ApplyTemplateResponse> {
        return try {
            val response = apiService.applyTemplate(ApplyTemplateRequest(templateId, weekStartDate))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to apply template")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    // Plans
    suspend fun getAdminPlans(): Result<List<TrainingPlanDTO>> {
        return try {
            val response = apiService.getAdminPlans()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get plans")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    // Pricing
    suspend fun getPricing(): Result<List<PricingItemDTO>> {
        return try {
            val response = apiService.getPricing()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get pricing")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    // Payments
    suspend fun getPayments(): Result<List<GopayPaymentDTO>> {
        return try {
            val response = apiService.getPayments()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Failed to get payments")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
