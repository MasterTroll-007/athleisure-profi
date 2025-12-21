package com.fitness.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object TrainingPlans : UUIDTable("training_plans") {
    val nameCs = varchar("name_cs", 255)
    val nameEn = varchar("name_en", 255).nullable()
    val descriptionCs = text("description_cs").nullable()
    val descriptionEn = text("description_en").nullable()
    val credits = integer("credits")
    val filePath = varchar("file_path", 500).nullable()
    val previewImage = varchar("preview_image", 500).nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").default(Instant.now())
}

@Serializable
data class TrainingPlanDTO(
    val id: String,
    val nameCs: String,
    val nameEn: String?,
    val descriptionCs: String?,
    val descriptionEn: String?,
    val credits: Int,
    val previewImage: String?,
    val hasFile: Boolean,
    val isActive: Boolean,
    val createdAt: String
)

@Serializable
data class CreateTrainingPlanRequest(
    val nameCs: String,
    val nameEn: String? = null,
    val descriptionCs: String? = null,
    val descriptionEn: String? = null,
    val credits: Int,
    val isActive: Boolean = true
)

@Serializable
data class UpdateTrainingPlanRequest(
    val nameCs: String? = null,
    val nameEn: String? = null,
    val descriptionCs: String? = null,
    val descriptionEn: String? = null,
    val credits: Int? = null,
    val isActive: Boolean? = null
)
