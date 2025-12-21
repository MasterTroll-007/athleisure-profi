package com.fitness.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object PricingItems : UUIDTable("pricing_items") {
    val nameCs = varchar("name_cs", 255)
    val nameEn = varchar("name_en", 255).nullable()
    val descriptionCs = text("description_cs").nullable()
    val descriptionEn = text("description_en").nullable()
    val credits = integer("credits")
    val isActive = bool("is_active").default(true)
    val sortOrder = integer("sort_order").default(0)
    val createdAt = timestamp("created_at").default(Instant.now())
}

@Serializable
data class PricingItemDTO(
    val id: String,
    val nameCs: String,
    val nameEn: String?,
    val descriptionCs: String?,
    val descriptionEn: String?,
    val credits: Int,
    val isActive: Boolean,
    val sortOrder: Int
)

@Serializable
data class CreatePricingItemRequest(
    val nameCs: String,
    val nameEn: String? = null,
    val descriptionCs: String? = null,
    val descriptionEn: String? = null,
    val credits: Int,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

@Serializable
data class UpdatePricingItemRequest(
    val nameCs: String? = null,
    val nameEn: String? = null,
    val descriptionCs: String? = null,
    val descriptionEn: String? = null,
    val credits: Int? = null,
    val isActive: Boolean? = null,
    val sortOrder: Int? = null
)
