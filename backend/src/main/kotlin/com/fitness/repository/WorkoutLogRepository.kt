package com.fitness.repository

import com.fitness.entity.WorkoutLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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

    @Query(
        value = """
            SELECT w FROM WorkoutLog w
            WHERE w.reservationId IN (SELECT r.id FROM Reservation r WHERE r.userId = :userId)
            ORDER BY w.createdAt DESC
        """,
        countQuery = """
            SELECT COUNT(w) FROM WorkoutLog w
            WHERE w.reservationId IN (SELECT r.id FROM Reservation r WHERE r.userId = :userId)
        """
    )
    fun findByUserId(userId: UUID, pageable: Pageable): Page<WorkoutLog>
}
