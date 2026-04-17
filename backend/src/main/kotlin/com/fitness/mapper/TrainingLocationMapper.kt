package com.fitness.mapper

import com.fitness.dto.TrainingLocationDTO
import com.fitness.entity.TrainingLocation
import org.springframework.stereotype.Component

@Component
class TrainingLocationMapper {
    fun toDTO(location: TrainingLocation): TrainingLocationDTO {
        return TrainingLocationDTO(
            id = location.id!!.toString(),
            nameCs = location.nameCs,
            nameEn = location.nameEn,
            addressCs = location.addressCs,
            addressEn = location.addressEn,
            color = location.color,
            isActive = location.isActive,
            createdAt = location.createdAt.toString()
        )
    }

    fun toDTOBatch(locations: List<TrainingLocation>): List<TrainingLocationDTO> {
        return locations.map { toDTO(it) }
    }
}
