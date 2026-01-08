package com.fitness.service

import com.fitness.entity.AvailabilityBlock
import com.fitness.repository.AvailabilityBlockRepository
import org.springframework.stereotype.Service
import java.time.LocalTime
import java.util.UUID

@Service
class AvailabilityBlockValidationService(
    private val availabilityBlockRepository: AvailabilityBlockRepository
) {
    /**
     * Check if a new availability block overlaps with existing active blocks.
     * Only checks non-blocked blocks for overlap.
     *
     * @param daysOfWeek List of days (1=Monday, 7=Sunday)
     * @param startTime Block start time
     * @param endTime Block end time
     * @param excludeBlockId Optional block ID to exclude (for updates)
     * @param adminId Optional admin ID to filter blocks by owner
     * @return The overlapping block if found, null otherwise
     */
    fun checkForOverlappingBlocks(
        daysOfWeek: List<Int>,
        startTime: LocalTime,
        endTime: LocalTime,
        excludeBlockId: UUID? = null,
        adminId: UUID? = null
    ): AvailabilityBlock? {
        val existingBlocks = if (adminId != null) {
            availabilityBlockRepository.findByIsActiveTrueAndAdminId(adminId)
        } else {
            availabilityBlockRepository.findByIsActiveTrue()
        }
            .filter { it.id != excludeBlockId }
            .filter { it.isBlocked != true }

        for (existingBlock in existingBlocks) {
            // Check time overlap: startA < endB AND endA > startB
            val timesOverlap = startTime < existingBlock.endTime && endTime > existingBlock.startTime

            if (!timesOverlap) continue

            // Check day overlap
            val existingDays = if (existingBlock.daysOfWeek.isNotEmpty()) {
                existingBlock.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
            } else if (existingBlock.dayOfWeek != null) {
                listOf(existingBlock.dayOfWeek!!.value)
            } else {
                emptyList()
            }

            val daysOverlap = daysOfWeek.any { it in existingDays }

            if (daysOverlap) {
                return existingBlock
            }
        }

        return null
    }

    /**
     * Formats an error message for overlapping blocks.
     */
    fun formatOverlapError(block: AvailabilityBlock): Map<String, String> {
        return mapOf(
            "error" to "OVERLAPPING_BLOCK",
            "message" to "Blok se překrývá s existujícím blokem '${block.name ?: "Bez názvu"}' (${block.startTime}-${block.endTime})"
        )
    }
}
