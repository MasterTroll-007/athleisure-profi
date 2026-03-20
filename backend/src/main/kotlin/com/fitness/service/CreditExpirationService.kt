package com.fitness.service

import com.fitness.entity.CreditTransaction
import com.fitness.entity.TransactionType
import com.fitness.repository.CreditTransactionRepository
import com.fitness.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CreditExpirationService(
    private val creditTransactionRepository: CreditTransactionRepository,
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(CreditExpirationService::class.java)

    /**
     * Check for expired credits daily at midnight.
     * Finds PURCHASE transactions with expiresAt in the past and deducts credits.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    fun processExpiredCredits() {
        logger.info("Checking for expired credits")

        val now = Instant.now()
        val expiredTransactions = creditTransactionRepository.findExpiredTransactions(now)

        for (tx in expiredTransactions) {
            if (tx.amount <= 0) continue

            val user = userRepository.findById(tx.userId).orElse(null) ?: continue

            // Only deduct what the user still has
            val deduction = minOf(tx.amount, user.credits)
            if (deduction <= 0) continue

            userRepository.updateCredits(tx.userId, -deduction)

            creditTransactionRepository.save(
                CreditTransaction(
                    userId = tx.userId,
                    amount = -deduction,
                    type = TransactionType.EXPIRATION.value,
                    referenceId = tx.id,
                    note = "Expirace kreditů"
                )
            )

            logger.info("Expired $deduction credits for user ${tx.userId}, transaction ${tx.id}")
        }

        logger.info("Credit expiration check completed, processed ${expiredTransactions.size} transactions")
    }
}
