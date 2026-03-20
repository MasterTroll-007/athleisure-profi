package com.fitness.repository

import com.fitness.entity.TrainingFeedback
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TrainingFeedbackRepository : JpaRepository<TrainingFeedback, UUID> {
    fun findByReservationId(reservationId: UUID): TrainingFeedback?
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<TrainingFeedback>
    fun existsByReservationId(reservationId: UUID): Boolean

    @Query("SELECT AVG(f.rating) FROM TrainingFeedback f WHERE f.userId IN (SELECT r.userId FROM Reservation r WHERE r.slotId IN (SELECT s.id FROM Slot s WHERE s.adminId = :trainerId))")
    fun getAverageRatingForTrainer(trainerId: UUID): Double?
}
