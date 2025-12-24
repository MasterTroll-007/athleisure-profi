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

