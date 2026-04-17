package com.fitness.mapper

import com.fitness.dto.AvailabilityBlockDTO
import com.fitness.entity.AvailabilityBlock
import com.fitness.entity.TrainingLocation
import com.fitness.repository.TrainingLocationRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class AvailabilityBlockMapper(
    private val locationRepository: TrainingLocationRepository
) {
    fun toDTO(block: AvailabilityBlock): AvailabilityBlockDTO {
        val location = block.locationId?.let { locationRepository.findById(it).orElse(null) }
        return toDTO(block, location)
    }

    fun toDTOBatch(blocks: List<AvailabilityBlock>): List<AvailabilityBlockDTO> {
        if (blocks.isEmpty()) return emptyList()
        val locationIds = blocks.mapNotNull { it.locationId }.toSet()
        val locationMap = if (locationIds.isNotEmpty()) {
            locationRepository.findAllById(locationIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }
        return blocks.map { toDTO(it, it.locationId?.let { id -> locationMap[id] }) }
    }

    private fun toDTO(block: AvailabilityBlock, location: TrainingLocation?): AvailabilityBlockDTO {
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
            locationId = block.locationId?.toString(),
            locationName = location?.nameCs,
            locationColor = location?.color,
            createdAt = block.createdAt.toString()
        )
    }

    private fun parseDaysOfWeek(daysOfWeek: String): List<Int> {
        return if (daysOfWeek.isNotEmpty()) {
            daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
        } else {
            emptyList()
        }
    }
}
