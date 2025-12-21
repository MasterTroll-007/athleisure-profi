package com.fitness.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.math.BigDecimal
import java.time.Instant

object CreditPackages : UUIDTable("credit_packages") {
    val nameCs = varchar("name_cs", 255)
    val nameEn = varchar("name_en", 255).nullable()
    val credits = integer("credits")
    val bonusCredits = integer("bonus_credits").default(0)
    val priceCzk = decimal("price_czk", 10, 2)
    val isActive = bool("is_active").default(true)
    val sortOrder = integer("sort_order").default(0)
    val createdAt = timestamp("created_at").default(Instant.now())
}

@Serializable
data class CreditPackageDTO(
    val id: String,
    val nameCs: String,
    val nameEn: String?,
    val credits: Int,
    val bonusCredits: Int,
    val priceCzk: Double,
    val isActive: Boolean,
    val sortOrder: Int
)

@Serializable
data class CreateCreditPackageRequest(
    val nameCs: String,
    val nameEn: String? = null,
    val credits: Int,
    val bonusCredits: Int = 0,
    val priceCzk: Double,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

@Serializable
data class UpdateCreditPackageRequest(
    val nameCs: String? = null,
    val nameEn: String? = null,
    val credits: Int? = null,
    val bonusCredits: Int? = null,
    val priceCzk: Double? = null,
    val isActive: Boolean? = null,
    val sortOrder: Int? = null
)
