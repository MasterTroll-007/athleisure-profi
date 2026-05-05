package com.fitness.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "stripe_payments",
    indexes = [
        Index(name = "idx_stripe_payment_user", columnList = "user_id"),
        Index(name = "idx_stripe_payment_intent", columnList = "stripe_payment_intent_id"),
        Index(name = "idx_stripe_payment_charge", columnList = "stripe_charge_id"),
        Index(name = "idx_stripe_payment_balance_tx", columnList = "stripe_balance_transaction_id"),
        Index(name = "idx_stripe_payment_status", columnList = "status")
    ]
)
data class StripePayment(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "user_id")
    val userId: UUID? = null,

    @Column(name = "stripe_session_id", unique = true)
    val stripeSessionId: String,

    @Column(name = "stripe_payment_intent_id")
    val stripePaymentIntentId: String? = null,

    @Column(name = "stripe_charge_id")
    val stripeChargeId: String? = null,

    @Column(name = "stripe_balance_transaction_id")
    val stripeBalanceTransactionId: String? = null,

    @Column(name = "stripe_payout_id")
    val stripePayoutId: String? = null,

    @Column(nullable = false, precision = 10, scale = 2)
    val amount: BigDecimal,

    @Column(name = "stripe_fee_amount", precision = 10, scale = 2)
    val stripeFeeAmount: BigDecimal? = null,

    @Column(name = "stripe_net_amount", precision = 10, scale = 2)
    val stripeNetAmount: BigDecimal? = null,

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
