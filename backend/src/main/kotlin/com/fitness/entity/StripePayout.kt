package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "stripe_payouts",
    indexes = [
        Index(name = "idx_stripe_payout_created", columnList = "created_at_stripe"),
        Index(name = "idx_stripe_payout_arrival", columnList = "arrival_date"),
        Index(name = "idx_stripe_payout_status", columnList = "status")
    ]
)
data class StripePayout(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "stripe_payout_id", nullable = false, unique = true)
    val stripePayoutId: String,

    @Column(name = "stripe_balance_transaction_id")
    val stripeBalanceTransactionId: String? = null,

    @Column(name = "amount_cents", nullable = false)
    val amountCents: Long,

    @Column(nullable = false)
    val currency: String,

    @Column(nullable = false)
    val status: String,

    @Column(name = "created_at_stripe", nullable = false)
    val createdAtStripe: Instant,

    @Column(name = "arrival_date")
    val arrivalDate: Instant? = null,

    val method: String? = null,

    val type: String? = null,

    val description: String? = null,

    @Column(name = "statement_descriptor")
    val statementDescriptor: String? = null,

    @Column(name = "failure_code")
    val failureCode: String? = null,

    @Column(name = "failure_message")
    val failureMessage: String? = null,

    @Column(name = "synced_at", nullable = false)
    val syncedAt: Instant = Instant.now()
)
