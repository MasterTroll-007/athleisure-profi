package com.fitness.service

import com.fitness.entity.CreditTransaction
import com.fitness.entity.GopayPayment
import com.fitness.entity.TransactionType
import com.fitness.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class GopayService(
    private val gopayPaymentRepository: GopayPaymentRepository,
    private val creditPackageRepository: CreditPackageRepository,
    private val userRepository: UserRepository,
    private val creditTransactionRepository: CreditTransactionRepository
) {

    @Transactional
    fun handleWebhook(gopayId: String, state: String): Boolean {
        val payment = gopayPaymentRepository.findByGopayId(gopayId)
            ?: return false

        if (payment.state == "PAID") {
            return true // Already processed
        }

        val updatedPayment = payment.copy(
            state = state,
            status = state,
            updatedAt = Instant.now()
        )
        gopayPaymentRepository.save(updatedPayment)

        if (state == "PAID" && payment.paymentType == "credit_purchase" && payment.creditPackageId != null) {
            val creditPackage = creditPackageRepository.findById(payment.creditPackageId).orElse(null)
            if (creditPackage != null && payment.userId != null) {
                val totalCredits = creditPackage.credits + creditPackage.bonusCredits
                userRepository.updateCredits(payment.userId, totalCredits)

                creditTransactionRepository.save(
                    CreditTransaction(
                        userId = payment.userId,
                        amount = totalCredits,
                        type = TransactionType.PURCHASE.value,
                        gopayPaymentId = gopayId,
                        note = "Nákup: ${creditPackage.nameCs}"
                    )
                )
            }
        }

        return true
    }

    fun getPaymentByGopayId(gopayId: String): GopayPayment? {
        return gopayPaymentRepository.findByGopayId(gopayId)
    }
}
