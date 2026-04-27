package com.fitness.service

import com.fitness.dto.AnnouncementDTO
import com.fitness.dto.CreateAnnouncementRequest
import com.fitness.entity.Announcement
import com.fitness.entity.displayName
import com.fitness.repository.AnnouncementRepository
import com.fitness.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class AnnouncementService(
    private val announcementRepository: AnnouncementRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(AnnouncementService::class.java)

    @Transactional
    fun createAnnouncement(trainerId: String, request: CreateAnnouncementRequest): AnnouncementDTO {
        val trainerUUID = UUID.fromString(trainerId)

        // Rate limit: 1 announcement per day
        val oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS)
        val recentCount = announcementRepository.countByTrainerIdAndCreatedAtAfter(trainerUUID, oneDayAgo)
        if (recentCount > 0) {
            throw IllegalArgumentException("You can only send one announcement per day")
        }

        val trainer = userRepository.findById(trainerUUID)
            .orElseThrow { NoSuchElementException("Trainer not found") }
        val trainerName = trainer.displayName

        // Get all clients of this trainer
        val pageable = PageRequest.of(0, 1000, Sort.by("createdAt").descending())
        val clients = userRepository.findClientsByTrainerId(trainerUUID, pageable).content

        // Send emails
        var sent = 0
        for (client in clients) {
            try {
                emailService.sendAnnouncementEmail(
                    to = client.email,
                    firstName = client.firstName,
                    subject = request.subject,
                    message = request.message,
                    trainerName = trainerName
                )
                sent++
            } catch (e: Exception) {
                logger.error("Failed to send announcement to ${client.email}", e)
            }
        }

        val announcement = announcementRepository.save(
            Announcement(
                trainerId = trainerUUID,
                subject = request.subject,
                message = request.message,
                recipientsCount = sent
            )
        )

        return toDTO(announcement)
    }

    fun getAnnouncements(trainerId: String): List<AnnouncementDTO> {
        val trainerUUID = UUID.fromString(trainerId)
        return announcementRepository.findByTrainerIdOrderByCreatedAtDesc(trainerUUID).map { toDTO(it) }
    }

    private fun toDTO(announcement: Announcement) = AnnouncementDTO(
        id = announcement.id.toString(),
        subject = announcement.subject,
        message = announcement.message,
        recipientsCount = announcement.recipientsCount,
        createdAt = announcement.createdAt.toString()
    )
}
