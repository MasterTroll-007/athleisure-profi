package com.fitness.service

import com.fitness.entity.WaitlistEntry
import com.fitness.entity.displayName
import com.fitness.repository.SlotRepository
import com.fitness.repository.UserRepository
import com.fitness.repository.WaitlistRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class WaitlistService(
    private val waitlistRepository: WaitlistRepository,
    private val slotRepository: SlotRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(WaitlistService::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("d. M. yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    @Transactional
    fun joinWaitlist(userId: String, slotId: String): WaitlistEntry {
        val userUUID = UUID.fromString(userId)
        val slotUUID = UUID.fromString(slotId)

        slotRepository.findById(slotUUID)
            .orElseThrow { NoSuchElementException("Slot not found") }

        if (waitlistRepository.existsByUserIdAndSlotIdAndStatus(userUUID, slotUUID, "waiting")) {
            throw IllegalArgumentException("Already on waitlist for this slot")
        }

        return waitlistRepository.save(
            WaitlistEntry(
                userId = userUUID,
                slotId = slotUUID
            )
        )
    }

    @Transactional
    fun leaveWaitlist(userId: String, slotId: String) {
        val userUUID = UUID.fromString(userId)
        val slotUUID = UUID.fromString(slotId)
        val entries = waitlistRepository.findBySlotIdAndStatusOrderByCreatedAt(slotUUID, "waiting")
        val entry = entries.find { it.userId == userUUID }
            ?: throw NoSuchElementException("Waitlist entry not found")
        waitlistRepository.save(entry.copy(status = "cancelled"))
    }

    fun getMyWaitlist(userId: String): List<Map<String, Any?>> {
        val entries = waitlistRepository.findByUserId(UUID.fromString(userId))
            .filter { it.status in listOf("waiting", "notified") }
        if (entries.isEmpty()) return emptyList()

        val slotIds = entries.map { it.slotId }.distinct()
        val slotsMap = slotRepository.findAllById(slotIds).associateBy { it.id }

        return entries.map { entry ->
            val slot = slotsMap[entry.slotId]
            mapOf(
                "id" to entry.id.toString(),
                "slotId" to entry.slotId.toString(),
                "status" to entry.status,
                "date" to slot?.date?.toString(),
                "startTime" to slot?.startTime?.format(timeFormatter),
                "endTime" to slot?.endTime?.format(timeFormatter),
                "createdAt" to entry.createdAt.toString()
            )
        }
    }

    @Transactional
    fun processWaitlistOnCancellation(slotId: UUID) {
        val slot = slotRepository.findById(slotId).orElse(null) ?: return
        val waitingEntries = waitlistRepository.findBySlotIdAndStatusOrderByCreatedAt(slotId, "waiting")
        if (waitingEntries.isEmpty()) return

        // Notify the first person on the waitlist
        val first = waitingEntries.first()
        val user = userRepository.findById(first.userId).orElse(null) ?: return

        val formattedDate = slot.date.format(dateFormatter)
        val formattedTime = "${slot.startTime.format(timeFormatter)} - ${slot.endTime.format(timeFormatter)}"

        emailService.sendWaitlistNotification(
            to = user.email,
            firstName = user.firstName,
            date = formattedDate,
            time = formattedTime
        )

        // Mark as notified with 2-hour expiration
        val updated = first.copy(
            status = "notified",
            notifiedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(7200)
        )
        waitlistRepository.save(updated)

        logger.info("Waitlist notification sent to ${user.email} for slot ${slot.id}")
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    fun expireNotifications() {
        val expired = waitlistRepository.findExpiredNotifications(Instant.now())
        for (entry in expired) {
            waitlistRepository.save(entry.copy(status = "expired"))
            // Notify next person
            processWaitlistOnCancellation(entry.slotId)
        }
    }
}
