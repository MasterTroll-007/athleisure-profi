package com.fitness.app.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreditPackageDTO(
    val id: String,
    val name: String,
    val description: String? = null,
    val credits: Int,
    val priceCzk: Double,
    val currency: String = "CZK",
    val isActive: Boolean = true
) {
    val price: Double get() = priceCzk
    val originalPrice: Double? get() = null
}

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
) {
    val paymentUrl: String? get() = gwUrl
}

@Serializable
data class CreditBalanceResponse(
    val balance: Int,
    val userId: String? = null
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
    val name: String,
    val description: String? = null,
    val credits: Int,
    val isActive: Boolean = true
)

// Admin Credit Package DTOs
@Serializable
data class AdminCreditPackageDTO(
    val id: String,
    val nameCs: String,
    val nameEn: String? = null,
    val description: String? = null,
    val credits: Int,
    val bonusCredits: Int = 0,
    val priceCzk: Double,
    val currency: String? = "CZK",
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: String? = null
) {
    val name: String get() = nameCs
    val totalCredits: Int get() = credits + bonusCredits
}

@Serializable
data class CreateCreditPackageRequest(
    val nameCs: String,
    val nameEn: String? = null,
    val description: String? = null,
    val credits: Int,
    val bonusCredits: Int = 0,
    val priceCzk: Double,
    val currency: String? = "CZK",
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

@Serializable
data class UpdateCreditPackageRequest(
    val nameCs: String? = null,
    val nameEn: String? = null,
    val description: String? = null,
    val credits: Int? = null,
    val bonusCredits: Int? = null,
    val priceCzk: Double? = null,
    val currency: String? = null,
    val isActive: Boolean? = null,
    val sortOrder: Int? = null
)

