package com.fitness.repository

import com.fitness.entity.ReminderSent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ReminderSentLogRepository : JpaRepository<ReminderSent, UUID> {

    fun existsByReservationIdAndReminderType(reservationId: UUID, reminderType: String): Boolean

    @Query("SELECT r.reservationId FROM ReminderSent r WHERE r.reminderType = :reminderType")
    fun findReservationIdsByReminderType(reminderType: String): List<UUID>
}
