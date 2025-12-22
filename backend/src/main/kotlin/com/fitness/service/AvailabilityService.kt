package com.fitness.service

import com.fitness.dto.AvailableSlotDTO
import com.fitness.entity.AvailabilityBlock
import com.fitness.repository.AvailabilityBlockRepository
import com.fitness.repository.ReservationRepository
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Service
class AvailabilityService(
    private val availabilityBlockRepository: AvailabilityBlockRepository,
    private val reservationRepository: ReservationRepository
) {

    fun getAvailableSlots(date: LocalDate): List<AvailableSlotDTO> {
        val dayOfWeek = date.dayOfWeek
        val dayNumber = dayOfWeek.value.toString() // 1=Monday, 7=Sunday

        // Get all active blocks
        val allBlocks = availabilityBlockRepository.findByIsActiveTrue()

        // Filter blocks for this day
        val blocksForDay = allBlocks.filter { block ->
            // Check dayOfWeek enum
            if (block.dayOfWeek == dayOfWeek) return@filter true
            
            // Check daysOfWeek string (comma-separated numbers: 1=Mon, 2=Tue, etc.)
            if (block.daysOfWeek.isNotEmpty()) {
                val days = block.daysOfWeek.split(",").map { it.trim() }
                if (days.contains(dayNumber)) return@filter true
            }
            
            // Check specific date
            if (block.specificDate == date && block.isBlocked != true) return@filter true
            
            false
        }.filter { it.isBlocked != true }

        // Get blocked times for this date
        val blockedBlocks = allBlocks.filter { 
            it.specificDate == date && it.isBlocked == true 
        }

        // Get existing reservations for this date
        val reservations = reservationRepository.findByDate(date)
        val bookedSlots = reservations
            .filter { it.status == "confirmed" }
            .map { it.blockId to it.startTime }
            .toSet()

        // Generate available slots
        return blocksForDay.flatMap { block ->
            generateSlotsFromBlock(block, date, bookedSlots, blockedBlocks)
        }.sortedBy { it.start }
    }

    private fun generateSlotsFromBlock(
        block: AvailabilityBlock,
        date: LocalDate,
        bookedSlots: Set<Pair<java.util.UUID?, LocalTime>>,
        blockedBlocks: List<AvailabilityBlock>
    ): List<AvailableSlotDTO> {
        val slots = mutableListOf<AvailableSlotDTO>()
        var currentTime = block.startTime
        val slotDuration = (block.slotDuration ?: block.slotDurationMinutes ?: 60).toLong()

        while (currentTime.plusMinutes(slotDuration) <= block.endTime) {
            val endTime = currentTime.plusMinutes(slotDuration)

            // Check if slot is blocked
            val isBlocked = blockedBlocks.any { blocked ->
                currentTime >= blocked.startTime && currentTime < blocked.endTime
            }

            // Check if slot is booked
            val isBooked = bookedSlots.contains(block.id to currentTime)

            slots.add(
                AvailableSlotDTO(
                    blockId = block.id.toString(),
                    date = date.toString(),
                    start = "${date}T${currentTime}",
                    end = "${date}T${endTime}",
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

    fun getActiveBlocks(): List<AvailabilityBlock> {
        return availabilityBlockRepository.findByIsActiveTrue()
    }
}
