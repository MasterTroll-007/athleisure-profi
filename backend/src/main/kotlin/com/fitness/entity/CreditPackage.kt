package com.fitness.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "credit_packages",
    indexes = [
        Index(name = "idx_credit_package_trainer", columnList = "trainer_id")
    ]
)
data class CreditPackage(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "trainer_id")
    val trainerId: UUID? = null,

    @Column(name = "name_cs", nullable = false)
    val nameCs: String,

    @Column(name = "name_en")
    val nameEn: String? = null,

    val description: String? = null,

    @Column(nullable = false)
    val credits: Int,

    @Column(name = "bonus_credits")
    val bonusCredits: Int = 0,

    @Column(name = "price_czk", nullable = false, precision = 10, scale = 2)
    val priceCzk: BigDecimal,

    val currency: String? = null,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    @Column(name = "sort_order")
    val sortOrder: Int = 0,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
)
