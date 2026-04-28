package com.fitness.service

import com.fitness.entity.ReminderSent
import com.fitness.repository.ReminderSentLogRepository
import com.fitness.repository.ReservationRepository
import com.fitness.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReminderService(
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val reminderSentLogRepository: ReminderSentLogRepository,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(ReminderService::class.java)

    /**
     * Manually trigger a reminder for a specific reservation (for testing/admin use).
     * Automatic reminders are handled by ReminderSchedulerService only, so the
     * app has a single scheduler and cannot send duplicate reminder emails.
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
            ReminderSent(
                reservationId = reservation.id!!,
                userId = reservation.userId,
                reminderType = "email"
            )
        )

        return true
    }
}
