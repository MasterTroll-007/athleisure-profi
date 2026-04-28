package com.fitness.service

import com.fitness.dto.AdminCalendarSlotDTO
import com.fitness.dto.AvailableSlotDTO
import com.fitness.dto.PricingItemSummary
import com.fitness.entity.SlotStatus
import com.fitness.mapper.SlotMapper
import com.fitness.repository.PricingItemRepository
import com.fitness.repository.ReservationRepository
import com.fitness.repository.SlotPricingItemRepository
import com.fitness.repository.SlotRepository
import com.fitness.repository.TrainingLocationRepository
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
    private val slotMapper: SlotMapper,
    private val locationRepository: TrainingLocationRepository,
    private val slotPricingItemRepository: SlotPricingItemRepository,
    private val pricingItemRepository: PricingItemRepository
) {
    private fun loadPricingItemsForSlots(slotIds: List<UUID>): Map<UUID, List<PricingItemSummary>> {
        if (slotIds.isEmpty()) return emptyMap()
        val slotPricingItems = slotPricingItemRepository.findBySlotIdIn(slotIds)
        if (slotPricingItems.isEmpty()) return emptyMap()
        val pricingItemIds = slotPricingItems.map { it.pricingItemId }.distinct()
        val pricingItems = pricingItemRepository.findAllById(pricingItemIds).associateBy { it.id }
        return slotPricingItems.groupBy({ it.slotId }) { spi ->
            pricingItems[spi.pricingItemId]?.let {
                PricingItemSummary(it.id.toString(), it.nameCs, it.nameEn, it.credits)
            }
        }.mapValues { (_, values) -> values.filterNotNull() }
    }

    /**
     * Get slots for a specific date for user booking.
     * Only shows slots from user's trainer.
     * Returns all slots with appropriate availability status:
     * - User's own reservations (isAvailable = false)
     * - Other's reservations (isAvailable = false, shown as occupied)
     * - Past slots (isAvailable = false)
     * - Free adjacent slots when adjacent booking is enabled (isAvailable = true)
     * - Free non-adjacent slots when adjacent booking is disabled (isAvailable = true)
     * - Cancelled slots treated as available
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
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

        val slotIds = slots.mapNotNull { it.id }.toSet()
        val confirmedReservations = if (slotIds.isEmpty()) {
            emptyList()
        } else {
            reservationRepository.findConfirmedByDateAndSlotIdIn(date, slotIds)
        }

        val confirmedSlotIds = confirmedReservations.map { it.slotId }.toSet()
        val slotToUserId = confirmedReservations.associate { it.slotId to it.userId.toString() }

        // Get trainer's adjacent booking setting
        val trainer = userRepository.findById(trainerId).orElse(null)
        val adjacentRequired = trainer?.adjacentBookingRequired ?: true

        // Get confirmed reservation times for adjacent slot logic.
        val confirmedReservationTimes = confirmedReservations.map { it.startTime to it.endTime }

        val locationIds = slots.mapNotNull { it.locationId }.toSet()
        val locationMap = if (locationIds.isNotEmpty()) {
            locationRepository.findAllById(locationIds).associateBy { it.id!! }
        } else emptyMap()
        val pricingItemsMap = loadPricingItemsForSlots(slots.mapNotNull { it.id })

        return slots.mapNotNull { slot ->
            val isPast = isToday && slot.startTime.plusMinutes(15) < now
            val isConfirmedReservation = confirmedSlotIds.contains(slot.id)
            val reservedByUserId = if (isConfirmedReservation) slotToUserId[slot.id] else null
            val isUserReservation = reservedByUserId == userId
            val isFreeSlot = !isConfirmedReservation

            // Check if slot is adjacent (only relevant when adjacent restriction is enabled).
            val isAdjacent = !adjacentRequired ||
                confirmedReservationTimes.isEmpty() ||
                confirmedReservationTimes.any { (start, end) ->
                    slot.startTime == start.minusMinutes(slot.durationMinutes.toLong()) ||
                        slot.startTime == end
                }

            // Hide non-adjacent free slots (only when adjacent restriction is on)
            if (adjacentRequired && isFreeSlot && !isAdjacent) return@mapNotNull null

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

            val location = slot.locationId?.let { locationMap[it] }

            AvailableSlotDTO(
                slotId = slot.id.toString(),
                date = date.toString(),
                start = "${date}T${slot.startTime}",
                end = "${date}T${slot.endTime}",
                isAvailable = isAvailable,
                reservedByUserId = reservedByUserId,
                pricingItems = pricingItemsMap[slot.id] ?: emptyList(),
                locationId = slot.locationId?.toString(),
                locationName = location?.nameCs,
                locationAddress = location?.addressCs,
                locationColor = location?.color
            )
        }.sortedBy { it.start }
    }

    /**
     * Get available slots for a date range.
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
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
