package com.fitness.service

import com.fitness.dto.CreateRecurringReservationRequest
import com.fitness.dto.RecurringReservationDTO
import com.fitness.entity.CreditTransaction
import com.fitness.entity.RecurringReservation
import com.fitness.entity.Reservation
import com.fitness.entity.SlotStatus
import com.fitness.entity.TransactionType
import com.fitness.repository.*

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class RecurringReservationService(
    private val recurringReservationRepository: RecurringReservationRepository,
    private val reservationRepository: ReservationRepository,
    private val slotRepository: SlotRepository,
    private val userRepository: UserRepository,
    private val pricingItemRepository: PricingItemRepository,
    private val creditTransactionRepository: CreditTransactionRepository,
    private val cancellationPolicyRepository: CancellationPolicyRepository
) {
    @Transactional
    fun createRecurringReservation(userId: String, request: CreateRecurringReservationRequest): RecurringReservationDTO {
        val userUUID = UUID.fromString(userId)
        val user = userRepository.findById(userUUID)
            .orElseThrow { NoSuchElementException("User not found") }

        if (user.isBlocked) {
            throw IllegalArgumentException("Your account has been blocked")
        }

        val startTime = LocalTime.parse(request.startTime)
        val endTime = LocalTime.parse(request.endTime)
        val dayOfWeek = DayOfWeek.of(request.dayOfWeek)

        // Calculate credits needed
        val creditsPerSession = request.pricingItemId?.let { piId ->
            pricingItemRepository.findById(UUID.fromString(piId))
                .orElseThrow { NoSuchElementException("Pricing item not found") }
                .credits
        } ?: 1
        val totalCredits = creditsPerSession * request.weeksCount

        // Find matching slots for each week
        val today = LocalDate.now()
        var nextDate = today.with(dayOfWeek)
        if (nextDate.isBefore(today) || nextDate.isEqual(today)) {
            nextDate = nextDate.plusWeeks(1)
        }

        val matchingSlots = mutableListOf<Pair<LocalDate, com.fitness.entity.Slot>>()
        for (i in 0 until request.weeksCount) {
            val date = nextDate.plusWeeks(i.toLong())
            val slot = slotRepository.findByDateAndStartTime(date, startTime)
            if (slot != null && slot.status in listOf(SlotStatus.UNLOCKED, SlotStatus.LOCKED)) {
                val currentBookings = reservationRepository.findByDateAndSlotId(date, slot.id).size
                if (currentBookings < slot.capacity) {
                    matchingSlots.add(date to slot)
                }
            }
        }

        if (matchingSlots.size < request.weeksCount) {
            throw IllegalArgumentException("Not all weeks have available slots (found ${matchingSlots.size} of ${request.weeksCount})")
        }

        // Atomically deduct all credits
        val rowsUpdated = userRepository.deductCreditsIfSufficient(userUUID, totalCredits)
        if (rowsUpdated == 0) {
            throw IllegalArgumentException("Not enough credits (need $totalCredits)")
        }

        // Create recurring reservation record
        val startDate = matchingSlots.first().first
        val endDate = matchingSlots.last().first
        val recurring = recurringReservationRepository.save(
            RecurringReservation(
                userId = userUUID,
                dayOfWeek = request.dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                weeksCount = request.weeksCount,
                startDate = startDate,
                endDate = endDate,
                pricingItemId = request.pricingItemId?.let { UUID.fromString(it) }
            )
        )

        // Create individual reservations
        val reservationIds = mutableListOf<UUID>()
        for ((date, slot) in matchingSlots) {
            val reservation = reservationRepository.save(
                Reservation(
                    userId = userUUID,
                    slotId = slot.id,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    creditsUsed = creditsPerSession,
                    pricingItemId = request.pricingItemId?.let { UUID.fromString(it) },
                    recurringReservationId = recurring.id
                )
            )
            reservationIds.add(reservation.id)

            // Update slot status if full
            val newBookings = reservationRepository.findByDateAndSlotId(date, slot.id).size
            if (newBookings >= slot.capacity) {
                val updatedSlot = slot.copy(status = SlotStatus.RESERVED)
                slotRepository.save(updatedSlot)
            }
        }

        // Record credit transaction
        creditTransactionRepository.save(
            CreditTransaction(
                userId = userUUID,
                amount = -totalCredits,
                type = TransactionType.RESERVATION.value,
                referenceId = recurring.id,
                note = "Opakovaná rezervace (${ request.weeksCount} týdnů)"
            )
        )

        return toDTO(recurring, reservationIds.map { it.toString() })
    }

    fun getMyRecurringReservations(userId: String): List<RecurringReservationDTO> {
        val userUUID = UUID.fromString(userId)
        val recurring = recurringReservationRepository.findByUserId(userUUID)
        return recurring.map { r ->
            val reservations = reservationRepository.findByUserId(userUUID)
                .filter { it.recurringReservationId == r.id }
            toDTO(r, reservations.map { it.id.toString() })
        }
    }

    @Transactional
    fun cancelRecurringReservation(userId: String, recurringId: String): RecurringReservationDTO {
        val userUUID = UUID.fromString(userId)
        val recurringUUID = UUID.fromString(recurringId)
        val recurring = recurringReservationRepository.findById(recurringUUID)
            .orElseThrow { NoSuchElementException("Recurring reservation not found") }

        if (recurring.userId != userUUID) {
            throw IllegalArgumentException("Access denied")
        }

        if (recurring.status == "cancelled") {
            throw IllegalArgumentException("Already cancelled")
        }

        // Cancel future reservations and refund
        val today = LocalDate.now()
        val reservations = reservationRepository.findByUserId(userUUID)
            .filter { it.recurringReservationId == recurringUUID && it.status == "confirmed" && it.date.isAfter(today) }

        var totalRefund = 0
        for (reservation in reservations) {
            val updated = reservation.copy(status = "cancelled", cancelledAt = Instant.now())
            reservationRepository.save(updated)

            // Apply cancellation policy per reservation
            val trainerId = reservation.slotId?.let { slotId ->
                slotRepository.findById(slotId).orElse(null)?.adminId
            }
            val policy = trainerId?.let { cancellationPolicyRepository.findByTrainerId(it) }
            val reservationDateTime = LocalDateTime.of(reservation.date, reservation.startTime)
            val hoursUntil = ChronoUnit.MINUTES.between(LocalDateTime.now(ZoneId.systemDefault()), reservationDateTime) / 60.0

            val refundPercentage = when {
                policy == null || !policy.isActive -> 100
                hoursUntil >= policy.fullRefundHours -> 100
                policy.partialRefundHours != null && policy.partialRefundPercentage != null && hoursUntil >= policy.partialRefundHours -> policy.partialRefundPercentage
                else -> 0
            }
            val refundAmount = (reservation.creditsUsed * refundPercentage / 100.0).toInt()
            totalRefund += refundAmount

            // Unlock slot if it was full
            reservation.slotId?.let { slotId ->
                slotRepository.findById(slotId).ifPresent { slot ->
                    if (slot.status == SlotStatus.RESERVED) {
                        slotRepository.save(slot.copy(status = SlotStatus.UNLOCKED))
                    }
                }
            }
        }

        if (totalRefund > 0) {
            userRepository.updateCredits(userUUID, totalRefund)
            creditTransactionRepository.save(
                CreditTransaction(
                    userId = userUUID,
                    amount = totalRefund,
                    type = TransactionType.REFUND.value,
                    referenceId = recurringUUID,
                    note = "Zrušení opakované rezervace - vrácení $totalRefund kreditů"
                )
            )
        }

        val updated = recurring.copy(status = "cancelled")
        recurringReservationRepository.save(updated)

        val allReservations = reservationRepository.findByUserId(userUUID)
            .filter { it.recurringReservationId == recurringUUID }

        return toDTO(updated, allReservations.map { it.id.toString() })
    }

    private fun toDTO(recurring: RecurringReservation, reservationIds: List<String>) = RecurringReservationDTO(
        id = recurring.id.toString(),
        userId = recurring.userId.toString(),
        dayOfWeek = recurring.dayOfWeek,
        startTime = recurring.startTime.toString(),
        endTime = recurring.endTime.toString(),
        weeksCount = recurring.weeksCount,
        startDate = recurring.startDate.toString(),
        endDate = recurring.endDate.toString(),
        status = recurring.status,
        pricingItemId = recurring.pricingItemId?.toString(),
        reservationIds = reservationIds,
        createdAt = recurring.createdAt.toString()
    )
}
