package com.fitness.service

import com.fitness.entity.ReminderSent
import com.fitness.repository.ReminderSentRepository
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
class ReminderSchedulerService(
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val reminderSentRepository: ReminderSentRepository,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(ReminderSchedulerService::class.java)

    companion object {
        const val REMINDER_TYPE_24H = "24h"
        const val REMINDER_TYPE_1H = "1h"
    }

    /**
     * Runs every 15 minutes to check for reservations needing reminders.
     * Checks for both 24-hour and 1-hour reminders based on user preferences.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes in milliseconds
    @Transactional
    fun processReminders() {
        logger.info("Starting reminder processing...")

        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)

        // Get all confirmed reservations for today and tomorrow
        val reservations = reservationRepository.findConfirmedByDateRange(today, tomorrow)

        if (reservations.isEmpty()) {
            logger.debug("No reservations to process for reminders")
            return
        }

        // Get all user IDs from reservations
        val userIds = reservations.map { it.userId }.distinct()
        val usersMap = userRepository.findAllById(userIds).associateBy { it.id }

        // Get reservation IDs that already have reminders sent
        val reservationIds = reservations.map { it.id }
        val sentReminders24h = reminderSentRepository.findReservationIdsWithReminder(reservationIds, REMINDER_TYPE_24H)
        val sentReminders1h = reminderSentRepository.findReservationIdsWithReminder(reservationIds, REMINDER_TYPE_1H)

        var sentCount = 0

        for (reservation in reservations) {
            val user = usersMap[reservation.userId] ?: continue

            // Skip if user has disabled email reminders
            if (!user.emailRemindersEnabled) {
                continue
            }

            val reservationDateTime = LocalDateTime.of(reservation.date, reservation.startTime)
            val hoursUntilReservation = java.time.Duration.between(now, reservationDateTime).toHours()

            // Determine which reminder to send based on user preference
            val reminderHours = user.reminderHoursBefore

            // Check for 24-hour reminder (if user preference is 24h)
            if (reminderHours >= 24 &&
                hoursUntilReservation in 23..26 &&
                reservation.id !in sentReminders24h) {

                sendReminderAndRecord(
                    reservation.id,
                    user.id,
                    user.email,
                    user.firstName,
                    reservation.date,
                    reservation.startTime,
                    reservation.endTime,
                    user.locale,
                    REMINDER_TYPE_24H
                )
                sentCount++
            }

            // Check for 1-hour reminder (if user preference is 1h or as additional reminder)
            if (reminderHours <= 1 &&
                hoursUntilReservation in 0..2 &&
                reservation.id !in sentReminders1h) {

                sendReminderAndRecord(
                    reservation.id,
                    user.id,
                    user.email,
                    user.firstName,
                    reservation.date,
                    reservation.startTime,
                    reservation.endTime,
                    user.locale,
                    REMINDER_TYPE_1H
                )
                sentCount++
            }
        }

        logger.info("Reminder processing completed. Sent $sentCount reminders.")
    }

    private fun sendReminderAndRecord(
        reservationId: java.util.UUID,
        userId: java.util.UUID,
        email: String,
        firstName: String?,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        locale: String,
        reminderType: String
    ) {
        try {
            // Record the reminder first to avoid duplicate sends in case of failure
            val reminderSent = ReminderSent(
                reservationId = reservationId,
                userId = userId,
                reminderType = reminderType
            )
            reminderSentRepository.save(reminderSent)

            // Send the email asynchronously
            emailService.sendReminderEmail(
                to = email,
                firstName = firstName,
                date = date,
                startTime = startTime,
                endTime = endTime,
                locale = locale
            )

            logger.debug("Sent $reminderType reminder to $email for reservation on $date at $startTime")
        } catch (e: Exception) {
            logger.error("Failed to send reminder to $email: ${e.message}", e)
        }
    }
}
