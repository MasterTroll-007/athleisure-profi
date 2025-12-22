package com.fitness.dto

data class ReservationDTO(
    val id: String,
    val userId: String,
    val userName: String?,
    val userEmail: String?,
    val blockId: String?,
    val date: String,
    val startTime: String,
    val endTime: String,
    val status: String,
    val creditsUsed: Int,
    val pricingItemId: String?,
    val pricingItemName: String?,
    val createdAt: String,
    val cancelledAt: String?,
    val note: String? = null
)

data class CreateReservationRequest(
    val date: String,
    val startTime: String,
    val endTime: String,
    val blockId: String,
    val pricingItemId: String? = null
)

data class AdminCreateReservationRequest(
    val userId: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val blockId: String,
    val deductCredits: Boolean = false,
    val note: String? = null
)

data class UpdateReservationNoteRequest(
    val note: String?
)

data class AdminCancelReservationRequest(
    val refundCredits: Boolean = true
)

data class ReservationCalendarEvent(
    val id: String,
    val title: String,
    val start: String,
    val end: String,
    val status: String,
    val clientName: String?,
    val clientEmail: String?
)

data class AvailableSlotDTO(
    val blockId: String,
    val date: String,
    val start: String,
    val end: String,
    val isAvailable: Boolean
)

data class AvailableSlotsResponse(
    val slots: List<AvailableSlotDTO>
)
