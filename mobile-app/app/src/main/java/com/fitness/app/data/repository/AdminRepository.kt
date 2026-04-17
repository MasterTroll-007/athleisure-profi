package com.fitness.app.data.repository

import android.net.Uri
import android.content.Context
import com.fitness.app.data.api.ApiService
import com.fitness.app.data.dto.*
import com.fitness.app.util.ValidationUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
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

    // Admin Reservations
    suspend fun getAdminReservations(start: String, end: String): Result<List<ReservationDTO>> = safeApiCall("Failed to get reservations") {
        apiService.getAdminReservations(start, end)
    }

    suspend fun createAdminReservation(request: AdminCreateReservationRequest): Result<ReservationDTO> = safeApiCall("Failed to create reservation") {
        apiService.createAdminReservation(request)
    }

    suspend fun cancelAdminReservation(id: String, refundCredits: Boolean = true): Result<String> = safeApiCallForMessage(
        fallbackError = "Failed to cancel reservation",
        successMessage = "Reservation cancelled"
    ) {
        apiService.cancelAdminReservation(id, refundCredits)
    }

    suspend fun updateReservationNote(id: String, note: String?): Result<ReservationDTO> = safeApiCall("Failed to update note") {
        apiService.updateReservationNote(id, UpdateReservationNoteRequest(note))
    }

    suspend fun getClientNotes(id: String): Result<List<ClientNoteDTO>> = safeApiCall("Failed to get notes") {
        apiService.getClientNotes(id)
    }

    suspend fun createClientNote(clientId: String, content: String): Result<ClientNoteDTO> = safeApiCall("Failed to create note") {
        apiService.createClientNote(clientId, CreateClientNoteRequest(content))
    }

    suspend fun deleteClientNote(noteId: String): Result<String> = safeApiCallForMessage(
        fallbackError = "Failed to delete note",
        successMessage = "Note deleted"
    ) {
        apiService.deleteClientNote(noteId)
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
    suspend fun getAdminPlans(): Result<List<AdminTrainingPlanDTO>> = safeApiCall("Failed to get plans") {
        apiService.getAdminPlans()
    }

    suspend fun createPlan(request: CreateTrainingPlanRequest): Result<AdminTrainingPlanDTO> = safeApiCall("Failed to create plan") {
        apiService.createPlan(request)
    }

    suspend fun updatePlan(id: String, request: UpdateTrainingPlanRequest): Result<AdminTrainingPlanDTO> = safeApiCall("Failed to update plan") {
        apiService.updatePlan(id, request)
    }

    suspend fun deletePlan(id: String): Result<String> = safeApiCallForMessage(
        fallbackError = "Failed to delete plan",
        successMessage = "Plan deleted"
    ) {
        apiService.deletePlan(id)
    }

    suspend fun uploadPlanFile(planId: String, file: File): Result<AdminTrainingPlanDTO> = safeApiCall("Failed to upload file") {
        val requestBody = file.asRequestBody("application/pdf".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        apiService.uploadPlanFile(planId, part)
    }

    suspend fun deletePlanFile(planId: String): Result<AdminTrainingPlanDTO> = safeApiCall("Failed to delete file") {
        apiService.deletePlanFile(planId)
    }

    // Pricing
    suspend fun getPricing(): Result<List<PricingItemDTO>> = safeApiCall("Failed to get pricing") {
        apiService.getPricing()
    }

    // Payments
    suspend fun getPayments(): Result<List<PaymentDTO>> = safeApiCall("Failed to get payments") {
        apiService.getPayments()
    }

    // Packages (Credit Packages)
    suspend fun getAdminPackages(): Result<List<AdminCreditPackageDTO>> = safeApiCall("Failed to get packages") {
        apiService.getAdminPackages()
    }

    suspend fun createPackage(request: CreateCreditPackageRequest): Result<AdminCreditPackageDTO> = safeApiCall("Failed to create package") {
        apiService.createPackage(request)
    }

    suspend fun updatePackage(id: String, request: UpdateCreditPackageRequest): Result<AdminCreditPackageDTO> = safeApiCall("Failed to update package") {
        apiService.updatePackage(id, request)
    }

    suspend fun deletePackage(id: String): Result<String> = safeApiCallForMessage(
        fallbackError = "Failed to delete package",
        successMessage = "Package deleted"
    ) {
        apiService.deletePackage(id)
    }
}
