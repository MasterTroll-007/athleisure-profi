package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "credit_transactions",
    indexes = [
        Index(name = "idx_credit_tx_user_created", columnList = "user_id, created_at DESC")
    ]
)
data class CreditTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val amount: Int,

    @Column(nullable = false)
    val type: String,

    @Column(name = "reference_id")
    val referenceId: UUID? = null,

    @Column(name = "gopay_payment_id")
    val gopayPaymentId: String? = null,

    @Column(name = "stripe_payment_id")
    val stripePaymentId: String? = null,

    val note: String? = null,

    @Column(name = "expires_at")
    val expiresAt: Instant? = null,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
)

enum class TransactionType(val value: String) {
    PURCHASE("purchase"),
    RESERVATION("reservation"),
    PLAN_PURCHASE("plan_purchase"),
    ADMIN_ADJUSTMENT("admin_adjustment"),
    REFUND("refund"),
    EXPIRATION("expiration")
}
