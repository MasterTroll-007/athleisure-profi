package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "pricing_items")
data class PricingItem(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val name: String,

    val description: String? = null,

    @Column(nullable = false)
    val credits: Int,

    @Column(name = "duration_minutes")
    val durationMinutes: Int = 60,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    @Column(name = "sort_order")
    val sortOrder: Int = 0,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
)
