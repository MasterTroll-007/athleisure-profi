package com.fitness.service

import com.fitness.dto.AdminCalendarSlotDTO
import com.fitness.dto.AvailableSlotDTO
import com.fitness.entity.SlotStatus
import com.fitness.mapper.SlotMapper
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
    private val userRepository: UserRepository,
    private val slotMapper: SlotMapper
) {

    /**
     * Get slots for a specific date for user booking.
     * Only shows slots from user's trainer.
     * Returns all slots with appropriate availability status:
     * - User's own reservations (isAvailable = false)
     * - Other's reservations (isAvailable = false, shown as occupied)
     * - Past slots (isAvailable = false)
     * - Free adjacent slots (isAvailable = true)
     * - Free non-adjacent slots when reservations exist (isAvailable = false)
     * - Cancelled slots treated as available
     */
    fun getAvailableSlots(date: LocalDate, userId: String): List<AvailableSlotDTO> {
        val now = LocalTime.now()
        val isToday = date == LocalDate.now()

        // Get user's trainer
        val user = userRepository.findById(UUID.fromString(userId)).orElse(null)
            ?: return emptyList()
        val trainerId = user.trainerId ?: return emptyList()

        // Get all unlocked and reserved slots for this date from user's trainer only
        val slots = slotRepository.findByDateAndAdminId(date, trainerId)
            .filter { it.status == SlotStatus.UNLOCKED || it.status == SlotStatus.RESERVED }

        if (slots.isEmpty()) return emptyList()

        val userUUID = UUID.fromString(userId)

        // Get all reservations for this date
        val allReservations = reservationRepository.findByDate(date)
        val confirmedReservations = allReservations.filter { it.status == "confirmed" }

        val confirmedSlotIds = confirmedReservations.map { it.slotId }.toSet()
        val slotToUserId = confirmedReservations.associate { it.slotId to it.userId.toString() }

        // Check if user already has a reservation on this date (max 1 per day)
        val userHasReservationToday = confirmedReservations.any { it.userId == userUUID }

        // Get confirmed reservation times for adjacent slot logic
        val confirmedReservationTimes = confirmedReservations.map { it.startTime to it.endTime }

        // Calculate adjacent times (slots that can be booked)
        val slotDuration = slots.firstOrNull()?.durationMinutes?.toLong() ?: 60L
        val adjacentTimes = confirmedReservationTimes.flatMap { (start, end) ->
            listOf(
                start.minusMinutes(slotDuration), // Slot before reservation
                end // Slot after reservation
            )
        }.toSet()

        return slots.mapNotNull { slot ->
            val isPast = isToday && slot.startTime.plusMinutes(15) < now
            val isConfirmedReservation = confirmedSlotIds.contains(slot.id)
            val reservedByUserId = if (isConfirmedReservation) slotToUserId[slot.id] else null
            val isUserReservation = reservedByUserId == userId
            val isFreeSlot = !isConfirmedReservation

            // Check if slot is adjacent (only relevant for free slots when reservations exist)
            val isAdjacent = confirmedReservationTimes.isEmpty() || adjacentTimes.contains(slot.startTime)

            // Hide non-adjacent free slots
            if (isFreeSlot && !isAdjacent) return@mapNotNull null

            // Hide free slots if user already has a reservation today (max 1 per day)
            if (isFreeSlot && userHasReservationToday) return@mapNotNull null

            // Determine availability
            val isAvailable = when {
                // User's own reservation - not available (already booked by them)
                isUserReservation -> false
                // Reserved by others - not available
                isConfirmedReservation -> false
                // Past slots - not available
                isPast -> false
                // Free adjacent slot - available
                else -> true
            }

            AvailableSlotDTO(
                blockId = slot.id.toString(),
                date = date.toString(),
                start = "${date}T${slot.startTime}",
                end = "${date}T${slot.endTime}",
                isAvailable = isAvailable,
                reservedByUserId = reservedByUserId
            )
        }.sortedBy { it.start }
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
        val reservations = reservationRepository.findByDateRange(startDate, endDate)
        return slotMapper.toAdminCalendarDTOBatch(slots, reservations)
    }
}
