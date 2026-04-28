package com.fitness.service

import com.fitness.dto.*
import com.fitness.entity.CreditTransaction
import com.fitness.entity.Reservation
import com.fitness.entity.Slot
import com.fitness.entity.SlotPricingItem
import com.fitness.entity.SlotStatus
import com.fitness.entity.TransactionType
import com.fitness.mapper.ReservationMapper
import com.fitness.repository.*
import org.springframework.data.domain.Pageable
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
    private val emailService: EmailService
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(ReservationService::class.java)

    private fun requireReservationOwnedByAdmin(reservation: Reservation, adminId: UUID) {
        val owned = reservation.slotId?.let { slotId ->
            slotRepository.findById(slotId).orElse(null)?.adminId == adminId
        } ?: (userRepository.findById(reservation.userId).orElse(null)?.trainerId == adminId)
        if (!owned) {
            throw org.springframework.security.access.AccessDeniedException("Access denied")
        }
    }

    private fun requireSlotMatchesRequest(
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

    private fun resolveCreditsNeeded(slotId: UUID, pricingItemId: String?, trainerId: UUID): Pair<Int, UUID?> {
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
            throw org.springframework.security.access.AccessDeniedException("Selected training type does not belong to your trainer")
        }

        if (slotPricingItems.isNotEmpty() && slotPricingItems.none { it.pricingItemId == pricingItem.id }) {
            throw IllegalArgumentException("Selected training type is not available for this slot")
        }

        return pricingItem.credits to pricingItemUUID
    }

    private fun findConfirmedReservationsForTrainerDate(date: LocalDate, trainerId: UUID): List<Reservation> {
        val trainerSlotIds = slotRepository.findByDateAndAdminId(date, trainerId)
            .mapNotNull { it.id }
        if (trainerSlotIds.isEmpty()) return emptyList()
        return reservationRepository.findConfirmedByDateAndSlotIdIn(date, trainerSlotIds)
    }

    private fun isAdjacentSlotAllowed(slot: Slot, confirmedReservations: List<Reservation>): Boolean {
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

    @Transactional
    fun createReservation(userId: String, request: CreateReservationRequest): ReservationDTO {
        val userUUID = UUID.fromString(userId)
        val user = userRepository.findByIdForUpdate(userUUID)
            ?: throw NoSuchElementException("User not found")

        val slotId = UUID.fromString(request.slotId)
        val lockedSlot = slotRepository.findByIdForUpdate(slotId)
            ?: throw NoSuchElementException("Slot not found")

        // Check if user is blocked
        if (user.isBlocked) {
            throw IllegalArgumentException("Your account has been blocked. Contact your trainer for more information.")
        }

        val trainerId = user.trainerId
            ?: throw IllegalArgumentException("Your account is not assigned to a trainer")
        if (lockedSlot.adminId != trainerId) {
            throw org.springframework.security.access.AccessDeniedException("Selected slot does not belong to your trainer")
        }
        if (lockedSlot.status != SlotStatus.UNLOCKED) {
            throw IllegalArgumentException("This slot is not available for booking")
        }

        val date = LocalDate.parse(request.date)
        val startTime = LocalTime.parse(request.startTime)
        val endTime = LocalTime.parse(request.endTime)
        requireSlotMatchesRequest(lockedSlot.date, lockedSlot.startTime, lockedSlot.endTime, date, startTime, endTime)

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

        val (creditsNeeded, resolvedPricingItemId) = resolveCreditsNeeded(slotId, request.pricingItemId, trainerId)

        // Check availability atomically (under pessimistic lock)
        val currentBookings = reservationRepository.countConfirmedByDateAndSlotId(date, slotId)
        if (currentBookings >= lockedSlot.capacity) {
            throw IllegalArgumentException("This slot is fully booked")
        }

        val trainer = userRepository.findById(trainerId).orElse(null)
        val adjacentRequired = trainer?.adjacentBookingRequired ?: true
        val confirmedTrainerReservations = findConfirmedReservationsForTrainerDate(date, trainerId)

        if (confirmedTrainerReservations.any { it.userId == userUUID && it.slotId == slotId }) {
            throw IllegalArgumentException("You already have a reservation for this slot")
        }

        if (adjacentRequired && !isAdjacentSlotAllowed(lockedSlot, confirmedTrainerReservations)) {
            throw IllegalArgumentException("This slot must be adjacent to an existing reservation")
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
                pricingItemId = resolvedPricingItemId
            )
        )

        // Update slot status to RESERVED only if now full (capacity reached).
        // Use the pessimistically-locked row (lockedSlot) so a concurrent
        // writer's changes aren't clobbered by a stale copy.
        if (currentBookings + 1 >= lockedSlot.capacity) {
            slotRepository.save(lockedSlot.copy(status = SlotStatus.RESERVED))
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
        notifyTrainerNewReservation(lockedSlot.adminId, user, date, startTime, endTime)

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

    fun getUserReservationsPage(userId: String, scope: String, pageable: Pageable): PageDTO<ReservationDTO> {
        val userUUID = UUID.fromString(userId)
        val today = LocalDate.now()
        val now = LocalTime.now()
        val page = when (scope.lowercase()) {
            "upcoming" -> reservationRepository.findUpcomingByUserId(userUUID, today, now, pageable)
            "past" -> reservationRepository.findPastByUserId(userUUID, today, now, pageable)
            else -> reservationRepository.findByUserIdOrderByDateDescStartTimeDesc(userUUID, pageable)
        }
        return page.toPageDTO { reservation ->
            reservationMapper.toDTO(reservation)
        }
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
        val reservation = reservationRepository.findByIdForUpdate(UUID.fromString(reservationId))
            ?: throw NoSuchElementException("Reservation not found")

        if (reservation.userId.toString() != userId) {
            throw org.springframework.security.access.AccessDeniedException("Access denied")
        }

        val reservationInstant = java.time.LocalDateTime.of(reservation.date, reservation.startTime)
        if (reservationInstant.isBefore(java.time.LocalDateTime.now())) {
            throw IllegalArgumentException("Cannot cancel a past reservation")
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
            val slot = slotRepository.findByIdForUpdate(slotId)
            if (slot != null) {
                val currentBookings = reservationRepository.countConfirmedByDateAndSlotId(reservation.date, slotId)
                if (slot.status == SlotStatus.RESERVED && currentBookings < slot.capacity) {
                    slotRepository.save(slot.copy(status = SlotStatus.UNLOCKED))
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

    fun getAllReservations(startDate: LocalDate, endDate: LocalDate, adminId: String): List<ReservationCalendarEvent> {
        val reservations = reservationRepository.findByDateRangeForAdmin(startDate, endDate, UUID.fromString(adminId))
        return reservationMapper.toCalendarEventBatch(reservations)
    }

    /**
     * Admin creates a reservation for a user.
     * Optionally deducts credits from the user.
     */
    @Transactional
    fun adminCreateReservation(request: AdminCreateReservationRequest, adminId: String? = null, adminEmail: String? = null): ReservationDTO {
        val userUUID = UUID.fromString(request.userId)
        val user = userRepository.findByIdForUpdate(userUUID)
            ?: throw NoSuchElementException("User not found")
        val adminUUID = adminId?.let { UUID.fromString(it) }
        if (adminUUID != null && user.trainerId != adminUUID) {
            throw org.springframework.security.access.AccessDeniedException("Client does not belong to this trainer")
        }

        val slotId = UUID.fromString(request.slotId)
        // Pessimistic lock on the slot row so two concurrent admin bookings
        // can't pass the capacity check simultaneously.
        val slot = slotRepository.findByIdForUpdate(slotId)
            ?: throw NoSuchElementException("Slot not found")

        val date = LocalDate.parse(request.date)
        val startTime = LocalTime.parse(request.startTime)
        val endTime = LocalTime.parse(request.endTime)
        requireSlotMatchesRequest(slot.date, slot.startTime, slot.endTime, date, startTime, endTime)
        if (adminUUID != null && slot.adminId != adminUUID) {
            throw org.springframework.security.access.AccessDeniedException("Selected slot does not belong to this trainer")
        }
        if (slot.status == SlotStatus.BLOCKED) {
            throw IllegalArgumentException("This slot is blocked")
        }

        // Validate date boundaries - admin can book up to 365 days in advance
        val today = LocalDate.now()
        if (LocalDateTime.of(date, startTime).isBefore(LocalDateTime.now(ZoneId.systemDefault()))) {
            throw IllegalArgumentException("Cannot create reservation for a past time")
        }
        val maxFutureDate = today.plusDays(365)
        if (date.isAfter(maxFutureDate)) {
            throw IllegalArgumentException("Cannot create reservation more than 365 days in advance")
        }

        // Capacity check under the pessimistic lock
        val currentBookings = reservationRepository.countConfirmedByDateAndSlotId(date, slotId)
        if (currentBookings >= slot.capacity) {
            throw IllegalArgumentException("This slot is already booked")
        }

        val trainerIdForPricing = adminUUID ?: slot.adminId ?: user.trainerId
            ?: throw IllegalArgumentException("Unable to resolve trainer for this reservation")
        val (creditsToDeduct, resolvedPricingItemId) = if (request.deductCredits) {
            resolveCreditsNeeded(slotId, request.pricingItemId, trainerIdForPricing)
        } else {
            0 to null
        }

        if (request.deductCredits && user.credits < creditsToDeduct) {
            throw IllegalArgumentException("User does not have enough credits")
        }

        // Create reservation
        val reservation = reservationRepository.save(
            Reservation(
                userId = userUUID,
                slotId = slotId,
                date = date,
                startTime = startTime,
                endTime = endTime,
                creditsUsed = creditsToDeduct,
                pricingItemId = resolvedPricingItemId,
                note = request.note
            )
        )

        // Mark reserved only when the reservation fills the slot. Capacity > 1
        // slots must stay bookable until they actually reach capacity.
        if (currentBookings + 1 >= slot.capacity) {
            slotRepository.save(slot.copy(status = SlotStatus.RESERVED))
        } else if (slot.status == SlotStatus.RESERVED) {
            slotRepository.save(slot.copy(status = SlotStatus.UNLOCKED))
        }

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
        val reservation = reservationRepository.findByIdForUpdate(UUID.fromString(reservationId))
            ?: throw NoSuchElementException("Reservation not found")
        val adminUUID = adminId?.let { UUID.fromString(it) }
        if (adminUUID != null) {
            requireReservationOwnedByAdmin(reservation, adminUUID)
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
            val slot = slotRepository.findByIdForUpdate(slotId)
            if (slot != null) {
                val currentBookings = reservationRepository.countConfirmedByDateAndSlotId(reservation.date, slotId)
                if (currentBookings < slot.capacity && slot.status == SlotStatus.RESERVED) {
                    slotRepository.save(slot.copy(status = SlotStatus.UNLOCKED))
                }
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

    @Transactional
    fun adminRescheduleReservation(
        reservationId: String,
        request: AdminRescheduleReservationRequest,
        adminId: String
    ): ReservationDTO {
        val adminUUID = UUID.fromString(adminId)
        val reservation = reservationRepository.findByIdForUpdate(UUID.fromString(reservationId))
            ?: throw NoSuchElementException("Reservation not found")
        requireReservationOwnedByAdmin(reservation, adminUUID)

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

        if (reservationRepository.findByUserId(reservation.userId).any {
                it.id != reservation.id && it.status == "confirmed" && it.date == targetDate
            }) {
            throw IllegalArgumentException("Client already has a confirmed reservation on the target date")
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
            requireSlotMatchesRequest(requestedSlot.date, requestedSlot.startTime, requestedSlot.endTime, targetDate, targetStart, targetEnd)
            if (requestedSlot.adminId != adminId) {
                throw org.springframework.security.access.AccessDeniedException("Target slot does not belong to this trainer")
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

        val durationMinutes = java.time.Duration.between(targetStart, targetEnd).toMinutes().toInt()
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

    /**
     * Update the note on a reservation.
     */
    @Transactional
    fun updateReservationNote(reservationId: String, note: String?, adminId: String): ReservationDTO {
        val reservation = reservationRepository.findById(UUID.fromString(reservationId))
            .orElseThrow { NoSuchElementException("Reservation not found") }
        requireReservationOwnedByAdmin(reservation, UUID.fromString(adminId))

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
        val reservation = reservationRepository.findByIdForUpdate(UUID.fromString(reservationId))
            ?: throw NoSuchElementException("Reservation not found")
        requireReservationOwnedByAdmin(reservation, UUID.fromString(adminId))

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
