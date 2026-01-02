package com.fitness.service

import com.fitness.dto.*
import com.fitness.entity.CreditTransaction
import com.fitness.entity.Reservation
import com.fitness.entity.SlotStatus
import com.fitness.entity.TransactionType
import com.fitness.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@Service
class ReservationService(
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val slotRepository: SlotRepository,
    private val pricingItemRepository: PricingItemRepository,
    private val creditTransactionRepository: CreditTransactionRepository
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

        return reservation.toDTO(user.firstName, user.lastName, user.email, null)
    }

    fun getUserReservations(userId: String): List<ReservationDTO> {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { NoSuchElementException("User not found") }

        return reservationRepository.findByUserId(UUID.fromString(userId))
            .map { it.toDTO(user.firstName, user.lastName, user.email, getPricingItemName(it.pricingItemId)) }
    }

    fun getUpcomingReservations(userId: String): List<ReservationDTO> {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { NoSuchElementException("User not found") }

        return reservationRepository.findUpcomingByUserId(UUID.fromString(userId), LocalDate.now())
            .map { it.toDTO(user.firstName, user.lastName, user.email, getPricingItemName(it.pricingItemId)) }
    }

    fun getReservationById(id: String): ReservationDTO {
        val reservation = reservationRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Reservation not found") }

        val user = userRepository.findById(reservation.userId)
            .orElseThrow { NoSuchElementException("User not found") }

        return reservation.toDTO(user.firstName, user.lastName, user.email, getPricingItemName(reservation.pricingItemId))
    }

    @Transactional
    fun cancelReservation(userId: String, reservationId: String): ReservationDTO {
        val reservation = reservationRepository.findById(UUID.fromString(reservationId))
            .orElseThrow { NoSuchElementException("Reservation not found") }

        if (reservation.userId.toString() != userId) {
            throw IllegalArgumentException("Access denied")
        }

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

        // Refund credits
        userRepository.updateCredits(reservation.userId, reservation.creditsUsed)

        creditTransactionRepository.save(
            CreditTransaction(
                userId = reservation.userId,
                amount = reservation.creditsUsed,
                type = TransactionType.REFUND.value,
                referenceId = reservation.id,
                note = "Vrácení kreditu za zrušenou rezervaci"
            )
        )

        val user = userRepository.findById(reservation.userId)
            .orElseThrow { NoSuchElementException("User not found") }

        return updated.toDTO(user.firstName, user.lastName, user.email, getPricingItemName(reservation.pricingItemId))
    }

    fun getAllReservations(startDate: LocalDate, endDate: LocalDate): List<ReservationCalendarEvent> {
        return reservationRepository.findByDateRange(startDate, endDate).map { reservation ->
            val user = userRepository.findById(reservation.userId).orElse(null)
            ReservationCalendarEvent(
                id = reservation.id.toString(),
                title = user?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim() } ?: "Unknown",
                start = "${reservation.date}T${reservation.startTime}",
                end = "${reservation.date}T${reservation.endTime}",
                status = reservation.status,
                clientName = user?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim() },
                clientEmail = user?.email
            )
        }
    }

    /**
     * Admin creates a reservation for a user.
     * Optionally deducts credits from the user.
     */
    @Transactional
    fun adminCreateReservation(request: AdminCreateReservationRequest): ReservationDTO {
        val userUUID = UUID.fromString(request.userId)
        val user = userRepository.findById(userUUID)
            .orElseThrow { NoSuchElementException("User not found") }

        val blockId = UUID.fromString(request.blockId)
        val slot = slotRepository.findById(blockId)
            .orElseThrow { NoSuchElementException("Slot not found") }

        val date = LocalDate.parse(request.date)
        val startTime = LocalTime.parse(request.startTime)
        val endTime = LocalTime.parse(request.endTime)

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

        return reservation.toDTO(user.firstName, user.lastName, user.email, null)
    }

    /**
     * Admin cancels any reservation.
     * Optionally refunds credits to the user.
     */
    @Transactional
    fun adminCancelReservation(reservationId: String, refundCredits: Boolean): ReservationDTO {
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
        if (refundCredits && reservation.creditsUsed > 0) {
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
        }

        val user = userRepository.findById(reservation.userId)
            .orElseThrow { NoSuchElementException("User not found") }

        return updated.toDTO(user.firstName, user.lastName, user.email, getPricingItemName(reservation.pricingItemId))
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

        val user = userRepository.findById(reservation.userId)
            .orElseThrow { NoSuchElementException("User not found") }

        return updated.toDTO(user.firstName, user.lastName, user.email, getPricingItemName(reservation.pricingItemId))
    }

    private fun getPricingItemName(id: UUID?): String? {
        return id?.let { pricingItemRepository.findById(it).orElse(null)?.nameCs }
    }

    private fun Reservation.toDTO(firstName: String?, lastName: String?, email: String?, pricingItemName: String?) = ReservationDTO(
        id = id.toString(),
        userId = userId.toString(),
        userName = "${firstName ?: ""} ${lastName ?: ""}".trim().ifEmpty { null },
        userEmail = email,
        blockId = blockId?.toString(),
        date = date.toString(),
        startTime = startTime.toString(),
        endTime = endTime.toString(),
        status = status,
        creditsUsed = creditsUsed,
        pricingItemId = pricingItemId?.toString(),
        pricingItemName = pricingItemName,
        createdAt = createdAt.toString(),
        cancelledAt = cancelledAt?.toString(),
        note = note
    )
}
