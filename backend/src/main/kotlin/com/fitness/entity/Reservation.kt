package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@Entity
@Table(
    name = "reservations",
    indexes = [
        Index(name = "idx_reservation_user_status", columnList = "user_id, status"),
        Index(name = "idx_reservation_user_date", columnList = "user_id, date"),
        Index(name = "idx_reservation_date_status", columnList = "date, status"),
        Index(name = "idx_reservation_slot", columnList = "slot_id")
    ]
)
data class Reservation(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "block_id")
    val blockId: UUID? = null,

    @Column(name = "slot_id")
    val slotId: UUID? = null,

    @Column(nullable = false)
    val date: LocalDate,

    @Column(name = "start_time", nullable = false)
    val startTime: LocalTime,

    @Column(name = "end_time", nullable = false)
    val endTime: LocalTime,

    val status: String = "confirmed",

    @Column(name = "credits_used")
    val creditsUsed: Int = 1,

    @Column(name = "pricing_item_id")
    val pricingItemId: UUID? = null,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @Column(name = "cancelled_at")
    val cancelledAt: Instant? = null,

    @Column(name = "note")
    val note: String? = null
)
