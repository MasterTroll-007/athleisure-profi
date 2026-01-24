package com.fitness.service

import com.fitness.entity.ReminderSentLog
import com.fitness.repository.ReminderSentLogRepository
import com.fitness.repository.ReservationRepository
import com.fitness.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class ReminderService(
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val reminderSentLogRepository: ReminderSentLogRepository,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(ReminderService::class.java)

    /**
     * Runs every 15 minutes to check for reservations that need reminders.
     * Checks for reservations that are within the user's preferred reminder window
     * (either 1 hour or 24 hours before).
     */
    @Scheduled(fixedRate = 900000) // Every 15 minutes (900,000 ms)
    @Transactional
    fun sendScheduledReminders() {
        logger.info("Starting scheduled reminder check...")

        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)

        // Get all confirmed reservations for today and tomorrow
        val reservations = reservationRepository.findConfirmedByDateRange(today, tomorrow)

        if (reservations.isEmpty()) {
            logger.debug("No upcoming reservations found for reminder check")
            return
        }

        // Get IDs of reservations that already have reminders sent
        val sentReminderIds = reminderSentLogRepository.findReservationIdsByReminderType("email").toSet()

        var remindersSent = 0

        for (reservation in reservations) {
            // Skip if reminder already sent
            if (reservation.id in sentReminderIds) {
                continue
            }

            // Get user preferences
            val user = userRepository.findById(reservation.userId).orElse(null) ?: continue

            // Skip if user has disabled reminders
            if (!user.emailRemindersEnabled) {
                continue
            }

            // Calculate the reminder time based on user preference
            val reservationDateTime = LocalDateTime.of(reservation.date, reservation.startTime)
            val reminderTime = reservationDateTime.minusHours(user.reminderHoursBefore.toLong())

            // Check if it's time to send the reminder (within the 15-minute window)
            val windowEnd = now.plusMinutes(15)

            if (now >= reminderTime && now <= reservationDateTime) {
                // Send reminder
                try {
                    emailService.sendReminderEmail(
                        to = user.email,
                        firstName = user.firstName,
                        date = reservation.date,
                        startTime = reservation.startTime,
                        endTime = reservation.endTime,
                        locale = user.locale
                    )

                    // Log the sent reminder
                    reminderSentLogRepository.save(
                        ReminderSentLog(
                            reservationId = reservation.id,
                            userId = reservation.userId,
                            reminderType = "email"
                        )
                    )

                    remindersSent++
                    logger.info("Sent reminder email to ${user.email} for reservation on ${reservation.date} at ${reservation.startTime}")
                } catch (e: Exception) {
                    logger.error("Failed to send reminder to ${user.email}: ${e.message}", e)
                }
            }
        }

        logger.info("Reminder check completed. Sent $remindersSent reminders.")
    }

    /**
     * Manually trigger a reminder for a specific reservation (for testing/admin use).
     */
    @Transactional
    fun sendReminderForReservation(reservationId: java.util.UUID): Boolean {
        val reservation = reservationRepository.findById(reservationId).orElse(null) ?: return false
        val user = userRepository.findById(reservation.userId).orElse(null) ?: return false

        // Check if reminder already sent
        if (reminderSentLogRepository.existsByReservationIdAndReminderType(reservationId, "email")) {
            logger.warn("Reminder already sent for reservation $reservationId")
            return false
        }

        emailService.sendReminderEmail(
            to = user.email,
            firstName = user.firstName,
            date = reservation.date,
            startTime = reservation.startTime,
            endTime = reservation.endTime,
            locale = user.locale
        )

        reminderSentLogRepository.save(
            ReminderSentLog(
                reservationId = reservation.id,
                userId = reservation.userId,
                reminderType = "email"
            )
        )

        return true
    }
}
