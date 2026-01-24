package com.fitness.repository

import com.fitness.entity.ReminderSentLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ReminderSentLogRepository : JpaRepository<ReminderSentLog, UUID> {

    fun existsByReservationIdAndReminderType(reservationId: UUID, reminderType: String): Boolean

    @Query("SELECT r.reservationId FROM ReminderSentLog r WHERE r.reminderType = :reminderType")
    fun findReservationIdsByReminderType(reminderType: String): List<UUID>
}
