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
