package com.fitness.repository

import com.fitness.entity.BodyMeasurement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BodyMeasurementRepository : JpaRepository<BodyMeasurement, UUID> {
    fun findByUserIdOrderByDateDesc(userId: UUID): List<BodyMeasurement>
}
