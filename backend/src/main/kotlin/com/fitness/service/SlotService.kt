package com.fitness.service

import com.fitness.dto.*
import com.fitness.entity.Slot
import com.fitness.entity.SlotStatus
import com.fitness.repository.ReservationRepository
import com.fitness.repository.SlotRepository
import com.fitness.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class SlotService(
    private val slotRepository: SlotRepository,
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository
) {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun getSlots(startDate: LocalDate, endDate: LocalDate): List<SlotDTO> {
        val slots = slotRepository.findByDateBetween(startDate, endDate)
        val allReservations = reservationRepository.findByDateRange(startDate, endDate)
        val confirmedReservations = allReservations.filter { it.status == "confirmed" }
        val cancelledReservations = allReservations.filter { it.status == "cancelled" }

        return slots.map { slot ->
            // Match confirmed reservation by slotId first, fall back to date-time matching
            val confirmedReservation = confirmedReservations.find { it.slotId == slot.id }
                ?: confirmedReservations.find { it.date == slot.date && it.startTime == slot.startTime }

            // Match cancelled reservation (only if no confirmed reservation)
            val cancelledReservation = if (confirmedReservation == null) {
                cancelledReservations.find { it.slotId == slot.id }
                    ?: cancelledReservations.find { it.date == slot.date && it.startTime == slot.startTime }
            } else null

            val reservation = confirmedReservation ?: cancelledReservation
            val user = slot.assignedUserId?.let { userRepository.findById(it).orElse(null) }
                ?: reservation?.userId?.let { userRepository.findById(it).orElse(null) }

            SlotDTO(
                id = slot.id.toString(),
                date = slot.date.format(dateFormatter),
                startTime = slot.startTime.format(timeFormatter),
                endTime = slot.endTime.format(timeFormatter),
                durationMinutes = slot.durationMinutes,
                status = when {
                    confirmedReservation != null -> "reserved"
                    cancelledReservation != null -> "cancelled"
                    else -> slot.status.name.lowercase()
                },
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
                cancelledAt = cancelledReservation?.cancelledAt?.toString()
            )
        }.sortedWith(compareBy({ it.date }, { it.startTime }))
    }

    fun getUserVisibleSlots(startDate: LocalDate, endDate: LocalDate): List<SlotDTO> {
        val slots = slotRepository.findUserVisibleSlots(startDate, endDate)
        val reservations = reservationRepository.findByDateRange(startDate, endDate)
            .filter { it.status == "confirmed" }

        return slots.map { slot ->
            // Match reservation by slotId first, fall back to date-time matching
            val reservation = reservations.find { it.slotId == slot.id }
                ?: reservations.find { it.date == slot.date && it.startTime == slot.startTime }

            SlotDTO(
                id = slot.id.toString(),
                date = slot.date.format(dateFormatter),
                startTime = slot.startTime.format(timeFormatter),
                endTime = slot.endTime.format(timeFormatter),
                durationMinutes = slot.durationMinutes,
                status = if (reservation != null) "reserved" else "unlocked",
                assignedUserId = null,
                assignedUserName = null,
                assignedUserEmail = null,
                note = null,
                reservationId = null,
                createdAt = slot.createdAt.toString()
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
            note = request.note
        )

        val savedSlot = slotRepository.save(slot)
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
            createdAt = savedSlot.createdAt.toString()
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

        val savedSlot = slotRepository.save(slot)
        val user = savedSlot.assignedUserId?.let { userRepository.findById(it).orElse(null) }

        // Match reservation by slotId first, fall back to date-time matching
        val confirmedReservations = reservationRepository.findByDateRange(savedSlot.date, savedSlot.date)
            .filter { it.status == "confirmed" }
        val reservation = confirmedReservations.find { it.slotId == savedSlot.id }
            ?: confirmedReservations.find { it.date == savedSlot.date && it.startTime == savedSlot.startTime }

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
            createdAt = savedSlot.createdAt.toString()
        )
    }

    @Transactional
    fun deleteSlot(id: UUID) {
        if (!slotRepository.existsById(id)) {
            throw IllegalArgumentException("Slot not found")
        }
        slotRepository.deleteById(id)
    }

    @Transactional
    fun unlockWeek(weekStartDate: LocalDate): Int {
        // Ensure it's a Monday
        val monday = if (weekStartDate.dayOfWeek == DayOfWeek.MONDAY) {
            weekStartDate
        } else {
            weekStartDate.with(DayOfWeek.MONDAY)
        }
        val sunday = monday.plusDays(6)

        return slotRepository.updateStatusByDateRangeAndStatus(
            monday,
            sunday,
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

        val createdSlots = mutableListOf<Slot>()

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
                templateId = templateId
            )

            createdSlots.add(slotRepository.save(slot))
        }

        return createdSlots.map { slot ->
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
                createdAt = slot.createdAt.toString()
            )
        }
    }
}
