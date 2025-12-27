package com.fitness.dto

import jakarta.validation.constraints.*
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
    @field:NotBlank(message = "User ID is required")
    val userId: String,

    @field:NotNull(message = "Amount is required")
    val amount: Int,

    @field:Size(max = 500, message = "Note too long")
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

data class PurchaseCreditsRequest(
    @field:NotBlank(message = "Package ID is required")
    val packageId: String
)

data class PurchaseCreditsResponse(
    val paymentId: String,
    val gwUrl: String? = null,  // GoPay gateway URL for redirect
    val status: String,
    val credits: Int,
    val newBalance: Int
)

data class AdminPaymentDTO(
    val id: String,
    val userId: String?,
    val userName: String?,
    val gopayId: Long? = null,  // Kept for backward compatibility, always null for Stripe
    val stripeSessionId: String?,
    val amount: BigDecimal,
    val currency: String,
    val state: String,  // Maps Stripe status to frontend-compatible states
    val creditPackageId: String?,
    val creditPackageName: String?,
    val createdAt: String,
    val updatedAt: String
)
