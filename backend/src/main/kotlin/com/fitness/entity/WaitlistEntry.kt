package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "waitlist_entries",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "slot_id"])
    ],
    indexes = [
        Index(name = "idx_waitlist_slot", columnList = "slot_id"),
        Index(name = "idx_waitlist_user", columnList = "user_id"),
        Index(name = "idx_waitlist_status", columnList = "status")
    ]
)
data class WaitlistEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "slot_id", nullable = false)
    val slotId: UUID,

    @Column(nullable = false)
    val status: String = "waiting",

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @Column(name = "notified_at")
    val notifiedAt: Instant? = null,

    @Column(name = "expires_at")
    val expiresAt: Instant? = null
)
