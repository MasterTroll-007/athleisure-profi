package com.fitness.repository

import com.fitness.entity.ReminderSent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ReminderSentRepository : JpaRepository<ReminderSent, UUID> {
    fun existsByReservationIdAndReminderType(reservationId: UUID, reminderType: String): Boolean

    @Query("SELECT rs.reservationId FROM ReminderSent rs WHERE rs.reservationId IN :reservationIds AND rs.reminderType = :reminderType")
    fun findReservationIdsWithReminder(reservationIds: List<UUID>, reminderType: String): Set<UUID>
}
