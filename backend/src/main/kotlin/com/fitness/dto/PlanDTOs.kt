package com.fitness.dto

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
    val nameCs: String,
    val nameEn: String? = null,
    val descriptionCs: String? = null,
    val descriptionEn: String? = null,
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
