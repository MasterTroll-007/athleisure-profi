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

    @Column(name = "user_id")
    val userId: UUID? = null,

    @Column(name = "gopay_id")
    val gopayId: String? = null,

    @Column(nullable = false, precision = 10, scale = 2)
    val amount: BigDecimal,

    val currency: String = "CZK",

    @Column(nullable = false)
    val state: String,

    @Column(nullable = false)
    val status: String,

    @Column(name = "payment_type", nullable = false)
    val paymentType: String,

    @Column(name = "credit_package_id")
    val creditPackageId: UUID? = null,

    @Column(name = "reference_id")
    val referenceId: UUID? = null,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
