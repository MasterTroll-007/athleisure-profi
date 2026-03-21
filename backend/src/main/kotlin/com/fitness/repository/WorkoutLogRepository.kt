package com.fitness.repository

import com.fitness.entity.WorkoutLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface WorkoutLogRepository : JpaRepository<WorkoutLog, UUID> {
    fun findByReservationId(reservationId: UUID): WorkoutLog?

    @Query("""
        SELECT w FROM WorkoutLog w
        WHERE w.reservationId IN (SELECT r.id FROM Reservation r WHERE r.userId = :userId)
        ORDER BY w.createdAt DESC
    """)
    fun findByUserId(userId: UUID): List<WorkoutLog>
}
