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
    val cancelledAt: String?
)

data class CreateReservationRequest(
    val date: String,
    val startTime: String,
    val endTime: String,
    val blockId: String,
    val pricingItemId: String? = null
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
    val startTime: String,
    val endTime: String,
    val isAvailable: Boolean
)
