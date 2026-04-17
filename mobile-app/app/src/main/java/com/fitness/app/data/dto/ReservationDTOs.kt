package com.fitness.app.data.dto

import kotlinx.serialization.SerialName
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
    // Backend serialises these as `userName` / `userEmail`. Map both so the
    // legacy `clientName` / `clientEmail` aliases still work in UI code.
    @SerialName("userName")
    val clientName: String? = null,
    @SerialName("userEmail")
    val clientEmail: String? = null,
    val createdAt: String? = null,
    val locationId: String? = null,
    val locationName: String? = null,
    val locationAddress: String? = null,
    val locationColor: String? = null
) {
    val userName: String? get() = clientName
    val userEmail: String? get() = clientEmail
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
data class PricingItemSummary(
    val id: String,
    val nameCs: String,
    val nameEn: String? = null,
    val credits: Int
)

@Serializable
data class AvailableSlotDTO(
    val blockId: String,
    val date: String,
    val start: String,
    val end: String,
    val isAvailable: Boolean,
    val reservedByUserId: String? = null,
    val pricingItems: List<PricingItemSummary> = emptyList(),
    val locationId: String? = null,
    val locationName: String? = null,
    val locationAddress: String? = null,
    val locationColor: String? = null
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
    val cancelledAt: String? = null,
    val pricingItems: List<PricingItemSummary> = emptyList(),
    val capacity: Int = 1,
    val currentBookings: Int = 0,
    val locationId: String? = null,
    val locationName: String? = null,
    val locationAddress: String? = null,
    val locationColor: String? = null
)

@Serializable
data class CreateSlotRequest(
    val date: String,
    val startTime: String,
    val durationMinutes: Int,
    val note: String? = null,
    val assignedUserId: String? = null,
    val locationId: String? = null
)

@Serializable
data class UpdateSlotRequest(
    val status: String? = null,
    val note: String? = null,
    val assignedUserId: String? = null,
    val date: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val locationId: String? = null,
    val clearLocation: Boolean? = null
)

@Serializable
data class UnlockWeekResponse(
    val unlockedCount: Int
)
