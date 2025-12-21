package com.fitness.services

import com.fitness.models.AvailableSlot
import com.fitness.models.AvailableSlotsResponse
import com.fitness.repositories.AvailabilityBlockRepository
import com.fitness.repositories.ReservationRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

object AvailabilityService {

    /**
     * Get available slots for a specific date.
     *
     * CRITICAL LOGIC - "Slot Sticking" Rule:
     * 1. First reservation of the day can be at ANY time within the block
     * 2. Subsequent reservations MUST be "stuck" to existing reservations:
     *    - Either immediately BEFORE (new end = existing start)
     *    - Or immediately AFTER (new start = existing end)
     * 3. This creates continuous blocks without gaps
     */
    fun getAvailableSlots(date: LocalDate): AvailableSlotsResponse {
        val dayOfWeek = date.dayOfWeek.value // 1 = Monday, 7 = Sunday
        val blocks = AvailabilityBlockRepository.findActiveForDay(dayOfWeek)

        if (blocks.isEmpty()) {
            return AvailableSlotsResponse(
                date = date.toString(),
                slots = emptyList()
            )
        }

        val allSlots = mutableListOf<AvailableSlot>()

        for (block in blocks) {
            val existingReservations = ReservationRepository.findConfirmedByDateAndBlock(date, block.id)
            val slotDuration = block.slotDurationMinutes.toLong()

            if (existingReservations.isEmpty()) {
                // No reservations yet - all slots in the block are available
                var currentStart = block.startTime
                while (currentStart.plusMinutes(slotDuration) <= block.endTime) {
                    val slotEnd = currentStart.plusMinutes(slotDuration)
                    allSlots.add(
                        AvailableSlot(
                            start = "${date}T${currentStart}:00",
                            end = "${date}T${slotEnd}:00",
                            blockId = block.id.toString()
                        )
                    )
                    currentStart = slotEnd
                }
            } else {
                // Apply "slot sticking" rule - only slots adjacent to existing reservations
                val availableSlots = calculateStickySlots(
                    blockStart = block.startTime,
                    blockEnd = block.endTime,
                    slotDuration = slotDuration,
                    existingReservations = existingReservations,
                    date = date,
                    blockId = block.id.toString()
                )
                allSlots.addAll(availableSlots)
            }
        }

        return AvailableSlotsResponse(
            date = date.toString(),
            slots = allSlots.sortedBy { it.start }
        )
    }

    private fun calculateStickySlots(
        blockStart: LocalTime,
        blockEnd: LocalTime,
        slotDuration: Long,
        existingReservations: List<com.fitness.repositories.ReservationTimeSlot>,
        date: LocalDate,
        blockId: String
    ): List<AvailableSlot> {
        val slots = mutableListOf<AvailableSlot>()

        // Sort reservations by start time
        val sorted = existingReservations.sortedBy { it.startTime }

        // Find the earliest and latest reservation times
        val earliestStart = sorted.minOf { it.startTime }
        val latestEnd = sorted.maxOf { it.endTime }

        // Check slot BEFORE the first reservation
        val slotBefore = earliestStart.minusMinutes(slotDuration)
        if (slotBefore >= blockStart && !isSlotOverlapping(slotBefore, earliestStart, sorted)) {
            slots.add(
                AvailableSlot(
                    start = "${date}T${slotBefore}:00",
                    end = "${date}T${earliestStart}:00",
                    blockId = blockId
                )
            )
        }

        // Check slot AFTER the last reservation
        val slotAfter = latestEnd.plusMinutes(slotDuration)
        if (slotAfter <= blockEnd && !isSlotOverlapping(latestEnd, slotAfter, sorted)) {
            slots.add(
                AvailableSlot(
                    start = "${date}T${latestEnd}:00",
                    end = "${date}T${slotAfter}:00",
                    blockId = blockId
                )
            )
        }

        // Check for gaps between reservations (if any exist)
        for (i in 0 until sorted.size - 1) {
            val currentEnd = sorted[i].endTime
            val nextStart = sorted[i + 1].startTime

            // If there's a gap big enough for a slot
            val gapMinutes = java.time.Duration.between(currentEnd, nextStart).toMinutes()
            if (gapMinutes >= slotDuration) {
                // Slot right after current reservation
                val slotEndAfterCurrent = currentEnd.plusMinutes(slotDuration)
                if (slotEndAfterCurrent <= nextStart) {
                    slots.add(
                        AvailableSlot(
                            start = "${date}T${currentEnd}:00",
                            end = "${date}T${slotEndAfterCurrent}:00",
                            blockId = blockId
                        )
                    )
                }

                // Slot right before next reservation (if different from above)
                val slotStartBeforeNext = nextStart.minusMinutes(slotDuration)
                if (slotStartBeforeNext >= currentEnd && slotStartBeforeNext != currentEnd) {
                    slots.add(
                        AvailableSlot(
                            start = "${date}T${slotStartBeforeNext}:00",
                            end = "${date}T${nextStart}:00",
                            blockId = blockId
                        )
                    )
                }
            }
        }

        return slots.distinctBy { it.start }
    }

    private fun isSlotOverlapping(
        slotStart: LocalTime,
        slotEnd: LocalTime,
        existingReservations: List<com.fitness.repositories.ReservationTimeSlot>
    ): Boolean {
        return existingReservations.any { reservation ->
            !(slotEnd <= reservation.startTime || slotStart >= reservation.endTime)
        }
    }

    /**
     * Validate that a slot can be reserved (follows the sticking rule)
     */
    fun validateSlotForReservation(
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        blockId: UUID
    ): Boolean {
        val availableSlots = getAvailableSlots(date)
        val requestedStart = "${date}T${startTime}:00"
        val requestedEnd = "${date}T${endTime}:00"

        return availableSlots.slots.any { slot ->
            slot.start == requestedStart &&
            slot.end == requestedEnd &&
            slot.blockId == blockId.toString()
        }
    }
}
