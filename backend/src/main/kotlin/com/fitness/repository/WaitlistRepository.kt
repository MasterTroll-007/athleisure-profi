package com.fitness.repository

import com.fitness.entity.WaitlistEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface WaitlistRepository : JpaRepository<WaitlistEntry, UUID> {
    fun findBySlotIdAndStatusOrderByCreatedAt(slotId: UUID, status: String): List<WaitlistEntry>
    fun findByUserIdAndStatus(userId: UUID, status: String): List<WaitlistEntry>
    fun findByUserId(userId: UUID): List<WaitlistEntry>
    fun existsByUserIdAndSlotIdAndStatus(userId: UUID, slotId: UUID, status: String): Boolean

    @Query("SELECT w FROM WaitlistEntry w WHERE w.status = 'notified' AND w.expiresAt < :now")
    fun findExpiredNotifications(now: Instant): List<WaitlistEntry>
}
