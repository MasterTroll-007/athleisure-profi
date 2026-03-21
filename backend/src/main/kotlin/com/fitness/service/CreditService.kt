package com.fitness.service

import com.fitness.dto.*
import com.fitness.entity.CreditTransaction
import com.fitness.entity.StripePayment
import com.fitness.entity.TransactionType
import com.fitness.mapper.CreditPackageMapper
import com.fitness.mapper.PricingItemMapper
import com.fitness.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class CreditService(
    private val userRepository: UserRepository,
    private val creditPackageRepository: CreditPackageRepository,
    private val creditTransactionRepository: CreditTransactionRepository,
    private val pricingItemRepository: PricingItemRepository,
    private val stripePaymentRepository: StripePaymentRepository,
    private val stripeService: StripeService,
    private val creditPackageMapper: CreditPackageMapper,
    private val pricingItemMapper: PricingItemMapper,
    private val auditService: AuditService
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(CreditService::class.java)

    fun getBalance(userId: String): CreditBalanceResponse {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { NoSuchElementException("User not found") }

        return CreditBalanceResponse(
            balance = user.credits,
            userId = userId
        )
    }

    fun getPackages(userId: String): List<CreditPackageDTO> {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { NoSuchElementException("User not found") }

        // Get packages from user's trainer, or all active if no trainer assigned
        val packages = if (user.trainerId != null) {
            creditPackageRepository.findByTrainerIdAndIsActiveTrueOrderBySortOrder(user.trainerId)
        } else {
            creditPackageRepository.findByIsActiveTrueOrderBySortOrder()
        }

        return packages.map { pkg -> creditPackageMapper.toDTO(pkg) }
    }

    fun getTransactions(userId: String): List<CreditTransactionDTO> {
        return creditTransactionRepository.findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId))
            .map { tx ->
                CreditTransactionDTO(
                    id = tx.id.toString(),
                    userId = tx.userId.toString(),
                    amount = tx.amount,
                    type = tx.type,
                    referenceId = tx.referenceId?.toString(),
                    gopayPaymentId = tx.gopayPaymentId,
                    note = tx.note,
                    expiresAt = tx.expiresAt?.toString(),
                    createdAt = tx.createdAt.toString()
                )
            }
    }

    fun sumCreditsSoldInPeriod(from: java.time.Instant, to: java.time.Instant): Long {
        return creditTransactionRepository.sumAmountByTypeAndDateRange("purchase", from, to) ?: 0L
    }

    fun getPricingItems(userId: String): List<PricingItemDTO> {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { NoSuchElementException("User not found") }

        // Get pricing items from user's trainer, or all active if no trainer assigned
        val items = if (user.trainerId != null) {
            pricingItemRepository.findByAdminIdAndIsActiveTrueOrderBySortOrder(user.trainerId!!)
        } else {
            pricingItemRepository.findByIsActiveTrueOrderBySortOrder()
        }

        return pricingItemMapper.toDTOBatch(items)
    }

    @Transactional
    fun adjustCredits(adminId: String, adminEmail: String?, request: AdminAdjustCreditsRequest): CreditBalanceResponse {
        val userId = UUID.fromString(request.userId)
        
        // Get previous balance for audit logging
        val userBefore = userRepository.findById(userId)
            .orElseThrow { NoSuchElementException("User not found") }
        val previousBalance = userBefore.credits

        userRepository.updateCredits(userId, request.amount)

        creditTransactionRepository.save(
            CreditTransaction(
                userId = userId,
                amount = request.amount,
                type = TransactionType.ADMIN_ADJUSTMENT.value,
                note = request.note ?: "Admin adjustment by $adminId"
            )
        )

        val user = userRepository.findById(userId)
            .orElseThrow { NoSuchElementException("User not found") }
        
        // Audit log the credit adjustment
        auditService.logCreditAdjustment(
            adminId = adminId,
            adminEmail = adminEmail,
            targetUserId = request.userId,
            previousBalance = previousBalance,
            adjustment = request.amount,
            newBalance = user.credits,
            reason = request.note
        )

        return CreditBalanceResponse(
            balance = user.credits,
            userId = request.userId
        )
    }

    @Transactional
    fun purchaseCredits(userId: String, packageId: String): PurchaseCreditsResponse {
        val userUUID = UUID.fromString(userId)
        val packageUUID = UUID.fromString(packageId)

        val user = userRepository.findById(userUUID)
            .orElseThrow { NoSuchElementException("User not found") }

        val creditPackage = creditPackageRepository.findById(packageUUID)
            .orElseThrow { NoSuchElementException("Credit package not found") }

        val totalCredits = creditPackage.credits

        // Check if Stripe is configured
        if (!stripeService.isConfigured()) {
            logger.warn("Stripe not configured, using simulation mode")
            return simulatePurchase(userUUID, creditPackage, totalCredits)
        }

        // Create Stripe Checkout Session
        return try {
            val checkoutResult = stripeService.createCheckoutSession(
                userId = userUUID,
                packageId = packageUUID,
                userEmail = user.email
            )

            logger.info("Stripe checkout session created: sessionId=${checkoutResult.sessionId}")

            PurchaseCreditsResponse(
                paymentId = checkoutResult.sessionId,
                gwUrl = checkoutResult.checkoutUrl,  // Frontend redirects to Stripe Checkout
                status = "pending",
                credits = totalCredits,
                newBalance = user.credits  // Not updated yet, will be updated via webhook
            )
        } catch (e: Exception) {
            logger.error("Failed to create Stripe checkout: ${e.message}", e)
            throw RuntimeException("Platba se nezdařila. Zkuste to prosím znovu.")
        }
    }

    /**
     * Simulate a successful payment (dev mode only).
     * Validates that the payment belongs to the requesting user.
     */
    @Transactional
    fun simulatePaymentSuccess(paymentIdOrSessionId: String, requestingUserId: String): Map<String, Any> {
        val payment = stripePaymentRepository.findByStripeSessionId(paymentIdOrSessionId)
            ?: try { stripePaymentRepository.findById(UUID.fromString(paymentIdOrSessionId)).orElse(null) } catch (_: Exception) { null }
            ?: throw NoSuchElementException("Payment not found")

        // Ownership check: only the user who created the payment can simulate it
        if (payment.userId.toString() != requestingUserId) {
            throw IllegalArgumentException("Access denied")
        }

        if (payment.status == "completed") {
            return mapOf("status" to "completed" as Any, "message" to "Payment already completed" as Any)
        }

        val updatedPayment = payment.copy(
            status = "completed",
            updatedAt = java.time.Instant.now()
        )
        stripePaymentRepository.save(updatedPayment)

        if (payment.creditPackageId != null && payment.userId != null) {
            val creditPackage = creditPackageRepository.findById(payment.creditPackageId).orElse(null)
            if (creditPackage != null) {
                userRepository.updateCredits(payment.userId, creditPackage.credits)
                creditTransactionRepository.save(
                    CreditTransaction(
                        userId = payment.userId,
                        amount = creditPackage.credits,
                        type = TransactionType.PURCHASE.value,
                        stripePaymentId = payment.stripeSessionId,
                        note = "Nákup (simulace): ${creditPackage.nameCs}"
                    )
                )
            }
        }

        logger.info("Payment simulated as successful: paymentId=$paymentIdOrSessionId, userId=${payment.userId}")
        return mapOf("status" to "completed" as Any, "message" to "Payment simulated successfully" as Any)
    }

    /**
     * Simulation mode for testing without Stripe credentials.
     */
    private fun simulatePurchase(
        userUUID: UUID,
        creditPackage: com.fitness.entity.CreditPackage,
        totalCredits: Int
    ): PurchaseCreditsResponse {
        val payment = stripePaymentRepository.save(
            StripePayment(
                userId = userUUID,
                stripeSessionId = "sim_${System.currentTimeMillis()}",
                amount = creditPackage.priceCzk,
                currency = creditPackage.currency ?: "CZK",
                status = "completed",
                creditPackageId = creditPackage.id
            )
        )

        // Add credits immediately in simulation mode
        userRepository.updateCredits(userUUID, totalCredits)

        creditTransactionRepository.save(
            CreditTransaction(
                userId = userUUID,
                amount = totalCredits,
                type = TransactionType.PURCHASE.value,
                referenceId = payment.id,
                stripePaymentId = payment.stripeSessionId,
                note = "Nákup: ${creditPackage.nameCs}"
            )
        )

        val user = userRepository.findById(userUUID)
            .orElseThrow { NoSuchElementException("User not found") }

        return PurchaseCreditsResponse(
            paymentId = payment.id.toString(),
            gwUrl = null,  // No redirect needed in simulation
            status = "completed",
            credits = totalCredits,
            newBalance = user.credits
        )
    }
}
