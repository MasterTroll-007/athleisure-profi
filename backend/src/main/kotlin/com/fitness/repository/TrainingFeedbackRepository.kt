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

    @Query("SELECT COUNT(f) FROM TrainingFeedback f WHERE f.userId IN (SELECT r.userId FROM Reservation r WHERE r.slotId IN (SELECT s.id FROM Slot s WHERE s.adminId = :trainerId))")
    fun countForTrainer(trainerId: UUID): Long

    @Query("SELECT f.rating, COUNT(f) FROM TrainingFeedback f WHERE f.userId IN (SELECT r.userId FROM Reservation r WHERE r.slotId IN (SELECT s.id FROM Slot s WHERE s.adminId = :trainerId)) GROUP BY f.rating")
    fun getRatingDistributionForTrainer(trainerId: UUID): List<Array<Any>>

    @Query("SELECT f FROM TrainingFeedback f WHERE f.userId IN (SELECT r.userId FROM Reservation r WHERE r.slotId IN (SELECT s.id FROM Slot s WHERE s.adminId = :trainerId)) ORDER BY f.createdAt DESC")
    fun findAllForTrainer(trainerId: UUID): List<TrainingFeedback>
}
