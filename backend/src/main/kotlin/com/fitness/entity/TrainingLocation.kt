package com.fitness.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "training_locations",
    indexes = [
        Index(name = "idx_training_location_admin", columnList = "admin_id"),
        Index(name = "idx_training_location_active", columnList = "is_active")
    ]
)
data class TrainingLocation(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "name_cs", nullable = false)
    val nameCs: String,

    @Column(name = "name_en")
    val nameEn: String? = null,

    @Column(name = "address_cs", columnDefinition = "TEXT")
    val addressCs: String? = null,

    @Column(name = "address_en", columnDefinition = "TEXT")
    val addressEn: String? = null,

    @Column(nullable = false, length = 7)
    val color: String,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    @Column(name = "admin_id")
    val adminId: UUID? = null,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
)
