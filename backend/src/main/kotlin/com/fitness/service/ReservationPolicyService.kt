package com.fitness.service

import com.fitness.entity.CancellationPolicy
import com.fitness.entity.Reservation
import com.fitness.entity.Slot
import com.fitness.repository.PricingItemRepository
import com.fitness.repository.ReservationRepository
import com.fitness.repository.SlotPricingItemRepository
import com.fitness.repository.SlotRepository
import com.fitness.repository.UserRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Service
class ReservationPolicyService(
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val slotRepository: SlotRepository,
    private val pricingItemRepository: PricingItemRepository,
    private val slotPricingItemRepository: SlotPricingItemRepository
) {
    fun requireReservationOwnedByAdmin(reservation: Reservation, adminId: UUID) {
        val owned = reservation.slotId?.let { slotId ->
            slotRepository.findById(slotId).orElse(null)?.adminId == adminId
        } ?: (userRepository.findById(reservation.userId).orElse(null)?.trainerId == adminId)
        if (!owned) {
            throw AccessDeniedException("Access denied")
        }
    }

    fun requireSlotMatchesRequest(
        slotDate: LocalDate,
        slotStartTime: LocalTime,
        slotEndTime: LocalTime,
        requestDate: LocalDate,
        requestStartTime: LocalTime,
        requestEndTime: LocalTime
    ) {
        if (slotDate != requestDate || slotStartTime != requestStartTime || slotEndTime != requestEndTime) {
            throw IllegalArgumentException("Reservation time does not match the selected slot")
        }
    }

    fun resolveCreditsNeeded(slotId: UUID, pricingItemId: String?, trainerId: UUID): Pair<Int, UUID?> {
        val slotPricingItems = slotPricingItemRepository.findBySlotId(slotId)
        val pricingItemUUID = when {
            pricingItemId != null -> UUID.fromString(pricingItemId)
            slotPricingItems.size == 1 -> slotPricingItems.single().pricingItemId
            slotPricingItems.size > 1 -> throw IllegalArgumentException("Please select a training type for this slot")
            else -> return 1 to null
        }

        val pricingItem = pricingItemRepository.findById(pricingItemUUID)
            .orElseThrow { NoSuchElementException("Pricing item not found") }

        if (!pricingItem.isActive) {
            throw IllegalArgumentException("Selected training type is no longer active")
        }
        if (pricingItem.adminId != trainerId) {
            throw AccessDeniedException("Selected training type does not belong to your trainer")
        }

        if (slotPricingItems.isNotEmpty() && slotPricingItems.none { it.pricingItemId == pricingItem.id }) {
            throw IllegalArgumentException("Selected training type is not available for this slot")
        }

        return pricingItem.credits to pricingItemUUID
    }

    fun findConfirmedReservationsForTrainerDate(date: LocalDate, trainerId: UUID): List<Reservation> {
        val trainerSlotIds = slotRepository.findByDateAndAdminId(date, trainerId)
            .mapNotNull { it.id }
        if (trainerSlotIds.isEmpty()) return emptyList()
        return reservationRepository.findConfirmedByDateAndSlotIdIn(date, trainerSlotIds)
    }

    fun isAdjacentSlotAllowed(slot: Slot, confirmedReservations: List<Reservation>): Boolean {
        if (confirmedReservations.isEmpty()) return true

        val slotId = slot.id
        if (slotId != null && confirmedReservations.any { it.slotId == slotId }) {
            return true
        }

        val durationMinutes = slot.durationMinutes.toLong()
        return confirmedReservations.any { reservation ->
            slot.startTime == reservation.startTime.minusMinutes(durationMinutes) ||
                slot.startTime == reservation.endTime
        }
    }

    fun calculateRefundPercentage(policy: CancellationPolicy?, hoursUntil: Double): Pair<Int, String> {
        if (policy == null || !policy.isActive) {
            return 100 to "NO_POLICY"
        }

        return when {
            hoursUntil >= policy.fullRefundHours -> 100 to "FULL_REFUND"
            policy.partialRefundHours != null &&
                policy.partialRefundPercentage != null &&
                hoursUntil >= policy.partialRefundHours ->
                    policy.partialRefundPercentage to "PARTIAL_REFUND"
            else -> 0 to "NO_REFUND"
        }
    }
}
