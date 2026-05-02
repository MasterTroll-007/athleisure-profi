package com.fitness.service

import com.fitness.dto.AdminRescheduleReservationRequest
import com.fitness.dto.ReservationDTO
import com.fitness.entity.Slot
import com.fitness.entity.SlotPricingItem
import com.fitness.entity.SlotStatus
import com.fitness.mapper.ReservationMapper
import com.fitness.repository.ReservationRepository
import com.fitness.repository.SlotPricingItemRepository
import com.fitness.repository.SlotRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.NoSuchElementException
import java.util.UUID

@Service
class ReservationRescheduleService(
    private val reservationRepository: ReservationRepository,
    private val slotRepository: SlotRepository,
    private val slotPricingItemRepository: SlotPricingItemRepository,
    private val reservationMapper: ReservationMapper,
    private val reservationPolicy: ReservationPolicyService
) {
    @Transactional
    fun adminRescheduleReservation(
        reservationId: String,
        request: AdminRescheduleReservationRequest,
        adminId: String
    ): ReservationDTO {
        val adminUUID = UUID.fromString(adminId)
        val reservation = reservationRepository.findByIdForUpdate(UUID.fromString(reservationId))
            ?: throw NoSuchElementException("Reservation not found")
        reservationPolicy.requireReservationOwnedByAdmin(reservation, adminUUID)

        if (reservation.status != "confirmed") {
            throw IllegalArgumentException("Only confirmed reservations can be rescheduled")
        }

        val targetDate = LocalDate.parse(request.date)
        val targetStart = LocalTime.parse(request.startTime)
        val targetEnd = LocalTime.parse(request.endTime)
        if (targetEnd <= targetStart) {
            throw IllegalArgumentException("Target end time must be after start time")
        }
        if (LocalDateTime.of(targetDate, targetStart).isBefore(LocalDateTime.now(ZoneId.systemDefault()))) {
            throw IllegalArgumentException("Cannot reschedule to the past")
        }

        val oldSlot = reservation.slotId?.let { slotRepository.findByIdForUpdate(it) }
        val targetSlot = resolveTargetSlotForReschedule(
            request = request,
            adminId = adminUUID,
            targetDate = targetDate,
            targetStart = targetStart,
            targetEnd = targetEnd,
            sourceSlot = oldSlot
        )

        if (targetSlot.id == reservation.slotId && reservation.date == targetDate && reservation.startTime == targetStart) {
            return reservationMapper.toDTO(reservation)
        }

        val targetPricingItems = slotPricingItemRepository.findBySlotId(targetSlot.id!!)
        if (reservation.pricingItemId != null &&
            targetPricingItems.isNotEmpty() &&
            targetPricingItems.none { it.pricingItemId == reservation.pricingItemId }) {
            throw IllegalArgumentException("Target slot does not offer this reservation's training type")
        }

        val currentBookings = reservationRepository.countConfirmedByDateAndSlotId(targetDate, targetSlot.id)
        if (currentBookings >= targetSlot.capacity) {
            throw IllegalArgumentException("Target slot is already full")
        }

        val updated = reservation.copy(
            slotId = targetSlot.id,
            blockId = null,
            date = targetDate,
            startTime = targetStart,
            endTime = targetEnd
        )
        reservationRepository.save(updated)

        oldSlot?.let { slot ->
            val remainingBookings = reservationRepository.countConfirmedByDateAndSlotId(slot.date, slot.id!!)
            if (slot.status == SlotStatus.RESERVED && remainingBookings < slot.capacity) {
                slotRepository.save(slot.copy(status = SlotStatus.UNLOCKED))
            }
        }

        val targetBookings = reservationRepository.countConfirmedByDateAndSlotId(targetDate, targetSlot.id)
        if (targetBookings >= targetSlot.capacity && targetSlot.status != SlotStatus.RESERVED) {
            slotRepository.save(targetSlot.copy(status = SlotStatus.RESERVED))
        }

        return reservationMapper.toDTO(updated)
    }

    private fun resolveTargetSlotForReschedule(
        request: AdminRescheduleReservationRequest,
        adminId: UUID,
        targetDate: LocalDate,
        targetStart: LocalTime,
        targetEnd: LocalTime,
        sourceSlot: Slot?
    ): Slot {
        val requestedSlot = request.targetSlotId?.let { slotRepository.findByIdForUpdate(UUID.fromString(it)) }
            ?: slotRepository.findByDateAndStartTimeAndAdminId(targetDate, targetStart, adminId)
                ?.let { slotRepository.findByIdForUpdate(it.id!!) }

        if (requestedSlot != null) {
            reservationPolicy.requireSlotMatchesRequest(requestedSlot.date, requestedSlot.startTime, requestedSlot.endTime, targetDate, targetStart, targetEnd)
            if (requestedSlot.adminId != adminId) {
                throw AccessDeniedException("Target slot does not belong to this trainer")
            }
            if (requestedSlot.status == SlotStatus.BLOCKED) {
                throw IllegalArgumentException("Target slot cannot be used")
            }
            return requestedSlot
        }

        if (!request.createSlotIfMissing) {
            throw NoSuchElementException("Target slot not found")
        }
        if (slotRepository.existsOverlappingSlotForAdmin(targetDate, targetStart, targetEnd, adminId, null)) {
            throw IllegalArgumentException("Target time overlaps with an existing slot")
        }

        val durationMinutes = Duration.between(targetStart, targetEnd).toMinutes().toInt()
        val created = slotRepository.save(
            Slot(
                date = targetDate,
                startTime = targetStart,
                endTime = targetEnd,
                durationMinutes = durationMinutes,
                status = SlotStatus.UNLOCKED,
                adminId = adminId,
                capacity = sourceSlot?.capacity ?: 1,
                locationId = sourceSlot?.locationId
            )
        )

        sourceSlot?.id?.let { sourceSlotId ->
            slotPricingItemRepository.findBySlotId(sourceSlotId).forEach { pricing ->
                slotPricingItemRepository.save(
                    SlotPricingItem(slotId = created.id!!, pricingItemId = pricing.pricingItemId)
                )
            }
        }

        return created
    }
}
