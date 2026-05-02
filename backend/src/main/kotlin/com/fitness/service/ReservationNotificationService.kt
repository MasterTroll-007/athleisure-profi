package com.fitness.service

import com.fitness.entity.User
import com.fitness.entity.displayName
import com.fitness.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Service
class ReservationNotificationService(
    private val userRepository: UserRepository,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(ReservationNotificationService::class.java)

    fun notifyTrainerNewReservation(
        trainerId: UUID?,
        client: User,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime
    ) {
        if (trainerId == null) return
        try {
            val trainer = userRepository.findById(trainerId).orElse(null) ?: return
            emailService.sendAdminNewReservationEmail(
                adminEmail = trainer.email,
                adminName = trainer.firstName,
                clientName = client.displayName.ifEmpty { client.email },
                clientEmail = client.email,
                date = date,
                startTime = startTime,
                endTime = endTime
            )
        } catch (e: Exception) {
            logger.error("Failed to send new reservation notification", e)
        }
    }

    fun notifyTrainerCancelledReservation(
        trainerId: UUID?,
        client: User,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime
    ) {
        if (trainerId == null) return
        try {
            val trainer = userRepository.findById(trainerId).orElse(null) ?: return
            emailService.sendAdminCancelledReservationEmail(
                adminEmail = trainer.email,
                adminName = trainer.firstName,
                clientName = client.displayName.ifEmpty { client.email },
                clientEmail = client.email,
                date = date,
                startTime = startTime,
                endTime = endTime
            )
        } catch (e: Exception) {
            logger.error("Failed to send cancellation notification", e)
        }
    }
}
