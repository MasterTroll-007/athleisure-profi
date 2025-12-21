package com.fitness.services

import com.fitness.models.*
import com.fitness.repositories.CreditRepository
import com.fitness.repositories.UserRepository
import java.util.*

object CreditService {

    fun getBalance(userId: String): CreditBalanceResponse {
        val balance = UserRepository.getCredits(UUID.fromString(userId))
        return CreditBalanceResponse(
            balance = balance,
            userId = userId
        )
    }

    fun getPackages(): List<CreditPackageDTO> {
        return CreditRepository.findActivePackages()
    }

    fun getAllPackages(): List<CreditPackageDTO> {
        return CreditRepository.findAllPackages()
    }

    fun getTransactionHistory(userId: String, limit: Int = 50): List<CreditTransactionDTO> {
        return CreditRepository.findTransactionsByUser(UUID.fromString(userId), limit)
    }

    fun getPricingItems(): List<PricingItemDTO> {
        return CreditRepository.findActivePricingItems()
    }

    fun getAllPricingItems(): List<PricingItemDTO> {
        return CreditRepository.findAllPricingItems()
    }

    // Admin functions
    fun adjustCredits(adminId: String, request: AdminAdjustCreditsRequest): CreditBalanceResponse {
        val userUUID = UUID.fromString(request.userId)

        // Verify user exists
        UserRepository.findById(userUUID)
            ?: throw NoSuchElementException("User not found")

        // Update credits
        UserRepository.updateCredits(userUUID, request.amount)

        // Record transaction
        CreditRepository.createTransaction(
            userId = userUUID,
            amount = request.amount,
            type = TransactionType.ADMIN_ADJUSTMENT.value,
            note = request.note ?: "Adjusted by admin"
        )

        val newBalance = UserRepository.getCredits(userUUID)
        return CreditBalanceResponse(
            balance = newBalance,
            userId = request.userId
        )
    }

    fun createPackage(request: CreateCreditPackageRequest): CreditPackageDTO {
        return CreditRepository.createPackage(request)
    }

    fun updatePackage(id: String, request: UpdateCreditPackageRequest): CreditPackageDTO {
        return CreditRepository.updatePackage(UUID.fromString(id), request)
            ?: throw NoSuchElementException("Package not found")
    }

    fun deletePackage(id: String): Boolean {
        return CreditRepository.deletePackage(UUID.fromString(id))
    }

    fun createPricingItem(request: CreatePricingItemRequest): PricingItemDTO {
        return CreditRepository.createPricingItem(request)
    }

    fun updatePricingItem(id: String, request: UpdatePricingItemRequest): PricingItemDTO {
        return CreditRepository.updatePricingItem(UUID.fromString(id), request)
            ?: throw NoSuchElementException("Pricing item not found")
    }

    fun deletePricingItem(id: String): Boolean {
        return CreditRepository.deletePricingItem(UUID.fromString(id))
    }

    fun getAllTransactions(limit: Int = 100): List<CreditTransactionDTO> {
        return CreditRepository.findAllTransactions(limit)
    }

    /**
     * Add credits after successful payment
     * Called by GoPay webhook handler
     */
    fun addCreditsFromPayment(
        userId: UUID,
        packageId: UUID,
        gopayPaymentId: String
    ): CreditBalanceResponse {
        val creditPackage = CreditRepository.findPackageById(packageId)
            ?: throw NoSuchElementException("Package not found")

        val totalCredits = creditPackage.credits + creditPackage.bonusCredits

        // Add credits to user
        UserRepository.updateCredits(userId, totalCredits)

        // Record transaction
        CreditRepository.createTransaction(
            userId = userId,
            amount = totalCredits,
            type = TransactionType.PURCHASE.value,
            referenceId = packageId,
            gopayPaymentId = gopayPaymentId,
            note = "Purchased ${creditPackage.nameCs}"
        )

        val newBalance = UserRepository.getCredits(userId)
        return CreditBalanceResponse(
            balance = newBalance,
            userId = userId.toString()
        )
    }
}
