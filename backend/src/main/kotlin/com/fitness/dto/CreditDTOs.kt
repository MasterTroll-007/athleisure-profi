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
    val isActive: Boolean,
    val highlightType: String,
    val isBasic: Boolean,
    val discountPercent: Int? // Discount compared to basic package price per credit
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

// Admin DTOs for Credit Package CRUD
data class AdminCreditPackageDTO(
    val id: String,
    val nameCs: String,
    val nameEn: String?,
    val description: String?,
    val credits: Int,
    val priceCzk: BigDecimal,
    val currency: String?,
    val isActive: Boolean,
    val sortOrder: Int,
    val highlightType: String,
    val isBasic: Boolean,
    val discountPercent: Int?, // Calculated discount compared to basic package
    val createdAt: String
)

data class CreateCreditPackageRequest(
    @field:NotBlank(message = "Czech name is required")
    val nameCs: String,

    val nameEn: String? = null,

    val description: String? = null,

    @field:Min(1, message = "Credits must be at least 1")
    val credits: Int,

    @field:DecimalMin("0.01", message = "Price must be positive")
    val priceCzk: BigDecimal,

    val currency: String? = "CZK",

    val isActive: Boolean = true,

    val sortOrder: Int = 0,

    val highlightType: String = "NONE",

    val isBasic: Boolean = false
)

data class UpdateCreditPackageRequest(
    val nameCs: String? = null,
    val nameEn: String? = null,
    val description: String? = null,
    val credits: Int? = null,
    val priceCzk: BigDecimal? = null,
    val currency: String? = null,
    val isActive: Boolean? = null,
    val sortOrder: Int? = null,
    val highlightType: String? = null,
    val isBasic: Boolean? = null
)

// Admin DTOs for Pricing Item CRUD
data class AdminPricingItemDTO(
    val id: String,
    val nameCs: String,
    val nameEn: String?,
    val descriptionCs: String?,
    val descriptionEn: String?,
    val credits: Int,
    val durationMinutes: Int?,
    val isActive: Boolean,
    val sortOrder: Int,
    val createdAt: String
)

data class CreatePricingItemRequest(
    @field:NotBlank(message = "Czech name is required")
    val nameCs: String,

    val nameEn: String? = null,

    val descriptionCs: String? = null,

    val descriptionEn: String? = null,

    @field:Min(1, message = "Credits must be at least 1")
    val credits: Int,

    val durationMinutes: Int? = 60,

    val isActive: Boolean = true,

    val sortOrder: Int = 0
)

data class UpdatePricingItemRequest(
    val nameCs: String? = null,
    val nameEn: String? = null,
    val descriptionCs: String? = null,
    val descriptionEn: String? = null,
    val credits: Int? = null,
    val durationMinutes: Int? = null,
    val isActive: Boolean? = null,
    val sortOrder: Int? = null
)
