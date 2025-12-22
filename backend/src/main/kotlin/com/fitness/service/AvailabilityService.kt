package com.fitness.service

import com.fitness.dto.AdminCalendarSlotDTO
import com.fitness.dto.AvailableSlotDTO
import com.fitness.dto.SlotReservationDTO
import com.fitness.entity.AvailabilityBlock
import com.fitness.repository.AvailabilityBlockRepository
import com.fitness.repository.ReservationRepository
import com.fitness.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Service
class AvailabilityService(
    private val availabilityBlockRepository: AvailabilityBlockRepository,
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository
) {

    fun getAvailableSlots(date: LocalDate, userId: String): List<AvailableSlotDTO> {
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

        // Get existing reservations for this date (all users)
        val allReservations = reservationRepository.findByDate(date)
        val bookedSlots = allReservations
            .filter { it.status == "confirmed" }
            .map { it.blockId to it.startTime }
            .toSet()

        // Get user's reservations for this date
        val userReservations = allReservations
            .filter { it.userId == UUID.fromString(userId) && it.status == "confirmed" }
            .map { it.startTime to it.endTime }

        // Generate all slots first
        val allSlots = blocksForDay.flatMap { block ->
            generateSlotsFromBlock(block, date, bookedSlots, blockedBlocks)
        }.sortedBy { it.start }

        // If user has no reservations on this day, all available slots are selectable
        if (userReservations.isEmpty()) {
            return allSlots
        }

        // If user has reservations, only adjacent slots are available for selection
        val slotDuration = blocksForDay.firstOrNull()?.let {
            (it.slotDuration ?: it.slotDurationMinutes ?: 60).toLong()
        } ?: 60L

        val adjacentTimes = userReservations.flatMap { (start, end) ->
            listOf(
                start.minusMinutes(slotDuration), // Slot before
                end // Slot after (end time = next slot's start time)
            )
        }.toSet()

        return allSlots.map { slot ->
            val slotStartTime = LocalTime.parse(slot.start.substringAfter("T"))
            val isAdjacent = adjacentTimes.contains(slotStartTime)

            // If slot is already booked or blocked, keep it as is
            // If slot is available but not adjacent, mark as unavailable
            if (slot.isAvailable && !isAdjacent) {
                slot.copy(isAvailable = false)
            } else {
                slot
            }
        }
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

        // Get current time for filtering past slots
        val now = LocalTime.now()
        val isToday = date == LocalDate.now()

        while (currentTime.plusMinutes(slotDuration) <= block.endTime) {
            val endTime = currentTime.plusMinutes(slotDuration)

            // Check if slot is in the past (for today only)
            // Allow booking if start time is within 15 minutes of current time
            val isPast = isToday && currentTime.plusMinutes(15) < now

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
                    isAvailable = !isBlocked && !isBooked && !isPast
                )
            )

            currentTime = endTime
        }

        return slots
    }

    fun getAvailableSlotsRange(startDate: LocalDate, endDate: LocalDate, userId: String): List<AvailableSlotDTO> {
        val allSlots = mutableListOf<AvailableSlotDTO>()
        var currentDate = startDate
        while (currentDate <= endDate) {
            allSlots.addAll(getAvailableSlots(currentDate, userId))
            currentDate = currentDate.plusDays(1)
        }
        return allSlots.sortedWith(compareBy({ it.start }))
    }

    fun getAllBlocks(): List<AvailabilityBlock> {
        return availabilityBlockRepository.findAll()
    }

    fun getActiveBlocks(): List<AvailabilityBlock> {
        return availabilityBlockRepository.findByIsActiveTrue()
    }

    /**
     * Get all generated slots for admin calendar view within a date range.
     * Each slot shows its status and reservation info if reserved.
     */
    fun getAdminCalendarSlots(startDate: LocalDate, endDate: LocalDate): List<AdminCalendarSlotDTO> {
        val allBlocks = availabilityBlockRepository.findByIsActiveTrue()
            .filter { it.isBlocked != true }

        // Get all reservations in the date range
        val reservations = reservationRepository.findByDateRange(startDate, endDate)
        val reservationMap = reservations
            .filter { it.status == "confirmed" }
            .associateBy { Triple(it.date, it.startTime, it.blockId) }

        // Get all blocked slots in the date range
        val blockedSlots = availabilityBlockRepository.findByIsActiveTrue()
            .filter { it.isBlocked == true && it.specificDate != null }
            .filter { it.specificDate!! >= startDate && it.specificDate!! <= endDate }

        val now = LocalTime.now()
        val today = LocalDate.now()
        val result = mutableListOf<AdminCalendarSlotDTO>()

        var currentDate = startDate
        while (currentDate <= endDate) {
            val dayNumber = currentDate.dayOfWeek.value.toString()

            // Find blocks for this day
            val blocksForDay = allBlocks.filter { block ->
                if (block.dayOfWeek == currentDate.dayOfWeek) return@filter true
                if (block.daysOfWeek.isNotEmpty()) {
                    val days = block.daysOfWeek.split(",").map { it.trim() }
                    if (days.contains(dayNumber)) return@filter true
                }
                false
            }

            // Generate slots for each block
            for (block in blocksForDay) {
                var currentTime = block.startTime
                val slotDuration = (block.slotDuration ?: block.slotDurationMinutes ?: 60).toLong()

                while (currentTime.plusMinutes(slotDuration) <= block.endTime) {
                    val endTime = currentTime.plusMinutes(slotDuration)
                    val slotId = "${block.id}-${currentDate}-${currentTime}"

                    // Determine slot status
                    val isPast = currentDate < today || (currentDate == today && currentTime.plusMinutes(15) < now)

                    val isBlocked = blockedSlots.any { blocked ->
                        blocked.specificDate == currentDate &&
                        currentTime >= blocked.startTime && currentTime < blocked.endTime
                    }

                    val reservation = reservationMap[Triple(currentDate, currentTime, block.id)]

                    val status = when {
                        reservation != null -> "reserved"
                        isBlocked -> "blocked"
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

                    result.add(AdminCalendarSlotDTO(
                        id = slotId,
                        blockId = block.id.toString(),
                        date = currentDate.toString(),
                        startTime = currentTime.toString(),
                        endTime = endTime.toString(),
                        status = status,
                        reservation = reservationInfo
                    ))

                    currentTime = endTime
                }
            }

            currentDate = currentDate.plusDays(1)
        }

        return result.sortedWith(compareBy({ it.date }, { it.startTime }))
    }

    /**
     * Block or unblock a specific time slot for a specific date.
     * Creates or removes a blocked availability entry.
     */
    fun blockSlot(date: LocalDate, startTime: LocalTime, endTime: LocalTime, isBlocked: Boolean): AvailabilityBlock {
        // Check if there's already a block entry for this slot
        val existing = availabilityBlockRepository.findByIsActiveTrue()
            .find { it.specificDate == date && it.startTime == startTime && it.isBlocked == true }

        if (isBlocked) {
            // Create or update blocked slot
            if (existing != null) {
                return existing
            }
            return availabilityBlockRepository.save(AvailabilityBlock(
                specificDate = date,
                startTime = startTime,
                endTime = endTime,
                isBlocked = true,
                isActive = true,
                daysOfWeek = ""
            ))
        } else {
            // Remove block
            if (existing != null) {
                availabilityBlockRepository.delete(existing)
            }
            return existing ?: throw NoSuchElementException("Blocked slot not found")
        }
    }
}
