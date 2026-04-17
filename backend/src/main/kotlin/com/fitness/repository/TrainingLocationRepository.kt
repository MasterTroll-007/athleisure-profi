package com.fitness.repository

import com.fitness.entity.TrainingLocation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TrainingLocationRepository : JpaRepository<TrainingLocation, UUID> {
    @Query("SELECT l FROM TrainingLocation l WHERE l.isActive = true ORDER BY l.nameCs")
    fun findByIsActiveTrue(): List<TrainingLocation>

    @Query("SELECT l FROM TrainingLocation l WHERE l.adminId = :adminId ORDER BY l.nameCs")
    fun findByAdminId(adminId: UUID): List<TrainingLocation>
}
