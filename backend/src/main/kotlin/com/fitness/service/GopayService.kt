package com.fitness.service

import com.fitness.entity.CreditTransaction
import com.fitness.entity.GopayPayment
import com.fitness.entity.TransactionType
import com.fitness.repository.*
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(GopayService::class.java)

    companion object {
        // GoPay payment states
        // See: https://doc.gopay.com/en/#payment-status
        private val TERMINAL_STATES = setOf("PAID", "CANCELED", "TIMEOUTED", "REFUNDED")
        private val FAILED_STATES = setOf("CANCELED", "TIMEOUTED")
    }

    /**
     * Handle GoPay webhook callback.
     * Called after webhook verifies payment status with GoPay API.
     */
    @Transactional
    fun handleWebhook(gopayId: String, state: String): Boolean {
        val payment = gopayPaymentRepository.findByGopayId(gopayId)
            ?: return false

        // Idempotency check - don't reprocess terminal states
        if (payment.state in TERMINAL_STATES) {
            logger.debug("Payment $gopayId already in terminal state: ${payment.state}")
            return true
        }

        // Map GoPay state to internal status
        val internalStatus = when (state) {
            "PAID" -> "completed"
            "CANCELED", "TIMEOUTED" -> "failed"
            "REFUNDED" -> "refunded"
            "CREATED", "PAYMENT_METHOD_CHOSEN", "AUTHORIZED" -> "pending"
            else -> "pending"
        }

        val updatedPayment = payment.copy(
            state = state,
            status = internalStatus,
            updatedAt = Instant.now()
        )
        gopayPaymentRepository.save(updatedPayment)

        logger.info("Payment $gopayId updated: state=$state, status=$internalStatus")

        // Process successful payments
        if (state == "PAID" && payment.paymentType == "credit_purchase" && payment.creditPackageId != null) {
            processCreditPurchase(payment, gopayId)
        }

        // Log failed payments for monitoring
        if (state in FAILED_STATES) {
            logger.warn("Payment failed: gopayId=$gopayId, state=$state, userId=${payment.userId}")
        }

        return true
    }

    private fun processCreditPurchase(payment: GopayPayment, gopayId: String) {
        val creditPackage = creditPackageRepository.findById(payment.creditPackageId!!).orElse(null)
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

            logger.info("Credits added: userId=${payment.userId}, credits=$totalCredits, package=${creditPackage.nameCs}")
        }
    }

    fun getPaymentByGopayId(gopayId: String): GopayPayment? {
        return gopayPaymentRepository.findByGopayId(gopayId)
    }
}
