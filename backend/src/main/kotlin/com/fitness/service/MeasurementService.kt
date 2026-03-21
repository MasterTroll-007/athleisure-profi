package com.fitness.service

import com.fitness.dto.CreateMeasurementRequest
import com.fitness.dto.MeasurementDTO
import com.fitness.entity.BodyMeasurement
import com.fitness.repository.BodyMeasurementRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class MeasurementService(
    private val measurementRepository: BodyMeasurementRepository
) {

    @Transactional
    fun createMeasurement(userId: String, request: CreateMeasurementRequest, createdBy: String): MeasurementDTO {
        val measurement = measurementRepository.save(
            BodyMeasurement(
                userId = UUID.fromString(userId),
                date = LocalDate.parse(request.date),
                weight = request.weight,
                bodyFat = request.bodyFat,
                chest = request.chest,
                waist = request.waist,
                hips = request.hips,
                bicep = request.bicep,
                thigh = request.thigh,
                notes = request.notes,
                createdBy = UUID.fromString(createdBy)
            )
        )
        return toDTO(measurement)
    }

    fun getMeasurements(userId: String): List<MeasurementDTO> {
        return measurementRepository.findByUserIdOrderByDateDesc(UUID.fromString(userId)).map { toDTO(it) }
    }

    private fun toDTO(m: BodyMeasurement) = MeasurementDTO(
        id = m.id.toString(),
        userId = m.userId.toString(),
        date = m.date.toString(),
        weight = m.weight,
        bodyFat = m.bodyFat,
        chest = m.chest,
        waist = m.waist,
        hips = m.hips,
        bicep = m.bicep,
        thigh = m.thigh,
        notes = m.notes,
        createdAt = m.createdAt.toString()
    )
}
