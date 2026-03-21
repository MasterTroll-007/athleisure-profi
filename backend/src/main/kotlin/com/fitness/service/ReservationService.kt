package com.fitness.service

import com.fitness.dto.*
import com.fitness.entity.CreditTransaction
import com.fitness.entity.Reservation
import com.fitness.entity.SlotStatus
import com.fitness.entity.TransactionType
import com.fitness.mapper.ReservationMapper
import com.fitness.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class ReservationService(
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val slotRepository: SlotRepository,
    private val pricingItemRepository: PricingItemRepository,
    private val slotPricingItemRepository: SlotPricingItemRepository,
    private val creditTransactionRepository: CreditTransactionRepository,
    private val cancellationPolicyRepository: CancellationPolicyRepository,
    private val reservationMapper: ReservationMapper,
    private val auditService: AuditService,
    private val emailService: EmailService,
    private val waitlistService: WaitlistService
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(ReservationService::class.java)

    @Transactional
    fun createReservation(userId: String, request: CreateReservationRequest): ReservationDTO {
        val userUUID = UUID.fromString(userId)
        val user = userRepository.findById(userUUID)
            .orElseThrow { NoSuchElementException("User not found") }

        val slotId = UUID.fromString(request.blockId)
        val slot = slotRepository.findById(slotId)
            .orElseThrow { NoSuchElementException("Slot not found") }

        // Check if user is blocked
        if (user.isBlocked) {
            throw IllegalArgumentException("Your account has been blocked. Contact your trainer for more information.")
        }

        val date = LocalDate.parse(request.date)
        val startTime = LocalTime.parse(request.startTime)
        val endTime = LocalTime.parse(request.endTime)

        // Validate date boundaries - cannot book in the past
        val today = LocalDate.now()
        if (date.isBefore(today)) {
            throw IllegalArgumentException("Cannot create reservation for a past date")
        }

        // Validate date boundaries - cannot book more than 90 days in advance
        val maxFutureDate = today.plusDays(90)
        if (date.isAfter(maxFutureDate)) {
            throw IllegalArgumentException("Cannot create reservation more than 90 days in advance")
        }

        // Check credits - validate pricing item belongs to slot
        val creditsNeeded = request.pricingItemId?.let { piId ->
            val pricingItem = pricingItemRepository.findById(UUID.fromString(piId))
                .orElseThrow { NoSuchElementException("Pricing item not found") }
            // Validate pricing item is assigned to this slot
            val slotPricingItems = slotPricingItemRepository.findBySlotId(slotId)
            if (slotPricingItems.isNotEmpty() && slotPricingItems.none { it.pricingItemId == pricingItem.id }) {
                throw IllegalArgumentException("Selected training type is not available for this slot")
            }
            pricingItem.credits
        } ?: 1

        // Check availability - support group training capacity
        val existingReservations = reservationRepository.findByDateAndSlotId(date, slotId)
        val currentBookings = existingReservations.size
        if (currentBookings >= slot.capacity) {
            throw IllegalArgumentException("This slot is fully booked")
        }

        // Check if user already has a reservation on this date (max 1 per day)
        if (reservationRepository.existsByUserIdAndDateConfirmed(userUUID, date)) {
            throw IllegalArgumentException("Již máte rezervaci na tento den. Maximálně jedna rezervace denně.")
        }

        // Atomically deduct credits (prevents race condition)
        val rowsUpdated = userRepository.deductCreditsIfSufficient(userUUID, creditsNeeded)
        if (rowsUpdated == 0) {
            throw IllegalArgumentException("Not enough credits")
        }

        // Create reservation
        val reservation = reservationRepository.save(
            Reservation(
                userId = userUUID,
                slotId = slotId,
                date = date,
                startTime = startTime,
                endTime = endTime,
                creditsUsed = creditsNeeded,
                pricingItemId = request.pricingItemId?.let { UUID.fromString(it) }
            )
        )

        // Update slot status to RESERVED only if now full (capacity reached)
        if (currentBookings + 1 >= slot.capacity) {
            val updatedSlot = slot.copy(status = SlotStatus.RESERVED)
            slotRepository.save(updatedSlot)
        }

        // Record transaction
        creditTransactionRepository.save(
            CreditTransaction(
                userId = userUUID,
                amount = -creditsNeeded,
                type = TransactionType.RESERVATION.value,
                referenceId = reservation.id,
                note = "Rezervace na $date"
            )
        )

        // Notify trainer about new reservation
        notifyTrainerNewReservation(slot.adminId, user, date, startTime, endTime)

        return reservationMapper.toDTO(
            reservation,
            user.firstName,
            user.lastName,
            user.email,
            null
        )
    }

    fun getUserReservations(userId: String): List<ReservationDTO> {
        val reservations = reservationRepository.findByUserId(UUID.fromString(userId))
        return reservationMapper.toDTOBatch(reservations)
    }

    fun getUpcomingReservations(userId: String): List<ReservationDTO> {
        val reservations = reservationRepository.findUpcomingByUserId(UUID.fromString(userId), LocalDate.now())
        return reservationMapper.toDTOBatch(reservations)
    }

    fun getReservationById(id: String): ReservationDTO {
        val reservation = reservationRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Reservation not found") }
        return reservationMapper.toDTO(reservation)
    }

    @Transactional
    fun cancelReservation(userId: String, reservationId: String): CancellationResultDTO {
        val reservation = reservationRepository.findById(UUID.fromString(reservationId))
            .orElseThrow { NoSuchElementException("Reservation not found") }

        if (reservation.userId.toString() != userId) {
            throw IllegalArgumentException("Access denied")
        }

        if (reservation.status == "cancelled") {
            throw IllegalArgumentException("Reservation already cancelled")
        }

        // Get trainer's cancellation policy
        val trainerId = reservation.slotId?.let { slotId ->
            slotRepository.findById(slotId).orElse(null)?.adminId
        }
        val policy = trainerId?.let { cancellationPolicyRepository.findByTrainerId(it) }

        // Calculate refund based on policy
        val reservationDateTime = LocalDateTime.of(reservation.date, reservation.startTime)
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val hoursUntil = ChronoUnit.MINUTES.between(now, reservationDateTime) / 60.0

        val (refundPercentage, policyApplied) = calculateRefundPercentage(policy, hoursUntil)
        val refundAmount = (reservation.creditsUsed * refundPercentage / 100.0).toInt()

        val updated = reservation.copy(
            status = "cancelled",
            cancelledAt = Instant.now()
        )
        reservationRepository.save(updated)

        // Update slot status back to UNLOCKED if it was full
        reservation.slotId?.let { slotId ->
            slotRepository.findById(slotId).ifPresent { slot ->
                if (slot.status == SlotStatus.RESERVED) {
                    val updatedSlot = slot.copy(status = SlotStatus.UNLOCKED)
                    slotRepository.save(updatedSlot)
                }
            }
        }

        // Refund credits based on policy
        if (refundAmount > 0) {
            userRepository.updateCredits(reservation.userId, refundAmount)

            val refundNote = when (policyApplied) {
                "FULL_REFUND" -> "Vrácení kreditu za zrušenou rezervaci (100%)"
                "PARTIAL_REFUND" -> "Částečné vrácení kreditu za zrušenou rezervaci ($refundPercentage%)"
                else -> "Vrácení kreditu za zrušenou rezervaci"
            }

            creditTransactionRepository.save(
                CreditTransaction(
                    userId = reservation.userId,
                    amount = refundAmount,
                    type = TransactionType.REFUND.value,
                    referenceId = reservation.id,
                    note = refundNote
                )
            )
        }

        // Trigger waitlist processing
        reservation.slotId?.let { slotId ->
            try {
                waitlistService.processWaitlistOnCancellation(slotId)
            } catch (e: Exception) {
                logger.error("Failed to process waitlist for slot $slotId", e)
            }
        }

        // Notify trainer about cancelled reservation
        val cancelUser = userRepository.findById(reservation.userId).orElse(null)
        if (cancelUser != null) {
            notifyTrainerCancelledReservation(trainerId, cancelUser, reservation.date, reservation.startTime, reservation.endTime)
        }

        return CancellationResultDTO(
            reservation = reservationMapper.toDTO(updated),
            refundAmount = refundAmount,
            refundPercentage = refundPercentage,
            policyApplied = policyApplied
        )
    }

    private fun calculateRefundPercentage(
        policy: com.fitness.entity.CancellationPolicy?,
        hoursUntil: Double
    ): Pair<Int, String> {
        if (policy == null || !policy.isActive) {
            return Pair(100, "NO_POLICY")
        }

        return when {
            hoursUntil >= policy.fullRefundHours -> Pair(100, "FULL_REFUND")
            policy.partialRefundHours != null &&
                policy.partialRefundPercentage != null &&
                hoursUntil >= policy.partialRefundHours ->
                    Pair(policy.partialRefundPercentage, "PARTIAL_REFUND")
            else -> Pair(0, "NO_REFUND")
        }
    }

    fun getAllReservations(startDate: LocalDate, endDate: LocalDate): List<ReservationCalendarEvent> {
        val reservations = reservationRepository.findByDateRange(startDate, endDate)
        return reservationMapper.toCalendarEventBatch(reservations)
    }

    /**
     * Admin creates a reservation for a user.
     * Optionally deducts credits from the user.
     */
    @Transactional
    fun adminCreateReservation(request: AdminCreateReservationRequest, adminId: String? = null, adminEmail: String? = null): ReservationDTO {
        val userUUID = UUID.fromString(request.userId)
        val user = userRepository.findById(userUUID)
            .orElseThrow { NoSuchElementException("User not found") }

        val blockId = UUID.fromString(request.blockId)
        val slot = slotRepository.findById(blockId)
            .orElseThrow { NoSuchElementException("Slot not found") }

        val date = LocalDate.parse(request.date)
        val startTime = LocalTime.parse(request.startTime)
        val endTime = LocalTime.parse(request.endTime)

        // Validate date boundaries - admin can book up to 365 days in advance
        val today = LocalDate.now()
        val maxFutureDate = today.plusDays(365)
        if (date.isAfter(maxFutureDate)) {
            throw IllegalArgumentException("Cannot create reservation more than 365 days in advance")
        }

        // Check availability
        val existingReservations = reservationRepository.findByDateAndSlotId(date, blockId)
        if (existingReservations.any { it.startTime == startTime && it.status == "confirmed" }) {
            throw IllegalArgumentException("This slot is already booked")
        }

        val creditsToDeduct = if (request.deductCredits) 1 else 0

        if (request.deductCredits && user.credits < creditsToDeduct) {
            throw IllegalArgumentException("User does not have enough credits")
        }

        // Create reservation
        val reservation = reservationRepository.save(
            Reservation(
                userId = userUUID,
                slotId = blockId,
                date = date,
                startTime = startTime,
                endTime = endTime,
                creditsUsed = creditsToDeduct,
                note = request.note
            )
        )

        // Update slot status to RESERVED
        val updatedSlot = slot.copy(status = SlotStatus.RESERVED)
        slotRepository.save(updatedSlot)

        // Deduct credits if requested
        if (request.deductCredits && creditsToDeduct > 0) {
            userRepository.updateCredits(userUUID, -creditsToDeduct)
            creditTransactionRepository.save(
                CreditTransaction(
                    userId = userUUID,
                    amount = -creditsToDeduct,
                    type = TransactionType.RESERVATION.value,
                    referenceId = reservation.id,
                    note = "Admin rezervace na $date"
                )
            )
        }
        
        // Audit log the admin-created reservation
        if (adminId != null) {
            auditService.logReservationCreated(
                adminId = adminId,
                adminEmail = adminEmail,
                reservationId = reservation.id.toString(),
                userId = request.userId,
                deductCredits = request.deductCredits,
                creditsDeducted = creditsToDeduct
            )
        }

        return reservationMapper.toDTO(
            reservation,
            user.firstName,
            user.lastName,
            user.email,
            null
        )
    }

    /**
     * Admin cancels any reservation.
     * Optionally refunds credits to the user.
     */
    @Transactional
    fun adminCancelReservation(reservationId: String, refundCredits: Boolean, adminId: String? = null, adminEmail: String? = null): ReservationDTO {
        val reservation = reservationRepository.findById(UUID.fromString(reservationId))
            .orElseThrow { NoSuchElementException("Reservation not found") }

        if (reservation.status == "cancelled") {
            throw IllegalArgumentException("Reservation already cancelled")
        }

        val updated = reservation.copy(
            status = "cancelled",
            cancelledAt = Instant.now()
        )
        reservationRepository.save(updated)

        // Update slot status back to UNLOCKED
        reservation.slotId?.let { slotId ->
            slotRepository.findById(slotId).ifPresent { slot ->
                val updatedSlot = slot.copy(status = SlotStatus.UNLOCKED)
                slotRepository.save(updatedSlot)
            }
        }

        // Refund credits if requested and credits were used
        val creditsRefunded = if (refundCredits && reservation.creditsUsed > 0) {
            userRepository.updateCredits(reservation.userId, reservation.creditsUsed)
            creditTransactionRepository.save(
                CreditTransaction(
                    userId = reservation.userId,
                    amount = reservation.creditsUsed,
                    type = TransactionType.REFUND.value,
                    referenceId = reservation.id,
                    note = "Admin zrušení rezervace - vrácení kreditu"
                )
            )
            reservation.creditsUsed
        } else {
            0
        }
        
        // Audit log the admin cancellation
        if (adminId != null) {
            auditService.logReservationCancellation(
                adminId = adminId,
                adminEmail = adminEmail,
                reservationId = reservationId,
                userId = reservation.userId.toString(),
                refundCredits = refundCredits,
                creditsRefunded = creditsRefunded
            )
        }

        return reservationMapper.toDTO(updated)
    }

    /**
     * Update the note on a reservation.
     */
    @Transactional
    fun updateReservationNote(reservationId: String, note: String?): ReservationDTO {
        val reservation = reservationRepository.findById(UUID.fromString(reservationId))
            .orElseThrow { NoSuchElementException("Reservation not found") }

        val updated = reservation.copy(note = note)
        reservationRepository.save(updated)

        return reservationMapper.toDTO(updated)
    }

    /**
     * Get refund preview for a reservation based on cancellation policy.
     */
    private fun notifyTrainerNewReservation(
        trainerId: UUID?,
        client: com.fitness.entity.User,
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
                clientName = "${client.firstName ?: ""} ${client.lastName ?: ""}".trim().ifEmpty { client.email },
                clientEmail = client.email,
                date = date,
                startTime = startTime,
                endTime = endTime
            )
        } catch (e: Exception) {
            logger.error("Failed to send new reservation notification", e)
        }
    }

    private fun notifyTrainerCancelledReservation(
        trainerId: UUID?,
        client: com.fitness.entity.User,
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
                clientName = "${client.firstName ?: ""} ${client.lastName ?: ""}".trim().ifEmpty { client.email },
                clientEmail = client.email,
                date = date,
                startTime = startTime,
                endTime = endTime
            )
        } catch (e: Exception) {
            logger.error("Failed to send cancellation notification", e)
        }
    }

    @Transactional
    fun markAttendance(reservationId: String, status: String, adminId: String, adminEmail: String?): ReservationDTO {
        val reservation = reservationRepository.findById(UUID.fromString(reservationId))
            .orElseThrow { NoSuchElementException("Reservation not found") }

        if (reservation.status != "confirmed") {
            throw IllegalArgumentException("Can only mark attendance on confirmed reservations")
        }

        // Must be in the past
        val reservationDateTime = LocalDateTime.of(reservation.date, reservation.startTime)
        if (reservationDateTime.isAfter(LocalDateTime.now(ZoneId.systemDefault()))) {
            throw IllegalArgumentException("Cannot mark attendance for future reservations")
        }

        val updated = reservation.copy(
            status = status,
            completedAt = if (status == "completed") Instant.now() else null
        )
        reservationRepository.save(updated)

        auditService.logReservationCancellation(
            adminId = adminId,
            adminEmail = adminEmail,
            reservationId = reservationId,
            userId = reservation.userId.toString(),
            refundCredits = false,
            creditsRefunded = 0
        )

        return reservationMapper.toDTO(updated)
    }

    fun getRefundPreview(userId: String, reservationId: String): CancellationRefundPreviewDTO {
        val reservation = reservationRepository.findById(UUID.fromString(reservationId))
            .orElseThrow { NoSuchElementException("Reservation not found") }

        if (reservation.userId.toString() != userId) {
            throw IllegalArgumentException("Access denied")
        }

        // Get trainer's cancellation policy
        val trainerId = reservation.slotId?.let { slotId ->
            slotRepository.findById(slotId).orElse(null)?.adminId
        }
        val policy = trainerId?.let { cancellationPolicyRepository.findByTrainerId(it) }

        // Calculate refund based on policy
        val reservationDateTime = LocalDateTime.of(reservation.date, reservation.startTime)
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val hoursUntil = ChronoUnit.MINUTES.between(now, reservationDateTime) / 60.0

        val (refundPercentage, policyApplied) = calculateRefundPercentage(policy, hoursUntil)
        val refundAmount = (reservation.creditsUsed * refundPercentage / 100.0).toInt()

        return CancellationRefundPreviewDTO(
            reservationId = reservationId,
            creditsUsed = reservation.creditsUsed,
            refundPercentage = refundPercentage,
            refundAmount = refundAmount,
            hoursUntilReservation = hoursUntil,
            policyApplied = policyApplied
        )
    }
}
