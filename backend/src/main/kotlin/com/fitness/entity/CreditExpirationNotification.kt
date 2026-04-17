package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "credit_expiration_notifications",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["transaction_id", "days_before"])
    ]
)
data class CreditExpirationNotification(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "transaction_id", nullable = false)
    val transactionId: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "days_before", nullable = false)
    val daysBefore: Int,

    @Column(name = "sent_at", nullable = false)
    val sentAt: Instant = Instant.now()
)
