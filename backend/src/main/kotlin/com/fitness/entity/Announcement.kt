package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "announcements",
    indexes = [
        Index(name = "idx_announcement_trainer", columnList = "trainer_id")
    ]
)
data class Announcement(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "trainer_id", nullable = false)
    val trainerId: UUID,

    @Column(nullable = false)
    val subject: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    val message: String,

    @Column(name = "recipients_count", nullable = false)
    val recipientsCount: Int = 0,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
)
