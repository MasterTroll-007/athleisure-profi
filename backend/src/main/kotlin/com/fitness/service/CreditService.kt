package com.fitness.service

import com.fitness.dto.*
import com.fitness.entity.CreditTransaction
import com.fitness.entity.GopayPayment
import com.fitness.entity.TransactionType
import com.fitness.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

@Service
class CreditService(
    private val userRepository: UserRepository,
    private val creditPackageRepository: CreditPackageRepository,
    private val creditTransactionRepository: CreditTransactionRepository,
    private val pricingItemRepository: PricingItemRepository,
    private val gopayPaymentRepository: GopayPaymentRepository,
    private val gopayApiClient: GopayApiClient
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

    fun getPackages(): List<CreditPackageDTO> {
        return creditPackageRepository.findByIsActiveTrueOrderBySortOrder()
            .map { pkg ->
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
        val orderNumber = "CR-${System.currentTimeMillis()}"

        // Check if GoPay is configured
        if (!gopayApiClient.isConfigured()) {
            logger.warn("GoPay not configured, using simulation mode")
            return simulatePurchase(userUUID, creditPackage, totalCredits)
        }

        // Create payment via GoPay API
        return try {
            val gopayResponse = gopayApiClient.createPayment(
                CreatePaymentRequest(
                    email = user.email,
                    amountInCents = (creditPackage.priceCzk.toLong() * 100),  // Convert to halere
                    currency = creditPackage.currency ?: "CZK",
                    orderNumber = orderNumber,
                    description = "Kredity: ${creditPackage.nameCs}"
                )
            )

            // Save payment record with CREATED state (not PAID yet)
            val payment = gopayPaymentRepository.save(
                GopayPayment(
                    userId = userUUID,
                    gopayId = gopayResponse.gopayId,
                    amount = creditPackage.priceCzk,
                    currency = creditPackage.currency ?: "CZK",
                    state = gopayResponse.state,
                    status = "pending",
                    paymentType = "credit_purchase",
                    creditPackageId = packageUUID,
                    orderNumber = orderNumber
                )
            )

            logger.info("GoPay payment initiated: paymentId=${payment.id}, gopayId=${gopayResponse.gopayId}")

            PurchaseCreditsResponse(
                paymentId = payment.id.toString(),
                gwUrl = gopayResponse.gatewayUrl,  // Frontend redirects to this URL
                status = "pending",
                credits = totalCredits,
                newBalance = user.credits  // Not updated yet, will be updated via webhook
            )
        } catch (e: Exception) {
            logger.error("Failed to create GoPay payment: ${e.message}", e)
            throw RuntimeException("Platba se nezdařila. Zkuste to prosím znovu.")
        }
    }

    /**
     * Simulation mode for testing without GoPay credentials.
     */
    private fun simulatePurchase(
        userUUID: UUID,
        creditPackage: com.fitness.entity.CreditPackage,
        totalCredits: Int
    ): PurchaseCreditsResponse {
        val payment = gopayPaymentRepository.save(
            GopayPayment(
                userId = userUUID,
                amount = creditPackage.priceCzk,
                currency = creditPackage.currency ?: "CZK",
                state = "PAID",
                status = "completed",
                paymentType = "credit_purchase",
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
                gopayPaymentId = payment.id.toString(),
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
