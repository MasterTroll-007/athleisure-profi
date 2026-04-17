package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@Entity
@Table(
    name = "recurring_reservations",
    indexes = [
        Index(name = "idx_recurring_user", columnList = "user_id"),
        Index(name = "idx_recurring_status", columnList = "status")
    ]
)
data class RecurringReservation(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "day_of_week", nullable = false)
    val dayOfWeek: Int,

    @Column(name = "start_time", nullable = false)
    val startTime: LocalTime,

    @Column(name = "end_time", nullable = false)
    val endTime: LocalTime,

    @Column(name = "weeks_count", nullable = false)
    val weeksCount: Int,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    val endDate: LocalDate,

    @Column(nullable = false)
    val status: String = "active",

    @Column(name = "pricing_item_id")
    val pricingItemId: UUID? = null,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
)
