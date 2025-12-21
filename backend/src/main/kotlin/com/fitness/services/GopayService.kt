package com.fitness.services

import com.fitness.models.*
import com.fitness.repositories.CreditRepository
import com.fitness.repositories.GopayRepository
import java.util.*

/**
 * GoPay Service - Prepared for future integration
 *
 * NOTE: Actual GoPay API integration is NOT implemented yet.
 * This service provides the structure and mock implementations
 * that will be replaced with real GoPay SDK calls later.
 */
object GopayService {

    // GoPay configuration (to be used later)
    private val clientId = System.getenv("GOPAY_CLIENT_ID") ?: ""
    private val clientSecret = System.getenv("GOPAY_CLIENT_SECRET") ?: ""
    private val goId = System.getenv("GOPAY_GOID") ?: ""
    private val isProduction = System.getenv("GOPAY_IS_PRODUCTION")?.toBoolean() ?: false

    /**
     * Create a payment for purchasing credits
     *
     * TODO: Implement actual GoPay API call
     * For now, this creates a payment record and returns a mock URL
     */
    fun createPayment(userId: String, packageId: String): PaymentResponse {
        val userUUID = UUID.fromString(userId)
        val packageUUID = UUID.fromString(packageId)

        // Get package details
        val creditPackage = CreditRepository.findPackageById(packageUUID)
            ?: throw NoSuchElementException("Package not found")

        if (!creditPackage.isActive) {
            throw IllegalStateException("This package is not available")
        }

        // Create payment record
        val payment = GopayRepository.create(
            userId = userUUID,
            amount = creditPackage.priceCzk,
            state = GopayState.CREATED.value,
            creditPackageId = packageUUID
        )

        // TODO: Replace with actual GoPay API call
        // val gopayResponse = gopayClient.createPayment(...)
        // GopayRepository.updateState(payment.id, GopayState.CREATED.value, gopayResponse.id)

        // For now, return mock response
        // In production, gwUrl would be the GoPay payment gateway URL
        return PaymentResponse(
            paymentId = payment.id,
            gwUrl = null // Will be GoPay gateway URL after integration
        )
    }

    /**
     * Handle GoPay webhook notification
     *
     * TODO: Implement actual webhook verification and handling
     */
    fun handleNotification(gopayId: Long, state: String): Boolean {
        // TODO: Verify webhook signature

        // Find payment by GoPay ID
        val payment = GopayRepository.findByGopayId(gopayId)
            ?: return false

        // Update payment state
        GopayRepository.updateState(UUID.fromString(payment.id), state)

        // If payment is successful, add credits
        if (state == GopayState.PAID.value && payment.creditPackageId != null) {
            CreditService.addCreditsFromPayment(
                userId = UUID.fromString(payment.userId),
                packageId = UUID.fromString(payment.creditPackageId),
                gopayPaymentId = gopayId.toString()
            )
        }

        return true
    }

    /**
     * Get payment status from GoPay
     *
     * TODO: Implement actual GoPay API call
     */
    fun getPaymentStatus(paymentId: String): GopayPaymentDTO {
        return GopayRepository.findById(UUID.fromString(paymentId))
            ?: throw NoSuchElementException("Payment not found")
    }

    /**
     * Simulate a successful payment (FOR TESTING ONLY)
     * This allows testing the credit flow without actual GoPay integration
     */
    fun simulateSuccessfulPayment(paymentId: String): GopayPaymentDTO {
        val paymentUUID = UUID.fromString(paymentId)

        val payment = GopayRepository.findById(paymentUUID)
            ?: throw NoSuchElementException("Payment not found")

        if (payment.state != GopayState.CREATED.value) {
            throw IllegalStateException("Payment is not in CREATED state")
        }

        // Update to PAID
        GopayRepository.updateState(paymentUUID, GopayState.PAID.value, 123456789L)

        // Add credits
        if (payment.creditPackageId != null) {
            CreditService.addCreditsFromPayment(
                userId = UUID.fromString(payment.userId),
                packageId = UUID.fromString(payment.creditPackageId),
                gopayPaymentId = "SIMULATED_${paymentId}"
            )
        }

        return GopayRepository.findById(paymentUUID)!!
    }

    // Admin functions
    fun getAllPayments(limit: Int = 100): List<GopayPaymentDTO> {
        return GopayRepository.findAll(limit)
    }

    fun getPaymentsByUser(userId: String): List<GopayPaymentDTO> {
        return GopayRepository.findByUser(UUID.fromString(userId))
    }
}
