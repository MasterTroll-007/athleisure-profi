package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

enum class SlotStatus {
    LOCKED,     // Not visible to users, gray with lock icon
    UNLOCKED,   // Available for booking, visible to users
    RESERVED,   // Booked by a user
    BLOCKED     // Admin manually blocked
}

@Entity
@Table(
    name = "slots",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["date", "start_time"])
    ],
    indexes = [
        Index(name = "idx_slot_date_status", columnList = "date, status"),
        Index(name = "idx_slot_assigned_user", columnList = "assigned_user_id"),
        Index(name = "idx_slot_template", columnList = "template_id"),
        Index(name = "idx_slot_admin", columnList = "admin_id")
    ]
)
data class Slot(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var date: LocalDate,

    @Column(name = "start_time", nullable = false)
    var startTime: LocalTime,

    @Column(name = "end_time", nullable = false)
    var endTime: LocalTime,

    @Column(name = "duration_minutes", nullable = false)
    val durationMinutes: Int = 60,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SlotStatus = SlotStatus.LOCKED,

    @Column(name = "assigned_user_id")
    var assignedUserId: UUID? = null,

    var note: String? = null,

    @Column(name = "template_id")
    val templateId: UUID? = null,

    @Column(name = "admin_id")
    val adminId: UUID? = null,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
)
