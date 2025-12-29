package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "client_notes",
    indexes = [
        Index(name = "idx_client_note_client", columnList = "client_id"),
        Index(name = "idx_client_note_admin", columnList = "admin_id")
    ]
)
data class ClientNote(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "client_id", nullable = false)
    val clientId: UUID,

    @Column(name = "admin_id", nullable = false)
    val adminId: UUID,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
