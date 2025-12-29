package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "verification_tokens",
    indexes = [
        Index(name = "idx_verification_token_user", columnList = "user_id"),
        Index(name = "idx_verification_token_expires", columnList = "expires_at")
    ]
)
data class VerificationToken(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(nullable = false, unique = true)
    val token: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
)
