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
) {
    val userName: String? get() = clientName
}

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
    val blockId: String,
    val date: String,
    val start: String,
    val end: String,
    val isAvailable: Boolean
) {
    // Aliases for compatibility with UI code
    val id: String get() = blockId
    // Extract time part if datetime format, otherwise return as-is
    val startTime: String get() = if (start.contains("T")) start.substringAfter("T").take(5) else start
    val endTime: String get() = if (end.contains("T")) end.substringAfter("T").take(5) else end
}

@Serializable
data class AvailableSlotsResponse(
    val slots: List<AvailableSlotDTO>
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
    val createdAt: String? = null,
    val cancelledAt: String? = null
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
