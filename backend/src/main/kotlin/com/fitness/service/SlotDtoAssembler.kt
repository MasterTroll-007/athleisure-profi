package com.fitness.service

import com.fitness.dto.PricingItemSummary
import com.fitness.dto.SlotDTO
import com.fitness.entity.Reservation
import com.fitness.entity.Slot
import com.fitness.entity.TrainingLocation
import com.fitness.entity.User
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

@Component
class SlotDtoAssembler {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun calendarAssignedUserName(user: User?): String? {
        if (user == null) return null
        val lastName = user.lastName?.trim().orEmpty()
        val firstName = user.firstName?.trim().orEmpty()
        return when {
            lastName.isNotEmpty() && firstName.isNotEmpty() -> "$lastName\n$firstName"
            lastName.isNotEmpty() -> lastName
            firstName.isNotEmpty() -> firstName
            else -> null
        }
    }

    fun defaultAssignedUserName(user: User?): String? =
        user?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim().ifEmpty { null } }

    fun toDto(
        slot: Slot,
        status: String = slot.status.name.lowercase(),
        user: User? = null,
        assignedUserName: String? = defaultAssignedUserName(user),
        reservation: Reservation? = null,
        cancelledReservation: Reservation? = null,
        pricingItems: List<PricingItemSummary> = emptyList(),
        currentBookings: Int = 0,
        location: TrainingLocation? = null
    ): SlotDTO =
        SlotDTO(
            id = slot.id.toString(),
            date = slot.date.format(dateFormatter),
            startTime = slot.startTime.format(timeFormatter),
            endTime = slot.endTime.format(timeFormatter),
            durationMinutes = slot.durationMinutes,
            status = status,
            assignedUserId = user?.id?.toString(),
            assignedUserName = assignedUserName,
            assignedUserEmail = user?.email,
            note = reservation?.note ?: slot.note,
            reservationId = reservation?.id?.toString(),
            createdAt = slot.createdAt.toString(),
            cancelledAt = cancelledReservation?.cancelledAt?.toString(),
            pricingItems = pricingItems,
            capacity = slot.capacity,
            currentBookings = currentBookings,
            locationId = slot.locationId?.toString(),
            locationName = location?.nameCs,
            locationAddress = location?.addressCs,
            locationColor = location?.color
        )
}
