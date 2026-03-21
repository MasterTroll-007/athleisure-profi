package com.fitness.service

import com.fitness.dto.*
import com.fitness.entity.CreditTransaction
import com.fitness.entity.Slot
import com.fitness.entity.SlotPricingItem
import com.fitness.entity.SlotStatus
import com.fitness.entity.TransactionType
import com.fitness.entity.displayName
import com.fitness.repository.CreditTransactionRepository
import com.fitness.repository.PricingItemRepository
import com.fitness.repository.ReservationRepository
import com.fitness.repository.SlotPricingItemRepository
import com.fitness.repository.SlotRepository
import com.fitness.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class SlotService(
    private val slotRepository: SlotRepository,
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val slotPricingItemRepository: SlotPricingItemRepository,
    private val pricingItemRepository: PricingItemRepository,
    private val creditTransactionRepository: CreditTransactionRepository,
    private val emailService: EmailService
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(SlotService::class.java)

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private fun loadPricingItemsForSlots(slotIds: List<UUID>): Map<UUID, List<PricingItemSummary>> {
        if (slotIds.isEmpty()) return emptyMap()
        val slotPricingItems = slotPricingItemRepository.findBySlotIdIn(slotIds)
        if (slotPricingItems.isEmpty()) return emptyMap()
        val pricingItemIds = slotPricingItems.map { it.pricingItemId }.distinct()
        val pricingItems = pricingItemRepository.findAllById(pricingItemIds).associateBy { it.id }
        return slotPricingItems.groupBy({ it.slotId }) { spi ->
            pricingItems[spi.pricingItemId]?.let {
                PricingItemSummary(it.id.toString(), it.nameCs, it.nameEn, it.credits)
            }
        }.mapValues { (_, v) -> v.filterNotNull() }
    }

    private fun savePricingItemsForSlot(slotId: UUID, pricingItemIds: List<String>) {
        for (piId in pricingItemIds) {
            slotPricingItemRepository.save(SlotPricingItem(slotId = slotId, pricingItemId = UUID.fromString(piId)))
        }
    }

    fun getSlots(startDate: LocalDate, endDate: LocalDate): List<SlotDTO> {
        val slots = slotRepository.findByDateBetween(startDate, endDate)
        val allReservations = reservationRepository.findByDateRange(startDate, endDate)
        val activeReservations = allReservations.filter { it.status in listOf("confirmed", "completed", "no_show") }
        val cancelledReservations = allReservations.filter { it.status == "cancelled" }
        val pricingItemsMap = loadPricingItemsForSlots(slots.map { it.id })

        // Count confirmed bookings per slot for group training
        val bookingsPerSlot = activeReservations.filter { it.slotId != null }
            .groupBy { it.slotId!! }
            .mapValues { (_, v) -> v.size }

        return slots.map { slot ->
            val currentBookings = bookingsPerSlot[slot.id] ?: 0
            // Match confirmed reservation by slotId first, fall back to date-time matching
            val confirmedReservation = activeReservations.find { it.slotId == slot.id }
                ?: activeReservations.find { it.date == slot.date && it.startTime == slot.startTime }

            // Match cancelled reservation (only if no confirmed reservation)
            val cancelledReservation = if (confirmedReservation == null) {
                cancelledReservations.find { it.slotId == slot.id }
                    ?: cancelledReservations.find { it.date == slot.date && it.startTime == slot.startTime }
            } else null

            val reservation = confirmedReservation ?: cancelledReservation
            val user = slot.assignedUserId?.let { userRepository.findById(it).orElse(null) }
                ?: reservation?.userId?.let { userRepository.findById(it).orElse(null) }

            val slotStatus = when {
                currentBookings >= slot.capacity -> "reserved"
                confirmedReservation != null && slot.capacity == 1 -> "reserved"
                cancelledReservation != null && currentBookings == 0 -> "cancelled"
                else -> slot.status.name.lowercase()
            }

            SlotDTO(
                id = slot.id.toString(),
                date = slot.date.format(dateFormatter),
                startTime = slot.startTime.format(timeFormatter),
                endTime = slot.endTime.format(timeFormatter),
                durationMinutes = slot.durationMinutes,
                status = slotStatus,
                assignedUserId = user?.id?.toString(),
                assignedUserName = user?.let {
                    val lastName = it.lastName?.trim() ?: ""
                    val firstName = it.firstName?.trim() ?: ""
                    when {
                        lastName.isNotEmpty() && firstName.isNotEmpty() -> "$lastName\n$firstName"
                        lastName.isNotEmpty() -> lastName
                        firstName.isNotEmpty() -> firstName
                        else -> null
                    }
                },
                assignedUserEmail = user?.email,
                note = reservation?.note ?: slot.note,
                reservationId = reservation?.id?.toString(),
                createdAt = slot.createdAt.toString(),
                cancelledAt = cancelledReservation?.cancelledAt?.toString(),
                pricingItems = pricingItemsMap[slot.id] ?: emptyList(),
                capacity = slot.capacity,
                currentBookings = currentBookings
            )
        }.sortedWith(compareBy({ it.date }, { it.startTime }))
    }

    fun getUserVisibleSlots(startDate: LocalDate, endDate: LocalDate): List<SlotDTO> {
        val slots = slotRepository.findUserVisibleSlots(startDate, endDate)
        val reservations = reservationRepository.findByDateRange(startDate, endDate)
            .filter { it.status in listOf("confirmed", "completed", "no_show") }
        val pricingItemsMap = loadPricingItemsForSlots(slots.map { it.id })

        val bookingsPerSlot = reservations.filter { it.slotId != null }
            .groupBy { it.slotId!! }
            .mapValues { (_, v) -> v.size }

        return slots.map { slot ->
            val currentBookings = bookingsPerSlot[slot.id] ?: 0
            val isFull = currentBookings >= slot.capacity

            SlotDTO(
                id = slot.id.toString(),
                date = slot.date.format(dateFormatter),
                startTime = slot.startTime.format(timeFormatter),
                endTime = slot.endTime.format(timeFormatter),
                durationMinutes = slot.durationMinutes,
                status = if (isFull) "reserved" else "unlocked",
                assignedUserId = null,
                assignedUserName = null,
                assignedUserEmail = null,
                note = null,
                reservationId = null,
                createdAt = slot.createdAt.toString(),
                pricingItems = pricingItemsMap[slot.id] ?: emptyList(),
                capacity = slot.capacity,
                currentBookings = currentBookings
            )
        }.sortedWith(compareBy({ it.date }, { it.startTime }))
    }

    @Transactional
    fun createSlot(request: CreateSlotRequest): SlotDTO {
        val date = LocalDate.parse(request.date, dateFormatter)
        val startTime = LocalTime.parse(request.startTime, timeFormatter)
        val endTime = startTime.plusMinutes(request.durationMinutes.toLong())

        // Check for overlapping slots
        if (slotRepository.existsOverlappingSlot(date, startTime, endTime)) {
            throw IllegalArgumentException("This time slot overlaps with an existing slot")
        }

        val assignedUserId = request.assignedUserId?.let { UUID.fromString(it) }

        val slot = Slot(
            date = date,
            startTime = startTime,
            endTime = endTime,
            durationMinutes = request.durationMinutes,
            status = SlotStatus.LOCKED,
            assignedUserId = assignedUserId,
            note = request.note,
            capacity = request.capacity
        )

        val savedSlot = slotRepository.save(slot)

        // Save pricing items
        savePricingItemsForSlot(savedSlot.id, request.pricingItemIds)
        val pricingItems = loadPricingItemsForSlots(listOf(savedSlot.id))[savedSlot.id] ?: emptyList()

        val user = assignedUserId?.let { userRepository.findById(it).orElse(null) }

        return SlotDTO(
            id = savedSlot.id.toString(),
            date = savedSlot.date.format(dateFormatter),
            startTime = savedSlot.startTime.format(timeFormatter),
            endTime = savedSlot.endTime.format(timeFormatter),
            durationMinutes = savedSlot.durationMinutes,
            status = savedSlot.status.name.lowercase(),
            assignedUserId = user?.id?.toString(),
            assignedUserName = user?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim().ifEmpty { null } },
            assignedUserEmail = user?.email,
            note = savedSlot.note,
            reservationId = null,
            createdAt = savedSlot.createdAt.toString(),
            pricingItems = pricingItems,
            capacity = savedSlot.capacity,
            currentBookings = 0
        )
    }

    @Transactional
    fun updateSlot(id: UUID, request: UpdateSlotRequest): SlotDTO {
        val slot = slotRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Slot not found") }

        request.status?.let {
            slot.status = SlotStatus.valueOf(it.uppercase())
        }
        request.note?.let {
            slot.note = it
        }
        request.assignedUserId?.let {
            slot.assignedUserId = if (it.isBlank()) null else UUID.fromString(it)
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

        // Update pricing items if provided
        request.pricingItemIds?.let { ids ->
            slotPricingItemRepository.deleteBySlotId(slot.id)
            savePricingItemsForSlot(slot.id, ids)
        }

        val savedSlot = slotRepository.save(slot)
        val user = savedSlot.assignedUserId?.let { userRepository.findById(it).orElse(null) }
        val pricingItems = loadPricingItemsForSlots(listOf(savedSlot.id))[savedSlot.id] ?: emptyList()

        // Match reservation by slotId first, fall back to date-time matching
        val confirmedReservations = reservationRepository.findByDateRange(savedSlot.date, savedSlot.date)
            .filter { it.status == "confirmed" }
        val reservation = confirmedReservations.find { it.slotId == savedSlot.id }
            ?: confirmedReservations.find { it.date == savedSlot.date && it.startTime == savedSlot.startTime }

        val currentBookings = confirmedReservations.count { it.slotId == savedSlot.id }

        return SlotDTO(
            id = savedSlot.id.toString(),
            date = savedSlot.date.format(dateFormatter),
            startTime = savedSlot.startTime.format(timeFormatter),
            endTime = savedSlot.endTime.format(timeFormatter),
            durationMinutes = savedSlot.durationMinutes,
            status = if (reservation != null) "reserved" else savedSlot.status.name.lowercase(),
            assignedUserId = user?.id?.toString(),
            assignedUserName = user?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim().ifEmpty { null } },
            assignedUserEmail = user?.email,
            note = savedSlot.note,
            reservationId = reservation?.id?.toString(),
            createdAt = savedSlot.createdAt.toString(),
            pricingItems = pricingItems,
            capacity = savedSlot.capacity,
            currentBookings = currentBookings
        )
    }

    fun getSlotCancellationPreview(id: UUID): SlotCancellationPreviewDTO {
        val slot = slotRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Slot not found") }

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
    fun deleteSlot(id: UUID) {
        if (!slotRepository.existsById(id)) {
            throw IllegalArgumentException("Slot not found")
        }

        val slot = slotRepository.findById(id).get()

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
                            note = "Vrácení kreditů - slot zrušen trenérem"
                        )
                    )
                }

                // Send notification email
                val user = usersMap[reservation.userId]
                if (user != null) {
                    try {
                        emailService.sendSlotCancelledByTrainerEmail(
                            to = user.email,
                            firstName = user.firstName,
                            date = formattedDate,
                            time = formattedTime,
                            creditsRefunded = reservation.creditsUsed
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
    fun unlockWeek(weekStartDate: LocalDate, endDate: LocalDate? = null): Int {
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

        return slotRepository.updateStatusByDateRangeAndStatus(
            start,
            end,
            SlotStatus.LOCKED,
            SlotStatus.UNLOCKED
        )
    }

    @Transactional
    fun applyTemplate(templateId: UUID, weekStartDate: LocalDate, templateSlots: List<TemplateSlotDTO>): List<SlotDTO> {
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

            // Skip if slot overlaps with existing one
            if (slotRepository.existsOverlappingSlot(slotDate, startTime, endTime)) {
                continue
            }

            val slot = Slot(
                date = slotDate,
                startTime = startTime,
                endTime = endTime,
                durationMinutes = templateSlot.durationMinutes,
                status = SlotStatus.LOCKED,
                templateId = templateId,
                capacity = templateSlot.capacity
            )

            val savedSlot = slotRepository.save(slot)

            // Copy pricing items from template slot
            if (templateSlot.pricingItemIds.isNotEmpty()) {
                savePricingItemsForSlot(savedSlot.id, templateSlot.pricingItemIds)
            }

            createdSlots.add(savedSlot to templateSlot.pricingItemIds)
        }

        val slotIds = createdSlots.map { it.first.id }
        val pricingItemsMap = loadPricingItemsForSlots(slotIds)

        return createdSlots.map { (slot, _) ->
            SlotDTO(
                id = slot.id.toString(),
                date = slot.date.format(dateFormatter),
                startTime = slot.startTime.format(timeFormatter),
                endTime = slot.endTime.format(timeFormatter),
                durationMinutes = slot.durationMinutes,
                status = slot.status.name.lowercase(),
                assignedUserId = null,
                assignedUserName = null,
                assignedUserEmail = null,
                note = null,
                reservationId = null,
                createdAt = slot.createdAt.toString(),
                pricingItems = pricingItemsMap[slot.id] ?: emptyList(),
                capacity = slot.capacity,
                currentBookings = 0
            )
        }
    }
}
