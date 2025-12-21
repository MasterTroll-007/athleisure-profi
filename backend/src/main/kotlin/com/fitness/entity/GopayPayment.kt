package com.fitness.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "gopay_payments")
data class GopayPayment(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "gopay_id", nullable = false, unique = true)
    val gopayId: String,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false, precision = 10, scale = 2)
    val amount: BigDecimal,

    val currency: String = "CZK",

    @Column(nullable = false)
    val status: String,

    @Column(name = "payment_type", nullable = false)
    val paymentType: String,

    @Column(name = "reference_id")
    val referenceId: UUID? = null,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
