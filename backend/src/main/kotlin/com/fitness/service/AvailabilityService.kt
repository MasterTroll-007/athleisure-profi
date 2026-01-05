package com.fitness.service

import com.fitness.dto.AdminCalendarSlotDTO
import com.fitness.dto.AvailableSlotDTO
import com.fitness.dto.SlotReservationDTO
import com.fitness.entity.Slot
import com.fitness.entity.SlotStatus
import com.fitness.repository.ReservationRepository
import com.fitness.repository.SlotRepository
import com.fitness.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Service
class AvailabilityService(
    private val slotRepository: SlotRepository,
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository
) {

    /**
     * Get available slots for a specific date for user booking.
     * Only returns UNLOCKED slots that are not already reserved.
     */
    fun getAvailableSlots(date: LocalDate, userId: String): List<AvailableSlotDTO> {
        val now = LocalTime.now()
        val isToday = date == LocalDate.now()

        // Get all unlocked and reserved slots for this date (show both available and occupied)
        val slots = slotRepository.findByDate(date)
            .filter { it.status == SlotStatus.UNLOCKED || it.status == SlotStatus.RESERVED }

        // Get existing reservations for this date (map slotId to userId)
        val reservations = reservationRepository.findByDate(date)
            .filter { it.status == "confirmed" }
        val reservedSlots = reservations.map { it.slotId to it.startTime }.toSet()
        val slotToUserId = reservations.associate { it.slotId to it.userId.toString() }

        // Get ALL reservations for this date (for adjacent slot logic)
        val allDayReservations = reservationRepository.findByDate(date)
            .filter { it.status == "confirmed" }
            .map { it.startTime to it.endTime }

        val allSlots = slots.map { slot ->
            // Check if slot is in the past
            val isPast = isToday && slot.startTime.plusMinutes(15) < now

            // Check if slot is reserved (either by status or has a reservation)
            val isReserved = slot.status == SlotStatus.RESERVED || reservedSlots.contains(slot.id to slot.startTime)

            // Get the userId who reserved this slot (if any)
            val reservedByUserId = if (isReserved) slotToUserId[slot.id] else null

            AvailableSlotDTO(
                blockId = slot.id.toString(),
                date = date.toString(),
                start = "${date}T${slot.startTime}",
                end = "${date}T${slot.endTime}",
                isAvailable = !isReserved && !isPast,
                reservedByUserId = reservedByUserId
            )
        }.sortedBy { it.start }

        // If there are no reservations on this day, all available slots are selectable
        if (allDayReservations.isEmpty()) {
            return allSlots
        }

        // If there are reservations, only adjacent slots are available for selection
        val slotDuration = slots.firstOrNull()?.durationMinutes?.toLong() ?: 60L

        val adjacentTimes = allDayReservations.flatMap { (start, end) ->
            listOf(
                start.minusMinutes(slotDuration), // Slot before ANY reservation
                end // Slot after ANY reservation
            )
        }.toSet()

        return allSlots.map { slot ->
            val slotStartTime = LocalTime.parse(slot.start.substringAfter("T"))
            val isAdjacent = adjacentTimes.contains(slotStartTime)

            if (slot.isAvailable && !isAdjacent) {
                slot.copy(isAvailable = false)
            } else {
                slot
            }
        }
    }

    /**
     * Get available slots for a date range.
     */
    fun getAvailableSlotsRange(startDate: LocalDate, endDate: LocalDate, userId: String): List<AvailableSlotDTO> {
        val allSlots = mutableListOf<AvailableSlotDTO>()
        var currentDate = startDate
        while (currentDate <= endDate) {
            allSlots.addAll(getAvailableSlots(currentDate, userId))
            currentDate = currentDate.plusDays(1)
        }
        return allSlots.sortedWith(compareBy({ it.start }))
    }

    /**
     * Get all slots for admin calendar view within a date range.
     * Shows all statuses with reservation info if reserved.
     */
    fun getAdminCalendarSlots(startDate: LocalDate, endDate: LocalDate): List<AdminCalendarSlotDTO> {
        val slots = slotRepository.findByDateBetween(startDate, endDate)

        // Get all reservations in the date range
        val reservations = reservationRepository.findByDateRange(startDate, endDate)
        val reservationMap = reservations
            .filter { it.status == "confirmed" }
            .associateBy { it.slotId }

        val now = LocalTime.now()
        val today = LocalDate.now()

        return slots.map { slot ->
            val isPast = slot.date < today || (slot.date == today && slot.startTime.plusMinutes(15) < now)
            val reservation = reservationMap[slot.id]

            val status = when {
                slot.status == SlotStatus.RESERVED || reservation != null -> "reserved"
                slot.status == SlotStatus.BLOCKED -> "blocked"
                slot.status == SlotStatus.LOCKED -> "locked"
                isPast -> "past"
                else -> "available"
            }

            val reservationInfo = reservation?.let { res ->
                val user = userRepository.findById(res.userId).orElse(null)
                SlotReservationDTO(
                    id = res.id.toString(),
                    userName = user?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim().ifEmpty { null } },
                    userEmail = user?.email,
                    status = res.status,
                    note = res.note
                )
            }

            AdminCalendarSlotDTO(
                id = slot.id.toString(),
                blockId = slot.id.toString(),
                date = slot.date.toString(),
                startTime = slot.startTime.toString(),
                endTime = slot.endTime.toString(),
                status = status,
                reservation = reservationInfo
            )
        }.sortedWith(compareBy({ it.date }, { it.startTime }))
    }
}
