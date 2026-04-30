package com.fitness.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(
    name = "audit_logs",
    indexes = [
        Index(name = "idx_audit_logs_admin_created", columnList = "admin_id, created_at"),
        Index(name = "idx_audit_logs_client_created", columnList = "client_id, created_at"),
        Index(name = "idx_audit_logs_reservation", columnList = "reservation_id"),
        Index(name = "idx_audit_logs_action", columnList = "action")
    ]
)
data class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "admin_id", nullable = false)
    val adminId: UUID,

    @Column(name = "actor_id")
    val actorId: UUID? = null,

    @Column(name = "actor_email")
    val actorEmail: String? = null,

    @Column(name = "actor_name")
    val actorName: String? = null,

    @Column(name = "actor_role", nullable = false)
    val actorRole: String,

    @Column(nullable = false)
    val action: String,

    @Column(name = "target_type", nullable = false)
    val targetType: String,

    @Column(name = "target_id")
    val targetId: UUID? = null,

    @Column(name = "client_id")
    val clientId: UUID? = null,

    @Column(name = "client_email")
    val clientEmail: String? = null,

    @Column(name = "client_name")
    val clientName: String? = null,

    @Column(name = "reservation_id")
    val reservationId: UUID? = null,

    @Column(name = "slot_id")
    val slotId: UUID? = null,

    val date: LocalDate? = null,

    @Column(name = "start_time")
    val startTime: LocalTime? = null,

    @Column(name = "end_time")
    val endTime: LocalTime? = null,

    @Column(name = "credits_change")
    val creditsChange: Int? = null,

    @Column(name = "refund_credits")
    val refundCredits: Boolean? = null,

    @Column(columnDefinition = "TEXT")
    val details: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
