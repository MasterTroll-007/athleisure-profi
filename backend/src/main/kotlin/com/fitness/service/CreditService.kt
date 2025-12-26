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
    private val gopayPaymentRepository: GopayPaymentRepository
) {

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

        val creditPackage = creditPackageRepository.findById(packageUUID)
            .orElseThrow { NoSuchElementException("Credit package not found") }

        val totalCredits = creditPackage.credits + creditPackage.bonusCredits

        // Create payment record (simulated - in production would integrate with GoPay)
        val payment = gopayPaymentRepository.save(
            GopayPayment(
                userId = userUUID,
                amount = creditPackage.priceCzk,
                currency = creditPackage.currency ?: "CZK",
                state = "PAID",
                status = "completed",
                paymentType = "credit_purchase",
                creditPackageId = packageUUID
            )
        )

        // Add credits to user
        userRepository.updateCredits(userUUID, totalCredits)

        // Record transaction
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
            status = "completed",
            credits = totalCredits,
            newBalance = user.credits
        )
    }
}
