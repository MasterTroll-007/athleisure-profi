package com.fitness.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "training_plans")
data class TrainingPlan(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val name: String = "",

    @Column(name = "name_cs", nullable = false)
    val nameCs: String,

    @Column(name = "name_en")
    val nameEn: String? = null,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "description_cs", columnDefinition = "TEXT")
    val descriptionCs: String? = null,

    @Column(name = "description_en", columnDefinition = "TEXT")
    val descriptionEn: String? = null,

    @Column(nullable = false)
    val credits: Int,

    @Column(name = "file_path")
    val filePath: String? = null,

    @Column(name = "preview_image")
    val previewImage: String? = null,

    @Column(nullable = false, precision = 10, scale = 2)
    val price: BigDecimal,

    val currency: String = "CZK",

    @Column(name = "validity_days")
    val validityDays: Int = 30,

    @Column(name = "sessions_count")
    val sessionsCount: Int? = null,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    @Column(name = "sort_order")
    val sortOrder: Int = 0,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
)
