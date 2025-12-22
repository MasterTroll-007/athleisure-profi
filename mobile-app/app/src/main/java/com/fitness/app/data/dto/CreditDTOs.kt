package com.fitness.app.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreditPackageDTO(
    val id: String,
    val nameCs: String,
    val nameEn: String? = null,
    val credits: Int,
    val bonusCredits: Int = 0,
    val priceCzk: Double,
    val isActive: Boolean,
    val sortOrder: Int
)

@Serializable
data class CreditTransactionDTO(
    val id: String,
    val userId: String? = null,
    val amount: Int,
    val type: String,
    val referenceId: String? = null,
    val note: String? = null,
    val createdAt: String
)

@Serializable
data class PurchaseCreditsRequest(
    val packageId: String
)

@Serializable
data class PurchaseCreditsResponse(
    val paymentId: String,
    val status: String,
    val gwUrl: String? = null
)

@Serializable
data class CreditBalanceResponse(
    val credits: Int
)

@Serializable
data class AdjustCreditsRequest(
    val userId: String,
    val amount: Int,
    val note: String? = null
)

@Serializable
data class PricingItemDTO(
    val id: String,
    val nameCs: String,
    val nameEn: String? = null,
    val descriptionCs: String? = null,
    val descriptionEn: String? = null,
    val credits: Int,
    val isActive: Boolean,
    val sortOrder: Int
)
