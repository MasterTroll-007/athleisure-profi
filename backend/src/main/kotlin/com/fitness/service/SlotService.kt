package com.fitness.service

import com.fitness.dto.*
import com.fitness.entity.CreditTransaction
import com.fitness.entity.Reservation
import com.fitness.entity.Slot
import com.fitness.entity.SlotStatus
import com.fitness.entity.TransactionType
import com.fitness.entity.displayName
import com.fitness.repository.CreditTransactionRepository
import com.fitness.repository.ReservationRepository
import com.fitness.repository.SlotPricingItemRepository
import com.fitness.repository.SlotRepository
import com.fitness.repository.TrainingLocationRepository
import com.fitness.repository.UserRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

data class CalendarRangeCleanupResult(
    val deletedSlots: Int,
    val cancelledReservations: Int,
    val refundedCredits: Int
)

@Service
class SlotService(
    private val slotRepository: SlotRepository,
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val slotPricingItemRepository: SlotPricingItemRepository,
    private val creditTransactionRepository: CreditTransactionRepository,
    private val locationRepository: TrainingLocationRepository,
    private val emailService: EmailService,
    private val auditService: AuditService,
    private val entityManager: EntityManager,
    private val slotPolicy: SlotPolicyService,
    private val slotPricingService: SlotPricingService,
    private val slotDtoAssembler: SlotDtoAssembler
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(SlotService::class.java)

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private fun buildLocationMap(slots: List<Slot>): Map<UUID, com.fitness.entity.TrainingLocation> {
        val ids = slots.mapNotNull { it.locationId }.toSet()
        if (ids.isEmpty()) return emptyMap()
        return locationRepository.findAllById(ids).associateBy { it.id!! }
    }

    private fun requireSlotOwnedByAdmin(slot: Slot, adminId: UUID) {
        if (slot.adminId != adminId) {
            throw org.springframework.security.access.AccessDeniedException("Access denied")
        }
    }

    private fun latestCancelledReservation(candidates: List<Reservation>): Reservation? =
        candidates.maxWithOrNull(
            compareBy<Reservation> { it.cancelledAt ?: Instant.EPOCH }
                .thenBy { it.createdAt }
        )

    private fun findLatestCancelledReservationForSlot(
        slot: Slot,
        cancelledReservations: List<Reservation>
    ): Reservation? {
        val slotId = slot.id
        val bySlotId = if (slotId != null) {
            cancelledReservations.filter { it.slotId == slotId }
        } else {
            emptyList()
        }
        return latestCancelledReservation(bySlotId)
            ?: latestCancelledReservation(
                cancelledReservations.filter {
                    it.date == slot.date && it.startTime == slot.startTime
                }
            )
    }

    fun getSlots(startDate: LocalDate, endDate: LocalDate, adminId: UUID): List<SlotDTO> {
        val admin = userRepository.findById(adminId).orElse(null)
        val slots = slotRepository.findByDateBetweenAndAdminId(startDate, endDate, adminId)
            .filter {
                slotPolicy.isWithinCalendarHours(
                    it.startTime,
                    it.endTime,
                    admin?.calendarStartHour ?: 6,
                    admin?.calendarEndHour ?: 22
                )
            }
        val allReservations = reservationRepository.findByDateRangeForAdmin(startDate, endDate, adminId)
        val activeReservations = allReservations.filter { it.status in listOf("confirmed", "completed", "no_show") }
        val cancelledReservations = allReservations.filter { it.status == "cancelled" }
        val pricingItemsMap = slotPricingService.loadPricingItemsForSlots(slots.mapNotNull { it.id })
        val locationMap = buildLocationMap(slots)

        // Count confirmed bookings per slot for group training
        val bookingsPerSlot = activeReservations.filter { it.slotId != null }
            .groupBy { it.slotId!! }
            .mapValues { (_, v) -> v.size }

        // Batch fetch all relevant users to avoid N+1 queries
        val userIds = (slots.mapNotNull { it.assignedUserId } +
            allReservations.mapNotNull { it.userId }).toSet()
        val usersMap = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id }
        } else emptyMap()

        return slots.map { slot ->
            val currentBookings = bookingsPerSlot[slot.id] ?: 0
            // Match confirmed reservation by slotId first, fall back to date-time matching
            val confirmedReservation = activeReservations.find { it.slotId == slot.id }
                ?: activeReservations.find { it.date == slot.date && it.startTime == slot.startTime }

            // Match cancelled reservation (only if no confirmed reservation)
            val cancelledReservation = if (confirmedReservation == null) {
                findLatestCancelledReservationForSlot(slot, cancelledReservations)
            } else null

            val reservation = confirmedReservation ?: cancelledReservation
            val user = slot.assignedUserId?.let { usersMap[it] }
                ?: reservation?.userId?.let { usersMap[it] }

            val slotStatus = when {
                currentBookings >= slot.capacity -> "reserved"
                confirmedReservation != null && slot.capacity == 1 -> "reserved"
                slot.status == SlotStatus.LOCKED -> SlotStatus.LOCKED.name.lowercase()
                slot.status == SlotStatus.BLOCKED -> SlotStatus.BLOCKED.name.lowercase()
                cancelledReservation != null && currentBookings == 0 -> "cancelled"
                slot.status == SlotStatus.RESERVED -> SlotStatus.UNLOCKED.name.lowercase()
                else -> slot.status.name.lowercase()
            }

            val location = slot.locationId?.let { locationMap[it] }

            slotDtoAssembler.toDto(
                slot = slot,
                status = slotStatus,
                user = user,
                assignedUserName = slotDtoAssembler.calendarAssignedUserName(user),
                reservation = reservation,
                cancelledReservation = cancelledReservation,
                pricingItems = pricingItemsMap[slot.id] ?: emptyList(),
                currentBookings = currentBookings,
                location = location
            )
        }.sortedWith(compareBy({ it.date }, { it.startTime }))
    }

    fun getUserVisibleSlots(startDate: LocalDate, endDate: LocalDate): List<SlotDTO> {
        val slots = slotRepository.findUserVisibleSlots(startDate, endDate)
        val reservations = reservationRepository.findByDateRange(startDate, endDate)
            .filter { it.status in listOf("confirmed", "completed", "no_show") }
        val pricingItemsMap = slotPricingService.loadPricingItemsForSlots(slots.mapNotNull { it.id })
        val locationMap = buildLocationMap(slots)

        val bookingsPerSlot = reservations.filter { it.slotId != null }
            .groupBy { it.slotId!! }
            .mapValues { (_, v) -> v.size }

        return slots.map { slot ->
            val currentBookings = bookingsPerSlot[slot.id] ?: 0
            val isFull = currentBookings >= slot.capacity
            val location = slot.locationId?.let { locationMap[it] }

            slotDtoAssembler.toDto(
                slot = slot,
                status = if (isFull) "reserved" else "unlocked",
                pricingItems = pricingItemsMap[slot.id] ?: emptyList(),
                currentBookings = currentBookings,
                location = location
            )
        }.sortedWith(compareBy({ it.date }, { it.startTime }))
    }

    @Transactional
    fun createSlot(request: CreateSlotRequest, adminId: UUID): SlotDTO {
        val date = LocalDate.parse(request.date, dateFormatter)
        val startTime = LocalTime.parse(request.startTime, timeFormatter)
        val endTime = startTime.plusMinutes(request.durationMinutes.toLong())
        if (LocalDateTime.of(date, startTime).isBefore(LocalDateTime.now(ZoneId.systemDefault()))) {
            throw IllegalArgumentException("Cannot create slot in the past")
        }

        val assignedUserId = request.assignedUserId?.let { UUID.fromString(it) }
        val locationId = request.locationId?.let { UUID.fromString(it) }

        slotPolicy.requireClientOwnedByAdmin(assignedUserId, adminId)
        slotPolicy.requireLocationOwnedByAdmin(locationId, adminId)
        slotPolicy.requirePricingItemsOwnedByAdmin(request.pricingItemIds, adminId)
        slotPolicy.requireWithinAdminCalendarHours(adminId, startTime, endTime)

        // Check for overlapping slots owned by the same trainer
        if (slotRepository.existsOverlappingSlotForAdmin(date, startTime, endTime, adminId, null)) {
            throw IllegalArgumentException("This time slot overlaps with an existing slot")
        }

        val slot = Slot(
            date = date,
            startTime = startTime,
            endTime = endTime,
            durationMinutes = request.durationMinutes,
            status = SlotStatus.LOCKED,
            assignedUserId = assignedUserId,
            locationId = locationId,
            note = request.note,
            adminId = adminId,
            capacity = request.capacity
        )

        val savedSlot = slotRepository.save(slot)

        // Save pricing items
        slotPricingService.savePricingItemsForSlot(savedSlot.id!!, request.pricingItemIds)
        val pricingItems = slotPricingService.loadPricingItemsForSlots(listOf(savedSlot.id))[savedSlot.id] ?: emptyList()

        val user = assignedUserId?.let { userRepository.findById(it).orElse(null) }
        val location = savedSlot.locationId?.let { locationRepository.findById(it).orElse(null) }

        return slotDtoAssembler.toDto(
            slot = savedSlot,
            status = savedSlot.status.name.lowercase(),
            user = user,
            pricingItems = pricingItems,
            location = location
        )
    }

    @Transactional
    fun updateSlot(id: UUID, request: UpdateSlotRequest, adminId: UUID): SlotDTO {
        val slot = slotRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Slot not found") }
        requireSlotOwnedByAdmin(slot, adminId)

        request.status?.let {
            slot.status = SlotStatus.valueOf(it.uppercase())
        }
        request.note?.let {
            slot.note = it
        }
        request.assignedUserId?.let {
            val assignedUserId = if (it.isBlank()) null else UUID.fromString(it)
            slotPolicy.requireClientOwnedByAdmin(assignedUserId, adminId)
            slot.assignedUserId = assignedUserId
        }
        request.date?.let {
            slot.date = LocalDate.parse(it)
        }
        request.startTime?.let {
            slot.startTime = LocalTime.parse(it)
        }
        request.endTime?.let {
            slot.endTime = LocalTime.parse(it)
        }
        when {
            request.clearLocation == true -> slot.locationId = null
            request.locationId != null -> {
                val locationId = UUID.fromString(request.locationId)
                slotPolicy.requireLocationOwnedByAdmin(locationId, adminId)
                slot.locationId = locationId
            }
        }

        if (slot.endTime <= slot.startTime) {
            throw IllegalArgumentException("Slot end time must be after start time")
        }
        slot.durationMinutes = Duration.between(slot.startTime, slot.endTime).toMinutes().toInt()
        if (slot.durationMinutes < 15 || slot.durationMinutes > 480) {
            throw IllegalArgumentException("Slot duration must be between 15 and 480 minutes")
        }

        slotPolicy.requireWithinAdminCalendarHours(adminId, slot.startTime, slot.endTime)

        if (slotRepository.existsOverlappingSlotForAdmin(slot.date, slot.startTime, slot.endTime, adminId, slot.id)) {
            throw IllegalArgumentException("This time slot overlaps with an existing slot")
        }

        // Update pricing items if provided
        request.pricingItemIds?.let { ids ->
            val distinctIds = ids.distinct()
            slotPolicy.requirePricingItemsOwnedByAdmin(distinctIds, adminId)
            val slotId = slot.id ?: throw IllegalArgumentException("Slot not found")
            slotPricingItemRepository.deleteBySlotId(slotId)
            entityManager.flush()
            slotPricingService.savePricingItemsForSlot(slotId, distinctIds)
        }

        val savedSlot = slotRepository.save(slot)
        val user = savedSlot.assignedUserId?.let { userRepository.findById(it).orElse(null) }
        val savedSlotId = savedSlot.id ?: throw IllegalArgumentException("Slot not found")
        val pricingItems = slotPricingService.loadPricingItemsForSlots(listOf(savedSlotId))[savedSlotId] ?: emptyList()
        val location = savedSlot.locationId?.let { locationRepository.findById(it).orElse(null) }

        // Match reservation by slotId first, fall back to date-time matching
        val confirmedReservations = reservationRepository.findByDateRange(savedSlot.date, savedSlot.date)
            .filter { it.status == "confirmed" }
        val reservation = confirmedReservations.find { it.slotId == savedSlot.id }
            ?: confirmedReservations.find { it.date == savedSlot.date && it.startTime == savedSlot.startTime }

        val currentBookings = confirmedReservations.count { it.slotId == savedSlot.id }

        return slotDtoAssembler.toDto(
            slot = savedSlot,
            status = when {
                reservation != null -> "reserved"
                currentBookings >= savedSlot.capacity && currentBookings > 0 -> "reserved"
                savedSlot.status == SlotStatus.RESERVED -> SlotStatus.UNLOCKED.name.lowercase()
                else -> savedSlot.status.name.lowercase()
            },
            user = user,
            reservation = reservation,
            pricingItems = pricingItems,
            currentBookings = currentBookings,
            location = location
        )
    }

    fun getSlotCancellationPreview(id: UUID, adminId: UUID): SlotCancellationPreviewDTO {
        val slot = slotRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Slot not found") }
        requireSlotOwnedByAdmin(slot, adminId)

        val confirmedReservations = reservationRepository.findConfirmedBySlotId(id)
        val userIds = confirmedReservations.map { it.userId }.distinct()
        val usersMap = userRepository.findAllById(userIds).associateBy { it.id }

        val affected = confirmedReservations.map { r ->
            val user = usersMap[r.userId]
            AffectedReservationDTO(
                reservationId = r.id.toString(),
                userId = r.userId.toString(),
                userName = user?.displayName,
                userEmail = user?.email,
                creditsUsed = r.creditsUsed
            )
        }

        return SlotCancellationPreviewDTO(
            slotId = id.toString(),
            affectedReservations = affected,
            totalCreditsToRefund = affected.sumOf { it.creditsUsed }
        )
    }

    @Transactional
    fun deleteSlot(
        id: UUID,
        adminId: UUID,
        cancellationReason: String? = null,
        refundNote: String = "Vrácení kreditů - slot zrušen trenérem"
    ) {
        val slot = slotRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Slot not found") }
        requireSlotOwnedByAdmin(slot, adminId)

        // Cancel all confirmed reservations and refund credits
        val confirmedReservations = reservationRepository.findConfirmedBySlotId(id)
        if (confirmedReservations.isNotEmpty()) {
            val userIds = confirmedReservations.map { it.userId }.distinct()
            val usersMap = userRepository.findAllById(userIds).associateBy { it.id }
            val formattedDate = slot.date.format(dateFormatter)
            val formattedTime = "${slot.startTime.format(timeFormatter)} - ${slot.endTime.format(timeFormatter)}"

            for (reservation in confirmedReservations) {
                // Cancel reservation
                val updated = reservation.copy(status = "cancelled", cancelledAt = Instant.now())
                reservationRepository.save(updated)

                // Refund credits
                if (reservation.creditsUsed > 0) {
                    userRepository.updateCredits(reservation.userId, reservation.creditsUsed)
                    creditTransactionRepository.save(
                        CreditTransaction(
                            userId = reservation.userId,
                            amount = reservation.creditsUsed,
                            type = TransactionType.REFUND.value,
                            referenceId = reservation.id,
                            note = refundNote
                        )
                    )
                }

                // Send notification email
                val user = usersMap[reservation.userId]
                if (user != null) {
                    auditService.logAdminReservationCancelled(
                        adminId = adminId,
                        adminEmail = null,
                        reservation = updated,
                        client = user,
                        refundCredits = reservation.creditsUsed > 0,
                        creditsRefunded = reservation.creditsUsed.coerceAtLeast(0),
                        reason = cancellationReason ?: "slot_deleted"
                    )
                    try {
                        emailService.sendSlotCancelledByTrainerEmail(
                            to = user.email,
                            firstName = user.firstName,
                            date = formattedDate,
                            time = formattedTime,
                            creditsRefunded = reservation.creditsUsed,
                            reason = cancellationReason
                        )
                    } catch (e: Exception) {
                        logger.error("Failed to send slot cancellation email to ${user.email}", e)
                    }
                }
            }
        }

        slotPricingItemRepository.deleteBySlotId(id)
        slotRepository.deleteById(id)
    }

    @Transactional
    fun deleteFutureSlotsOutsideCalendarRange(adminId: UUID, startHour: Int, endHour: Int): CalendarRangeCleanupResult {
        if (startHour >= endHour) {
            throw IllegalArgumentException("Start hour must be less than end hour")
        }

        val startBoundary = LocalTime.of(startHour, 0)
        val endBoundary = if (endHour >= 24) LocalTime.MAX else LocalTime.of(endHour, 0)
        val today = LocalDate.now()
        val now = LocalTime.now()

        val slots = slotRepository.findFutureOutsideCalendarRangeForUpdate(
            adminId = adminId,
            today = today,
            now = now,
            startBoundary = startBoundary,
            endBoundary = endBoundary
        )

        var cancelledReservations = 0
        var refundedCredits = 0
        val reason = "Termín byl zrušen kvůli změně rozsahu kalendáře trenéra."
        val note = "Vrácení kreditů - termín zrušen kvůli změně rozsahu kalendáře"

        for (slot in slots) {
            val slotId = slot.id ?: continue
            val confirmedReservations = reservationRepository.findConfirmedBySlotId(slotId)
            cancelledReservations += confirmedReservations.size
            refundedCredits += confirmedReservations.sumOf { it.creditsUsed.coerceAtLeast(0) }
            deleteSlot(slotId, adminId, cancellationReason = reason, refundNote = note)
        }

        if (slots.isNotEmpty()) {
            logger.info(
                "Calendar range cleanup for admin {} deleted {} slots, cancelled {} reservations, refunded {} credits",
                adminId,
                slots.size,
                cancelledReservations,
                refundedCredits
            )
        }

        return CalendarRangeCleanupResult(
            deletedSlots = slots.size,
            cancelledReservations = cancelledReservations,
            refundedCredits = refundedCredits
        )
    }

    @Transactional
    fun unlockWeek(weekStartDate: LocalDate, endDate: LocalDate? = null, adminId: UUID): Int {
        val start = weekStartDate
        val end = endDate ?: run {
            // Fallback: if no endDate, use Monday-Sunday of the week
            val monday = if (weekStartDate.dayOfWeek == DayOfWeek.MONDAY) {
                weekStartDate
            } else {
                weekStartDate.with(DayOfWeek.MONDAY)
            }
            monday.plusDays(6)
        }

        return slotRepository.updateStatusByDateRangeAndStatusAndAdminId(
            start,
            end,
            SlotStatus.LOCKED,
            SlotStatus.UNLOCKED,
            adminId
        )
    }

    @Transactional
    fun applyTemplate(
        templateId: UUID,
        weekStartDate: LocalDate,
        templateSlots: List<TemplateSlotDTO>,
        templateLocationId: UUID? = null,
        adminId: UUID
    ): List<SlotDTO> {
        slotPolicy.requireLocationOwnedByAdmin(templateLocationId, adminId)
        val admin = userRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("Admin not found") }
        // Ensure it's a Monday
        val monday = if (weekStartDate.dayOfWeek == DayOfWeek.MONDAY) {
            weekStartDate
        } else {
            weekStartDate.with(DayOfWeek.MONDAY)
        }

        val createdSlots = mutableListOf<Pair<Slot, List<String>>>()

        for (templateSlot in templateSlots) {
            val dayOfWeek = DayOfWeek.of(templateSlot.dayOfWeek)
            val slotDate = monday.with(dayOfWeek)
            val startTime = LocalTime.parse(templateSlot.startTime, timeFormatter)
            val endTime = LocalTime.parse(templateSlot.endTime, timeFormatter)

            if (endTime <= startTime) {
                throw IllegalArgumentException("Template slot end time must be after start time")
            }

            if (!slotPolicy.isWithinCalendarHours(startTime, endTime, admin.calendarStartHour, admin.calendarEndHour)) {
                continue
            }

            // Per-slot location override takes priority, falling back to the
            // template-level location so a whole-template choice still propagates.
            val effectiveLocationId = templateSlot.locationId?.let { UUID.fromString(it) }
                ?: templateLocationId
            slotPolicy.requireLocationOwnedByAdmin(effectiveLocationId, adminId)
            slotPolicy.requirePricingItemsOwnedByAdmin(templateSlot.pricingItemIds, adminId)
            if (slotRepository.existsOverlappingSlotForAdmin(slotDate, startTime, endTime, adminId, null)) {
                continue
            }
            val slot = Slot(
                date = slotDate,
                startTime = startTime,
                endTime = endTime,
                durationMinutes = templateSlot.durationMinutes,
                status = SlotStatus.LOCKED,
                templateId = templateId,
                adminId = adminId,
                locationId = effectiveLocationId,
                capacity = templateSlot.capacity
            )

            val savedSlot = slotRepository.save(slot)

            // Copy pricing items from template slot
            if (templateSlot.pricingItemIds.isNotEmpty()) {
                slotPricingService.savePricingItemsForSlot(savedSlot.id!!, templateSlot.pricingItemIds)
            }

            createdSlots.add(savedSlot to templateSlot.pricingItemIds)
        }

        val slotIds = createdSlots.mapNotNull { it.first.id }
        val pricingItemsMap = slotPricingService.loadPricingItemsForSlots(slotIds)
        val locationMap = buildLocationMap(createdSlots.map { it.first })

        return createdSlots.map { (slot, _) ->
            val location = slot.locationId?.let { locationMap[it] }
            slotDtoAssembler.toDto(
                slot = slot,
                status = slot.status.name.lowercase(),
                pricingItems = pricingItemsMap[slot.id] ?: emptyList(),
                location = location
            )
        }
    }
}
