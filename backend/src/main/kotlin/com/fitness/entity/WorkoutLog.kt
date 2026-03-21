package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "workout_logs",
    indexes = [
        Index(name = "idx_workout_reservation", columnList = "reservation_id")
    ]
)
data class WorkoutLog(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "reservation_id", nullable = false, unique = true)
    val reservationId: UUID,

    @Column(columnDefinition = "TEXT")
    val exercises: String? = null,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
