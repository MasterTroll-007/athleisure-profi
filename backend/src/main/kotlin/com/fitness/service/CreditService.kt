package com.fitness.service

import com.fitness.dto.*
import com.fitness.entity.CreditTransaction
import com.fitness.entity.StripePayment
import com.fitness.entity.TransactionType
import com.fitness.repository.*
import org.springframework.cache.annotation.Cacheable
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
    private val stripeService: StripeService
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

        return packages.map { pkg ->
            CreditPackageDTO(
                id = pkg.id.toString(),
                name = pkg.nameCs,
                description = pkg.description,
                credits = pkg.credits + pkg.bonusCredits,
                priceCzk = pkg.priceCzk,
                currency = pkg.currency ?: "CZK",
                isActive = pkg.isActive
            )
        }
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
                    createdAt = tx.createdAt.toString()
                )
            }
    }

    @Cacheable("pricingItems")
    fun getPricingItems(): List<PricingItemDTO> {
        return pricingItemRepository.findByIsActiveTrueOrderBySortOrder()
            .map { item ->
                PricingItemDTO(
                    id = item.id.toString(),
                    nameCs = item.nameCs,
                    nameEn = item.nameEn,
                    descriptionCs = item.descriptionCs,
                    descriptionEn = item.descriptionEn,
                    credits = item.credits,
                    isActive = item.isActive,
                    sortOrder = item.sortOrder
                )
            }
    }

    @Transactional
    fun adjustCredits(adminId: String, request: AdminAdjustCreditsRequest): CreditBalanceResponse {
        val userId = UUID.fromString(request.userId)

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

        val totalCredits = creditPackage.credits + creditPackage.bonusCredits

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
