package com.fitness.app.data.api

import com.fitness.app.data.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ==================== AUTH ====================

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<MessageResponse>

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): Response<MessageResponse>

    @POST("auth/resend-verification")
    suspend fun resendVerification(@Body request: ResendVerificationRequest): Response<MessageResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<MessageResponse>

    @GET("auth/me")
    suspend fun getProfile(): Response<UserDTO>

    @PATCH("auth/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserDTO>

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<MessageResponse>

    // ==================== RESERVATIONS (Client) ====================

    @GET("reservations")
    suspend fun getReservations(): Response<List<ReservationDTO>>

    @GET("reservations/upcoming")
    suspend fun getUpcomingReservations(): Response<List<ReservationDTO>>

    @GET("reservations/available")
    suspend fun getAvailableSlots(
        @Query("start") start: String,
        @Query("end") end: String
    ): Response<AvailableSlotsResponse>

    @POST("reservations")
    suspend fun createReservation(@Body request: CreateReservationRequest): Response<ReservationDTO>

    @DELETE("reservations/{id}")
    suspend fun cancelReservation(@Path("id") id: String): Response<MessageResponse>

    // ==================== CREDITS ====================

    @GET("credits/balance")
    suspend fun getCreditBalance(): Response<CreditBalanceResponse>

    @GET("credits/history")
    suspend fun getCreditHistory(): Response<List<CreditTransactionDTO>>

    @GET("credits/packages")
    suspend fun getCreditPackages(): Response<List<CreditPackageDTO>>

    @POST("credits/purchase")
    suspend fun purchaseCredits(@Body request: PurchaseCreditsRequest): Response<PurchaseCreditsResponse>

    // ==================== PLANS ====================

    @GET("plans")
    suspend fun getPlans(): Response<List<TrainingPlanDTO>>

    @GET("plans/my")
    suspend fun getMyPlans(): Response<List<PurchasedPlanDTO>>

    @POST("plans/{id}/purchase")
    suspend fun purchasePlan(@Path("id") id: String): Response<PurchasedPlanDTO>

    // ==================== ADMIN ====================

    @GET("admin/dashboard")
    suspend fun getAdminStats(): Response<AdminStatsDTO>

    @GET("admin/reservations/today")
    suspend fun getTodayReservations(): Response<List<ReservationDTO>>

    // Admin Clients
    @GET("admin/clients")
    suspend fun getClients(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ClientsPageDTO>

    @GET("admin/clients/search")
    suspend fun searchClients(@Query("q") query: String): Response<List<ClientDTO>>

    @GET("admin/clients/{id}")
    suspend fun getClient(@Path("id") id: String): Response<ClientDTO>

    @GET("admin/clients/{id}/reservations")
    suspend fun getClientReservations(@Path("id") id: String): Response<List<ReservationDTO>>

    @POST("admin/credits/adjust")
    suspend fun adjustCredits(@Body request: AdjustCreditsRequest): Response<CreditTransactionDTO>

    // Admin Reservations
    @GET("admin/reservations")
    suspend fun getAdminReservations(
        @Query("start") start: String,
        @Query("end") end: String
    ): Response<List<ReservationDTO>>

    @POST("admin/reservations")
    suspend fun createAdminReservation(@Body request: AdminCreateReservationRequest): Response<ReservationDTO>

    @DELETE("admin/reservations/{id}")
    suspend fun cancelAdminReservation(
        @Path("id") id: String,
        @Query("refundCredits") refundCredits: Boolean = true
    ): Response<MessageResponse>

    @PATCH("admin/reservations/{id}/note")
    suspend fun updateReservationNote(
        @Path("id") id: String,
        @Body request: UpdateReservationNoteRequest
    ): Response<ReservationDTO>

    // Admin Slots
    @GET("admin/slots")
    suspend fun getSlots(
        @Query("start") start: String,
        @Query("end") end: String
    ): Response<List<SlotDTO>>

    @POST("admin/slots")
    suspend fun createSlot(@Body request: CreateSlotRequest): Response<SlotDTO>

    @PATCH("admin/slots/{id}")
    suspend fun updateSlot(
        @Path("id") id: String,
        @Body request: UpdateSlotRequest
    ): Response<SlotDTO>

    @DELETE("admin/slots/{id}")
    suspend fun deleteSlot(@Path("id") id: String): Response<MessageResponse>

    @POST("admin/slots/unlock-week")
    suspend fun unlockWeek(@Body request: UnlockWeekRequest): Response<UnlockWeekResponse>

    // Admin Templates
    @GET("admin/templates")
    suspend fun getTemplates(): Response<List<SlotTemplateDTO>>

    @POST("admin/templates")
    suspend fun createTemplate(@Body request: CreateTemplateRequest): Response<SlotTemplateDTO>

    @PATCH("admin/templates/{id}")
    suspend fun updateTemplate(
        @Path("id") id: String,
        @Body request: UpdateTemplateRequest
    ): Response<SlotTemplateDTO>

    @DELETE("admin/templates/{id}")
    suspend fun deleteTemplate(@Path("id") id: String): Response<MessageResponse>

    @POST("admin/slots/apply-template")
    suspend fun applyTemplate(@Body request: ApplyTemplateRequest): Response<ApplyTemplateResponse>

    // Admin Plans
    @GET("admin/plans")
    suspend fun getAdminPlans(): Response<List<TrainingPlanDTO>>

    // Admin Pricing
    @GET("admin/pricing")
    suspend fun getPricing(): Response<List<PricingItemDTO>>

    // Admin Payments
    @GET("admin/payments")
    suspend fun getPayments(): Response<List<GopayPaymentDTO>>
}
