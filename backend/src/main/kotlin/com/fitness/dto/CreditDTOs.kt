package com.fitness.dto

import java.math.BigDecimal

data class CreditPackageDTO(
    val id: String,
    val name: String,
    val description: String?,
    val credits: Int,
    val priceCzk: BigDecimal,
    val currency: String,
    val isActive: Boolean
)

data class CreditTransactionDTO(
    val id: String,
    val userId: String,
    val amount: Int,
    val type: String,
    val referenceId: String?,
    val gopayPaymentId: String?,
    val note: String?,
    val createdAt: String
)

data class CreditBalanceResponse(
    val balance: Int,
    val userId: String
)

data class AdminAdjustCreditsRequest(
    val userId: String,
    val amount: Int,
    val note: String? = null
)

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
