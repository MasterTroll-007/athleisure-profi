package com.fitness.mapper

import com.fitness.dto.AvailabilityBlockDTO
import com.fitness.entity.AvailabilityBlock
import org.springframework.stereotype.Component

@Component
class AvailabilityBlockMapper {
    /**
     * Convert AvailabilityBlock entity to AvailabilityBlockDTO.
     */
    fun toDTO(block: AvailabilityBlock): AvailabilityBlockDTO {
        return AvailabilityBlockDTO(
            id = block.id.toString(),
            name = block.name,
            daysOfWeek = parseDaysOfWeek(block.daysOfWeek),
            dayOfWeek = block.dayOfWeek?.name,
            specificDate = block.specificDate?.toString(),
            startTime = block.startTime.toString(),
            endTime = block.endTime.toString(),
            slotDurationMinutes = block.slotDurationMinutes ?: block.slotDuration,
            isRecurring = block.isRecurring,
            isBlocked = block.isBlocked,
            isActive = block.isActive,
            createdAt = block.createdAt.toString()
        )
    }

    /**
     * Batch convert AvailabilityBlock entities to DTOs.
     */
    fun toDTOBatch(blocks: List<AvailabilityBlock>): List<AvailabilityBlockDTO> {
        return blocks.map { toDTO(it) }
    }

    /**
     * Parse days of week string (e.g., "1,2,3,4,5") to list of integers.
     */
    private fun parseDaysOfWeek(daysOfWeek: String): List<Int> {
        return if (daysOfWeek.isNotEmpty()) {
            daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
        } else {
            emptyList()
        }
    }
}
