package com.fitness.dto

import jakarta.validation.constraints.*

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
    @field:NotBlank(message = "Date is required")
    @field:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}\$", message = "Date must be in YYYY-MM-DD format")
    val date: String,

    @field:NotBlank(message = "Start time is required")
    @field:Pattern(regexp = "^\\d{2}:\\d{2}\$", message = "Start time must be in HH:mm format")
    val startTime: String,

    @field:NotBlank(message = "End time is required")
    @field:Pattern(regexp = "^\\d{2}:\\d{2}\$", message = "End time must be in HH:mm format")
    val endTime: String,

    @field:NotBlank(message = "Block ID is required")
    val blockId: String,

    val pricingItemId: String? = null
)

data class AdminCreateReservationRequest(
    @field:NotBlank(message = "User ID is required")
    val userId: String,

    @field:NotBlank(message = "Date is required")
    @field:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}\$", message = "Date must be in YYYY-MM-DD format")
    val date: String,

    @field:NotBlank(message = "Start time is required")
    @field:Pattern(regexp = "^\\d{2}:\\d{2}\$", message = "Start time must be in HH:mm format")
    val startTime: String,

    @field:NotBlank(message = "End time is required")
    @field:Pattern(regexp = "^\\d{2}:\\d{2}\$", message = "End time must be in HH:mm format")
    val endTime: String,

    @field:NotBlank(message = "Block ID is required")
    val blockId: String,

    val deductCredits: Boolean = false,

    @field:Size(max = 500, message = "Note too long")
    val note: String? = null
)

data class UpdateReservationNoteRequest(
    @field:Size(max = 500, message = "Note too long")
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
    val isAvailable: Boolean,
    val reservedByUserId: String? = null
)

data class AvailableSlotsResponse(
    val slots: List<AvailableSlotDTO>
)
