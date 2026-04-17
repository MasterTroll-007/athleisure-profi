package com.fitness.service

import com.fitness.entity.CreditExpirationNotification
import com.fitness.repository.CreditExpirationNotificationRepository
import com.fitness.repository.CreditTransactionRepository
import com.fitness.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class CreditExpirationWarningService(
    private val creditTransactionRepository: CreditTransactionRepository,
    private val notificationRepository: CreditExpirationNotificationRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(CreditExpirationWarningService::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("d. M. yyyy")

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    fun sendExpirationWarnings() {
        logger.info("Checking for expiring credits to send warnings")
        val zone = ZoneId.of("Europe/Prague")

        for (daysBefore in listOf(7, 1)) {
            val targetDate = LocalDate.now(zone).plusDays(daysBefore.toLong())
            val from = targetDate.atStartOfDay(zone).toInstant()
            val to = targetDate.plusDays(1).atStartOfDay(zone).toInstant()

            val expiringTransactions = creditTransactionRepository.findExpiringBetween(from, to)

            for (tx in expiringTransactions) {
                if (tx.amount <= 0) continue
                if (notificationRepository.existsByTransactionIdAndDaysBeforeValue(tx.id!!, daysBefore)) continue

                val user = userRepository.findById(tx.userId).orElse(null) ?: continue
                val expiresAt = tx.expiresAt?.atZone(zone)?.format(dateFormatter) ?: continue

                emailService.sendCreditExpirationWarning(
                    to = user.email,
                    firstName = user.firstName,
                    credits = tx.amount,
                    expiresAt = expiresAt,
                    daysUntil = daysBefore
                )

                notificationRepository.save(
                    CreditExpirationNotification(
                        transactionId = tx.id!!,
                        userId = tx.userId,
                        daysBefore = daysBefore
                    )
                )
            }
        }

        logger.info("Credit expiration warning check completed")
    }
}
