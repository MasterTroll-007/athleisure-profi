package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(
    name = "body_measurements",
    indexes = [
        Index(name = "idx_measurement_user", columnList = "user_id"),
        Index(name = "idx_measurement_date", columnList = "user_id, date")
    ]
)
data class BodyMeasurement(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val date: LocalDate,

    val weight: Double? = null,

    @Column(name = "body_fat")
    val bodyFat: Double? = null,

    val chest: Double? = null,
    val waist: Double? = null,
    val hips: Double? = null,
    val bicep: Double? = null,
    val thigh: Double? = null,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(name = "created_by", nullable = false)
    val createdBy: UUID,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
)
