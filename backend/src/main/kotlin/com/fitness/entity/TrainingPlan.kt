package com.fitness.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "training_plans")
data class TrainingPlan(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val name: String,

    val description: String? = null,

    @Column(nullable = false)
    val credits: Int,

    @Column(nullable = false, precision = 10, scale = 2)
    val price: BigDecimal,

    val currency: String = "CZK",

    @Column(name = "validity_days")
    val validityDays: Int = 30,

    @Column(name = "sessions_count")
    val sessionsCount: Int? = null,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    @Column(name = "sort_order")
    val sortOrder: Int = 0,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
)
