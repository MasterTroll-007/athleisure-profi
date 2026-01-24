package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "cancellation_policies",
    indexes = [
        Index(name = "idx_cancellation_policy_trainer", columnList = "trainer_id")
    ]
)
data class CancellationPolicy(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "trainer_id", nullable = false, unique = true)
    val trainerId: UUID,

    @Column(name = "full_refund_hours", nullable = false)
    val fullRefundHours: Int = 24,

    @Column(name = "partial_refund_hours")
    val partialRefundHours: Int? = null,

    @Column(name = "partial_refund_percentage")
    val partialRefundPercentage: Int? = null,

    @Column(name = "no_refund_hours", nullable = false)
    val noRefundHours: Int = 0,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
