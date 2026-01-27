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
    private val creditTransactionRepository: CreditTransactionRepository,
    private val cancellationPolicyRepository: CancellationPolicyRepository,
    private val reservationMapper: ReservationMapper,
    private val auditService: AuditService
) {

    @Transactional
    fun createReservation(userId: String, request: CreateReservationRequest): ReservationDTO {
        val userUUID = UUID.fromString(userId)
        val user = userRepository.findById(userUUID)
            .orElseThrow { NoSuchElementException("User not found") }

        val slotId = UUID.fromString(request.blockId)
        val slot = slotRepository.findById(slotId)
            .orElseThrow { NoSuchElementException("Slot not found") }

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

        // Check credits
        val creditsNeeded = request.pricingItemId?.let {
            pricingItemRepository.findById(UUID.fromString(it))
                .orElseThrow { NoSuchElementException("Pricing item not found") }
                .credits
        } ?: 1

        if (user.credits < creditsNeeded) {
            throw IllegalArgumentException("Not enough credits")
        }

        // Check availability
        val existingReservations = reservationRepository.findByDateAndSlotId(date, slotId)
        if (existingReservations.any { it.startTime == startTime }) {
            throw IllegalArgumentException("This slot is already booked")
        }

        // Check if user already has a reservation on this date (max 1 per day)
        if (reservationRepository.existsByUserIdAndDateConfirmed(userUUID, date)) {
            throw IllegalArgumentException("Již máte rezervaci na tento den. Maximálně jedna rezervace denně.")
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

        // Update slot status to RESERVED
        val updatedSlot = slot.copy(status = SlotStatus.RESERVED)
        slotRepository.save(updatedSlot)

        // Deduct credits
        userRepository.updateCredits(userUUID, -creditsNeeded)

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

        // Update slot status back to UNLOCKED
        reservation.slotId?.let { slotId ->
            slotRepository.findById(slotId).ifPresent { slot ->
                val updatedSlot = slot.copy(status = SlotStatus.UNLOCKED)
                slotRepository.save(updatedSlot)
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
