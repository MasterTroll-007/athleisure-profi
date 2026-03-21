package com.fitness.service

import com.fitness.repository.CreditTransactionRepository
import com.fitness.repository.ReservationRepository
import com.fitness.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@Service
class MonthlyReportService(
    private val userRepository: UserRepository,
    private val reservationRepository: ReservationRepository,
    private val creditTransactionRepository: CreditTransactionRepository,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(MonthlyReportService::class.java)

    @Scheduled(cron = "0 0 8 1 * *") // 1st of month at 8:00
    fun sendMonthlyReports() {
        logger.info("Generating monthly reports")
        val zone = ZoneId.of("Europe/Prague")
        val today = LocalDate.now(zone)
        val lastMonthStart = today.minusMonths(1).withDayOfMonth(1)
        val lastMonthEnd = today.withDayOfMonth(1).minusDays(1)

        val instantStart = lastMonthStart.atStartOfDay(zone).toInstant()
        val instantEnd = lastMonthEnd.plusDays(1).atStartOfDay(zone).toInstant()

        val admins = userRepository.findByRole("admin")

        for (admin in admins) {
            try {
                val adminId = admin.id

                val completedCount = reservationRepository.countByStatusAndDateBetween("completed", lastMonthStart, lastMonthEnd)
                val noShowCount = reservationRepository.countByStatusAndDateBetween("no_show", lastMonthStart, lastMonthEnd)
                val confirmedCount = reservationRepository.countByStatusAndDateBetween("confirmed", lastMonthStart, lastMonthEnd)

                val totalSessions = completedCount + noShowCount + confirmedCount
                val attendanceRate = if (totalSessions > 0) {
                    (completedCount * 100.0 / totalSessions).toLong()
                } else 0L

                val noShowRate = if (totalSessions > 0) {
                    (noShowCount * 100.0 / totalSessions).toLong()
                } else 0L

                val newClients = userRepository.countByTrainerId(adminId) // simplified
                val creditsSold = creditTransactionRepository.sumAmountByTypeAndDateRange(
                    "purchase", instantStart, instantEnd
                ) ?: 0L

                val stats = mapOf<String, Any>(
                    "completedSessions" to completedCount,
                    "newClients" to newClients,
                    "creditsSold" to creditsSold,
                    "attendanceRate" to attendanceRate,
                    "noShowRate" to noShowRate
                )

                emailService.sendMonthlyReportEmail(
                    to = admin.email,
                    trainerName = admin.firstName,
                    stats = stats
                )

                logger.info("Monthly report sent to ${admin.email}")
            } catch (e: Exception) {
                logger.error("Failed to send monthly report to ${admin.email}", e)
            }
        }
    }
}
