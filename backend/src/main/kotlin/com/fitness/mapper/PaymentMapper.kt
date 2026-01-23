package com.fitness.mapper

import com.fitness.dto.AdminPaymentDTO
import com.fitness.entity.CreditPackage
import com.fitness.entity.StripePayment
import com.fitness.entity.User
import com.fitness.repository.CreditPackageRepository
import com.fitness.repository.UserRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class PaymentMapper(
    private val userRepository: UserRepository,
    private val creditPackageRepository: CreditPackageRepository
) {
    /**
     * Convert StripePayment entity to AdminPaymentDTO with pre-fetched data.
     */
    fun toAdminDTO(
        payment: StripePayment,
        user: User?,
        creditPackage: CreditPackage?
    ): AdminPaymentDTO {
        return AdminPaymentDTO(
            id = payment.id.toString(),
            userId = payment.userId?.toString(),
            userName = user?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim().ifEmpty { null } },
            gopayId = null,  // Not used for Stripe
            stripeSessionId = payment.stripeSessionId,
            amount = payment.amount,
            currency = payment.currency,
            state = mapStripeStatusToLegacy(payment.status),
            creditPackageId = payment.creditPackageId?.toString(),
            creditPackageName = creditPackage?.nameCs,
            createdAt = payment.createdAt.toString(),
            updatedAt = payment.updatedAt.toString()
        )
    }

    /**
     * Convert StripePayment entity to AdminPaymentDTO, fetching user and package from repositories.
     */
    fun toAdminDTO(payment: StripePayment): AdminPaymentDTO {
        val user = payment.userId?.let { userRepository.findById(it).orElse(null) }
        val creditPackage = payment.creditPackageId?.let { creditPackageRepository.findById(it).orElse(null) }
        return toAdminDTO(payment, user, creditPackage)
    }

    /**
     * Batch convert payments to AdminPaymentDTO, efficiently fetching users and packages.
     */
    fun toAdminDTOBatch(payments: List<StripePayment>): List<AdminPaymentDTO> {
        if (payments.isEmpty()) return emptyList()

        // Batch fetch all users
        val userIds = payments.mapNotNull { it.userId }.distinct()
        val usersMap = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id }
        } else {
            emptyMap()
        }

        // Batch fetch all credit packages
        val packageIds = payments.mapNotNull { it.creditPackageId }.distinct()
        val packagesMap = if (packageIds.isNotEmpty()) {
            creditPackageRepository.findAllById(packageIds).associateBy { it.id }
        } else {
            emptyMap()
        }

        return payments.map { payment ->
            val user = payment.userId?.let { usersMap[it] }
            val creditPackage = payment.creditPackageId?.let { packagesMap[it] }
            toAdminDTO(payment, user, creditPackage)
        }
    }

    /**
     * Map Stripe statuses to GoPay-style statuses for frontend compatibility.
     */
    fun mapStripeStatusToLegacy(stripeStatus: String): String {
        return when (stripeStatus.lowercase()) {
            "completed" -> "PAID"
            "pending" -> "CREATED"
            "expired" -> "TIMEOUTED"
            "refunded" -> "REFUNDED"
            else -> stripeStatus.uppercase()
        }
    }
}
