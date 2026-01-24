package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "reminders_sent",
    indexes = [
        Index(name = "idx_reminder_reservation", columnList = "reservation_id"),
        Index(name = "idx_reminder_user", columnList = "user_id"),
        Index(name = "idx_reminder_sent_at", columnList = "sent_at")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_reminder_reservation_type", columnNames = ["reservation_id", "reminder_type"])
    ]
)
data class ReminderSent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "reservation_id", nullable = false)
    val reservationId: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "reminder_type", nullable = false)
    val reminderType: String,

    @Column(name = "sent_at", nullable = false)
    val sentAt: Instant = Instant.now()
)
