package com.fitness.dto

import jakarta.validation.constraints.*
import java.math.BigDecimal

data class TrainingPlanDTO(
    val id: String,
    val name: String,
    val description: String?,
    val credits: Int,
    val price: BigDecimal,
    val currency: String,
    val validityDays: Int,
    val sessionsCount: Int?,
    val isActive: Boolean
)

data class PurchasedPlanDTO(
    val id: String,
    val userId: String,
    val planId: String,
    val planName: String?,
    val purchaseDate: String,
    val expiryDate: String,
    val sessionsRemaining: Int?,
    val status: String
)

data class CreateTrainingPlanRequest(
    @field:NotBlank(message = "Czech name is required")
    @field:Size(max = 200, message = "Name too long")
    val nameCs: String,

    @field:Size(max = 200, message = "Name too long")
    val nameEn: String? = null,

    @field:Size(max = 2000, message = "Description too long")
    val descriptionCs: String? = null,

    @field:Size(max = 2000, message = "Description too long")
    val descriptionEn: String? = null,

    @field:Min(value = 1, message = "Credits must be at least 1")
    @field:Max(value = 1000, message = "Credits cannot exceed 1000")
    val credits: Int = 5,

    val isActive: Boolean = true
)

data class UpdateTrainingPlanRequest(
    val nameCs: String? = null,
    val nameEn: String? = null,
    val descriptionCs: String? = null,
    val descriptionEn: String? = null,
    val credits: Int? = null,
    val isActive: Boolean? = null
)

data class AdminTrainingPlanDTO(
    val id: String,
    val nameCs: String,
    val nameEn: String?,
    val descriptionCs: String?,
    val descriptionEn: String?,
    val credits: Int,
    val isActive: Boolean,
    val createdAt: String
)
