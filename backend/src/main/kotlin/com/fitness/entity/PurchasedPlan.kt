package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "purchased_plans")
data class PurchasedPlan(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "plan_id", nullable = false)
    val planId: UUID,

    @Column(name = "purchase_date", nullable = false)
    val purchaseDate: LocalDate = LocalDate.now(),

    @Column(name = "expiry_date", nullable = false)
    val expiryDate: LocalDate,

    @Column(name = "sessions_remaining")
    val sessionsRemaining: Int? = null,

    @Column(name = "gopay_payment_id")
    val gopayPaymentId: String? = null,

    val status: String = "active",

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
)
