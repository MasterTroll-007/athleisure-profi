package com.fitness.repository

import com.fitness.entity.RecurringReservation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RecurringReservationRepository : JpaRepository<RecurringReservation, UUID> {
    fun findByUserIdAndStatus(userId: UUID, status: String): List<RecurringReservation>
    fun findByUserId(userId: UUID): List<RecurringReservation>
}
