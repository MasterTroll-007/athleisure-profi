package com.fitness.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "stripe_payments")
data class StripePayment(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id")
    val userId: UUID? = null,

    @Column(name = "stripe_session_id", unique = true)
    val stripeSessionId: String,

    @Column(name = "stripe_payment_intent_id")
    val stripePaymentIntentId: String? = null,

    @Column(nullable = false, precision = 10, scale = 2)
    val amount: BigDecimal,

    val currency: String = "CZK",

    @Column(nullable = false)
    val status: String,  // pending, completed, expired, refunded

    @Column(name = "credit_package_id")
    val creditPackageId: UUID? = null,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
