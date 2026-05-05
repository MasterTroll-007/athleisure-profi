package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "stripe_balance_transactions",
    indexes = [
        Index(name = "idx_stripe_balance_tx_created", columnList = "created_at_stripe"),
        Index(name = "idx_stripe_balance_tx_source", columnList = "stripe_source_id"),
        Index(name = "idx_stripe_balance_tx_payment_intent", columnList = "stripe_payment_intent_id"),
        Index(name = "idx_stripe_balance_tx_payout", columnList = "stripe_payout_id")
    ]
)
data class StripeBalanceTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "stripe_balance_transaction_id", nullable = false, unique = true)
    val stripeBalanceTransactionId: String,

    @Column(name = "stripe_source_id")
    val stripeSourceId: String? = null,

    @Column(name = "stripe_charge_id")
    val stripeChargeId: String? = null,

    @Column(name = "stripe_payment_intent_id")
    val stripePaymentIntentId: String? = null,

    @Column(name = "stripe_payout_id")
    val stripePayoutId: String? = null,

    @Column(nullable = false)
    val type: String,

    @Column(name = "reporting_category")
    val reportingCategory: String? = null,

    val description: String? = null,

    @Column(nullable = false)
    val currency: String,

    @Column(name = "amount_cents", nullable = false)
    val amountCents: Long,

    @Column(name = "fee_cents", nullable = false)
    val feeCents: Long,

    @Column(name = "net_cents", nullable = false)
    val netCents: Long,

    val status: String? = null,

    @Column(name = "created_at_stripe", nullable = false)
    val createdAtStripe: Instant,

    @Column(name = "available_on")
    val availableOn: Instant? = null,

    @Column(name = "synced_at", nullable = false)
    val syncedAt: Instant = Instant.now()
)
