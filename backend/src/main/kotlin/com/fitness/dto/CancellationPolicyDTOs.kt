package com.fitness.dto

import jakarta.validation.constraints.*

data class CancellationPolicyDTO(
    val id: String,
    val fullRefundHours: Int,
    val partialRefundHours: Int?,
    val partialRefundPercentage: Int?,
    val noRefundHours: Int,
    val isActive: Boolean
)

data class UpdateCancellationPolicyRequest(
    @field:Min(value = 0, message = "Full refund hours must be 0 or greater")
    @field:Max(value = 168, message = "Full refund hours cannot exceed 168 (1 week)")
    val fullRefundHours: Int? = null,

    @field:Min(value = 0, message = "Partial refund hours must be 0 or greater")
    @field:Max(value = 168, message = "Partial refund hours cannot exceed 168 (1 week)")
    val partialRefundHours: Int? = null,

    @field:Min(value = 0, message = "Partial refund percentage must be 0-100")
    @field:Max(value = 100, message = "Partial refund percentage must be 0-100")
    val partialRefundPercentage: Int? = null,

    @field:Min(value = 0, message = "No refund hours must be 0 or greater")
    @field:Max(value = 168, message = "No refund hours cannot exceed 168 (1 week)")
    val noRefundHours: Int? = null,

    val isActive: Boolean? = null
)

data class CancellationRefundPreviewDTO(
    val reservationId: String,
    val creditsUsed: Int,
    val refundPercentage: Int,
    val refundAmount: Int,
    val hoursUntilReservation: Double,
    val policyApplied: String
)

data class CancellationResultDTO(
    val reservation: ReservationDTO,
    val refundAmount: Int,
    val refundPercentage: Int,
    val policyApplied: String
)
