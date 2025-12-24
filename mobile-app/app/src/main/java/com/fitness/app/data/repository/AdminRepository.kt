package com.fitness.app.data.repository

import com.fitness.app.data.api.ApiService
import com.fitness.app.data.dto.*
import com.fitness.app.util.ValidationUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for admin operations.
 * Provides methods for managing clients, slots, templates, plans, and payments.
 */
@Singleton
class AdminRepository @Inject constructor(
    private val apiService: ApiService
) : BaseRepository() {

    suspend fun getDashboardStats(): Result<AdminStatsDTO> = safeApiCall("Failed to get stats") {
        apiService.getAdminStats()
    }

    suspend fun getTodayReservations(): Result<List<ReservationDTO>> = safeApiCall("Failed to get reservations") {
        apiService.getTodayReservations()
    }

    suspend fun getClients(page: Int = 0, size: Int = 20): Result<ClientsPageDTO> = safeApiCall("Failed to get clients") {
        apiService.getClients(page, size)
    }

    suspend fun searchClients(query: String): Result<List<ClientDTO>> = safeApiCall("Search failed") {
        apiService.searchClients(query)
    }

    suspend fun getClient(id: String): Result<ClientDTO> = safeApiCall("Failed to get client") {
        apiService.getClient(id)
    }

    suspend fun getClientReservations(id: String): Result<List<ReservationDTO>> = safeApiCall("Failed to get reservations") {
        apiService.getClientReservations(id)
    }

    suspend fun adjustCredits(userId: String, amount: Int, reason: String): Result<CreditTransactionDTO> {
        // Validate input
        ValidationUtils.validateCreditAmount(amount)?.let {
            return Result.Error(it)
        }
        val sanitizedReason = ValidationUtils.sanitizeText(reason)
            ?: return Result.Error("Reason is required")

        return safeApiCall("Failed to adjust credits") {
            apiService.adjustCredits(AdjustCreditsRequest(userId, amount, sanitizedReason))
        }
    }

    suspend fun getSlots(start: String, end: String): Result<List<SlotDTO>> = safeApiCall("Failed to get slots") {
        apiService.getSlots(start, end)
    }

    suspend fun createSlot(request: CreateSlotRequest): Result<SlotDTO> = safeApiCall("Failed to create slot") {
        apiService.createSlot(request)
    }

    suspend fun updateSlot(id: String, request: UpdateSlotRequest): Result<SlotDTO> = safeApiCall("Failed to update slot") {
        apiService.updateSlot(id, request)
    }

    suspend fun deleteSlot(id: String): Result<String> = safeApiCallForMessage(
        fallbackError = "Failed to delete slot",
        successMessage = "Slot deleted"
    ) {
        apiService.deleteSlot(id)
    }

    suspend fun unlockWeek(weekStartDate: String): Result<UnlockWeekResponse> = safeApiCall("Failed to unlock week") {
        apiService.unlockWeek(UnlockWeekRequest(weekStartDate))
    }

    // Templates
    suspend fun getTemplates(): Result<List<SlotTemplateDTO>> = safeApiCall("Failed to get templates") {
        apiService.getTemplates()
    }

    suspend fun createTemplate(request: CreateTemplateRequest): Result<SlotTemplateDTO> = safeApiCall("Failed to create template") {
        apiService.createTemplate(request)
    }

    suspend fun updateTemplate(id: String, request: UpdateTemplateRequest): Result<SlotTemplateDTO> = safeApiCall("Failed to update template") {
        apiService.updateTemplate(id, request)
    }

    suspend fun deleteTemplate(id: String): Result<String> = safeApiCallForMessage(
        fallbackError = "Failed to delete template",
        successMessage = "Template deleted"
    ) {
        apiService.deleteTemplate(id)
    }

    suspend fun applyTemplate(templateId: String, weekStartDate: String): Result<ApplyTemplateResponse> = safeApiCall("Failed to apply template") {
        apiService.applyTemplate(ApplyTemplateRequest(templateId, weekStartDate))
    }

    // Plans
    suspend fun getAdminPlans(): Result<List<TrainingPlanDTO>> = safeApiCall("Failed to get plans") {
        apiService.getAdminPlans()
    }

    // Pricing
    suspend fun getPricing(): Result<List<PricingItemDTO>> = safeApiCall("Failed to get pricing") {
        apiService.getPricing()
    }

    // Payments
    suspend fun getPayments(): Result<List<GopayPaymentDTO>> = safeApiCall("Failed to get payments") {
        apiService.getPayments()
    }
}
