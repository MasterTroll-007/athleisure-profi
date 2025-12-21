package com.fitness.services

import com.fitness.models.*
import com.fitness.repositories.CreditRepository
import com.fitness.repositories.ReservationRepository
import com.fitness.repositories.UserRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

object ReservationService {

    fun createReservation(userId: String, request: CreateReservationRequest): ReservationDTO {
        val userUUID = UUID.fromString(userId)
        val blockUUID = UUID.fromString(request.blockId)
        val date = LocalDate.parse(request.date)
        val startTime = LocalTime.parse(request.startTime)
        val endTime = LocalTime.parse(request.endTime)

        // Validate slot is available (follows sticking rule)
        if (!AvailabilityService.validateSlotForReservation(date, startTime, endTime, blockUUID)) {
            throw IllegalArgumentException("Selected slot is not available")
        }

        // Get pricing item and credits needed
        val creditsNeeded = if (request.pricingItemId != null) {
            val pricingItem = CreditRepository.findPricingItemById(UUID.fromString(request.pricingItemId))
                ?: throw NoSuchElementException("Pricing item not found")
            pricingItem.credits
        } else {
            1 // Default to 1 credit
        }

        // Check user has enough credits
        val userCredits = UserRepository.getCredits(userUUID)
        if (userCredits < creditsNeeded) {
            throw IllegalStateException("Not enough credits. Required: $creditsNeeded, Available: $userCredits")
        }

        // Deduct credits
        UserRepository.updateCredits(userUUID, -creditsNeeded)

        // Record credit transaction
        CreditRepository.createTransaction(
            userId = userUUID,
            amount = -creditsNeeded,
            type = TransactionType.RESERVATION.value,
            referenceId = null, // Will be updated after reservation is created
            note = "Reservation for $date ${startTime}-${endTime}"
        )

        // Create reservation
        val reservation = ReservationRepository.create(
            userId = userUUID,
            blockId = blockUUID,
            date = date,
            startTime = startTime,
            endTime = endTime,
            creditsUsed = creditsNeeded,
            pricingItemId = request.pricingItemId?.let { UUID.fromString(it) }
        )

        return reservation
    }

    fun cancelReservation(userId: String, reservationId: String): ReservationDTO {
        val userUUID = UUID.fromString(userId)
        val reservationUUID = UUID.fromString(reservationId)

        val reservation = ReservationRepository.findById(reservationUUID)
            ?: throw NoSuchElementException("Reservation not found")

        // Check if user owns this reservation
        if (reservation.userId != userId) {
            throw SecurityException("You can only cancel your own reservations")
        }

        // Check if already cancelled
        if (reservation.status == "cancelled") {
            throw IllegalStateException("Reservation is already cancelled")
        }

        // Check 24-hour cancellation policy
        val reservationDateTime = LocalDateTime.parse("${reservation.date}T${reservation.startTime}")
        val now = LocalDateTime.now()
        val hoursUntilReservation = java.time.Duration.between(now, reservationDateTime).toHours()

        if (hoursUntilReservation < 24) {
            throw IllegalStateException("Reservations can only be cancelled at least 24 hours in advance")
        }

        // Cancel reservation
        ReservationRepository.cancel(reservationUUID)

        // Refund credits
        UserRepository.updateCredits(userUUID, reservation.creditsUsed)

        // Record refund transaction
        CreditRepository.createTransaction(
            userId = userUUID,
            amount = reservation.creditsUsed,
            type = TransactionType.REFUND.value,
            referenceId = reservationUUID,
            note = "Refund for cancelled reservation on ${reservation.date}"
        )

        return ReservationRepository.findById(reservationUUID)!!
    }

    fun getUserReservations(userId: String): List<ReservationDTO> {
        return ReservationRepository.findByUser(UUID.fromString(userId))
    }

    fun getUpcomingReservations(userId: String): List<ReservationDTO> {
        return ReservationRepository.findUpcomingByUser(UUID.fromString(userId))
    }

    fun getReservationById(reservationId: String): ReservationDTO {
        return ReservationRepository.findById(UUID.fromString(reservationId))
            ?: throw NoSuchElementException("Reservation not found")
    }

    // Admin functions
    fun getAllReservations(): List<ReservationDTO> {
        return ReservationRepository.getAll()
    }

    fun getReservationsByDate(date: LocalDate): List<ReservationDTO> {
        return ReservationRepository.findByDate(date)
    }

    fun getReservationsByDateRange(startDate: LocalDate, endDate: LocalDate): List<ReservationDTO> {
        return ReservationRepository.findByDateRange(startDate, endDate)
    }

    fun getCalendarEvents(startDate: LocalDate, endDate: LocalDate): List<ReservationCalendarEvent> {
        return ReservationRepository.getCalendarEvents(startDate, endDate)
    }

    fun adminCancelReservation(reservationId: String, refundCredits: Boolean = true): ReservationDTO {
        val reservationUUID = UUID.fromString(reservationId)

        val reservation = ReservationRepository.findById(reservationUUID)
            ?: throw NoSuchElementException("Reservation not found")

        if (reservation.status == "cancelled") {
            throw IllegalStateException("Reservation is already cancelled")
        }

        // Cancel reservation
        ReservationRepository.cancel(reservationUUID)

        if (refundCredits) {
            // Refund credits
            val userUUID = UUID.fromString(reservation.userId)
            UserRepository.updateCredits(userUUID, reservation.creditsUsed)

            // Record refund transaction
            CreditRepository.createTransaction(
                userId = userUUID,
                amount = reservation.creditsUsed,
                type = TransactionType.REFUND.value,
                referenceId = reservationUUID,
                note = "Admin cancelled reservation on ${reservation.date}"
            )
        }

        return ReservationRepository.findById(reservationUUID)!!
    }
}
