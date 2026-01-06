package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_user_email", columnList = "email"),
        Index(name = "idx_user_role", columnList = "role"),
        Index(name = "idx_user_trainer", columnList = "trainer_id"),
        Index(name = "idx_user_invite_code", columnList = "invite_code")
    ]
)
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(name = "password_hash", nullable = false)
    val passwordHash: String,

    @Column(name = "first_name")
    val firstName: String? = null,

    @Column(name = "last_name")
    val lastName: String? = null,

    val phone: String? = null,

    val role: String = "client",

    val credits: Int = 0,

    val locale: String = "cs",

    val theme: String = "system",

    @Column(name = "email_verified")
    val emailVerified: Boolean = false,

    @Column(name = "trainer_id")
    val trainerId: UUID? = null,

    @Column(name = "calendar_start_hour")
    val calendarStartHour: Int = 6,

    @Column(name = "calendar_end_hour")
    val calendarEndHour: Int = 22,

    @Column(name = "invite_code", unique = true)
    val inviteCode: String? = null,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
