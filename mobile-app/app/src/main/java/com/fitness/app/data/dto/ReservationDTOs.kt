package com.fitness.app.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReservationDTO(
    val id: String,
    val userId: String? = null,
    val date: String,
    val startTime: String,
    val endTime: String,
    val status: String,
    val creditsUsed: Int,
    val note: String? = null,
    val clientName: String? = null,
    val clientEmail: String? = null,
    val createdAt: String? = null
)

@Serializable
data class CreateReservationRequest(
    val date: String,
    val startTime: String,
    val endTime: String,
    val blockId: String? = null,
    val pricingItemId: String? = null
)

@Serializable
data class AvailableSlotDTO(
    val id: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val status: String,
    val durationMinutes: Int? = null
)

@Serializable
data class SlotDTO(
    val id: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int,
    val status: String,
    val note: String? = null,
    val assignedUserId: String? = null,
    val assignedUserName: String? = null,
    val assignedUserEmail: String? = null,
    val reservationId: String? = null,
    val createdAt: String? = null
)

@Serializable
data class CreateSlotRequest(
    val date: String,
    val startTime: String,
    val durationMinutes: Int,
    val note: String? = null,
    val assignedUserId: String? = null
)

@Serializable
data class UpdateSlotRequest(
    val status: String? = null,
    val note: String? = null,
    val assignedUserId: String? = null,
    val date: String? = null,
    val startTime: String? = null,
    val endTime: String? = null
)

@Serializable
data class UnlockWeekResponse(
    val unlockedCount: Int
)
