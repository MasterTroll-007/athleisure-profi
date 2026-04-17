package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "training_feedback",
    indexes = [
        Index(name = "idx_feedback_reservation", columnList = "reservation_id"),
        Index(name = "idx_feedback_user", columnList = "user_id")
    ]
)
data class TrainingFeedback(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "reservation_id", nullable = false, unique = true)
    val reservationId: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val rating: Int, // 1-5

    @Column(columnDefinition = "TEXT")
    val comment: String? = null,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
)
