package com.fitness.service

import com.fitness.dto.AvailableSlotDTO
import com.fitness.entity.AvailabilityBlock
import com.fitness.repository.AvailabilityBlockRepository
import com.fitness.repository.ReservationRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime

@Service
class AvailabilityService(
    private val availabilityBlockRepository: AvailabilityBlockRepository,
    private val reservationRepository: ReservationRepository
) {

    fun getAvailableSlots(date: LocalDate): List<AvailableSlotDTO> {
        val dayOfWeek = date.dayOfWeek

        // Get recurring blocks for this day of week
        val recurringBlocks = availabilityBlockRepository.findByDayOfWeekAndIsRecurringTrueAndIsBlockedFalse(dayOfWeek)

        // Get specific date blocks
        val specificBlocks = availabilityBlockRepository.findBySpecificDateAndIsBlockedFalse(date)

        // Get blocked times for this date
        val blockedTimes = availabilityBlockRepository.findBySpecificDateAndIsBlockedTrue(date)

        // Combine blocks
        val allBlocks = (recurringBlocks + specificBlocks).distinctBy { it.id }

        // Get existing reservations for this date
        val reservations = reservationRepository.findByDate(date)
        val bookedSlots = reservations
            .filter { it.status == "confirmed" }
            .map { it.blockId to it.startTime }
            .toSet()

        // Generate available slots
        return allBlocks.flatMap { block ->
            generateSlotsFromBlock(block, date, bookedSlots, blockedTimes)
        }.sortedBy { it.startTime }
    }

    private fun generateSlotsFromBlock(
        block: AvailabilityBlock,
        date: LocalDate,
        bookedSlots: Set<Pair<java.util.UUID?, LocalTime>>,
        blockedTimes: List<AvailabilityBlock>
    ): List<AvailableSlotDTO> {
        val slots = mutableListOf<AvailableSlotDTO>()
        var currentTime = block.startTime
        val slotDuration = block.slotDuration.toLong()

        while (currentTime.plusMinutes(slotDuration) <= block.endTime) {
            val endTime = currentTime.plusMinutes(slotDuration)

            // Check if slot is blocked
            val isBlocked = blockedTimes.any { blocked ->
                currentTime >= blocked.startTime && currentTime < blocked.endTime
            }

            // Check if slot is booked
            val isBooked = bookedSlots.contains(block.id to currentTime)

            slots.add(
                AvailableSlotDTO(
                    blockId = block.id.toString(),
                    date = date.toString(),
                    startTime = currentTime.toString(),
                    endTime = endTime.toString(),
                    isAvailable = !isBlocked && !isBooked
                )
            )

            currentTime = endTime
        }

        return slots
    }

    fun getAllBlocks(): List<AvailabilityBlock> {
        return availabilityBlockRepository.findAll()
    }

    fun getRecurringBlocks(): List<AvailabilityBlock> {
        return availabilityBlockRepository.findAllRecurring()
    }
}
