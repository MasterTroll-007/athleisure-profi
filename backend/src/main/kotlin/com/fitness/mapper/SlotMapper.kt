package com.fitness.mapper

import com.fitness.dto.AdminCalendarSlotDTO
import com.fitness.dto.SlotDTO
import com.fitness.dto.SlotReservationDTO
import com.fitness.entity.Reservation
import com.fitness.entity.Slot
import com.fitness.entity.SlotStatus
import com.fitness.entity.User
import com.fitness.repository.ReservationRepository
import com.fitness.repository.UserRepository
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@Component
class SlotMapper(
    private val userRepository: UserRepository,
    private val reservationRepository: ReservationRepository
) {
    /**
     * Convert Slot entity to SlotDTO with pre-fetched reservation and user data.
     */
    fun toDTO(
        slot: Slot,
        reservation: Reservation?,
        user: User?
    ): SlotDTO {
        val status = when {
            reservation != null && reservation.status == "confirmed" -> "reserved"
            slot.status == SlotStatus.BLOCKED -> "blocked"
            slot.status == SlotStatus.LOCKED -> "locked"
            slot.status == SlotStatus.RESERVED -> "reserved"
            else -> "unlocked"
        }

        val userName = user?.let {
            "${it.firstName ?: ""} ${it.lastName ?: ""}".trim().ifEmpty { null }
        }

        return SlotDTO(
            id = slot.id.toString(),
            date = slot.date.toString(),
            startTime = slot.startTime.toString(),
            endTime = slot.endTime.toString(),
            durationMinutes = slot.durationMinutes,
            status = status,
            assignedUserId = (reservation?.userId ?: slot.assignedUserId)?.toString(),
            assignedUserName = userName,
            assignedUserEmail = user?.email,
            note = reservation?.note ?: slot.note,
            reservationId = reservation?.id?.toString(),
            createdAt = slot.createdAt.toString(),
            cancelledAt = reservation?.cancelledAt?.toString()
        )
    }

    /**
     * Convert Slot entity to AdminCalendarSlotDTO for admin calendar view.
     */
    fun toAdminCalendarDTO(
        slot: Slot,
        reservation: Reservation?,
        user: User?,
        isPast: Boolean
    ): AdminCalendarSlotDTO {
        val status = when {
            slot.status == SlotStatus.RESERVED || reservation != null -> "reserved"
            slot.status == SlotStatus.BLOCKED -> "blocked"
            slot.status == SlotStatus.LOCKED -> "locked"
            isPast -> "past"
            else -> "available"
        }

        val reservationInfo = reservation?.let { res ->
            SlotReservationDTO(
                id = res.id.toString(),
                userName = user?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim().ifEmpty { null } },
                userEmail = user?.email,
                status = res.status,
                note = res.note
            )
        }

        return AdminCalendarSlotDTO(
            id = slot.id.toString(),
            blockId = slot.id.toString(),
            date = slot.date.toString(),
            startTime = slot.startTime.toString(),
            endTime = slot.endTime.toString(),
            status = status,
            reservation = reservationInfo
        )
    }

    /**
     * Batch convert slots to DTOs with efficient data fetching.
     */
    fun toDTOBatch(
        slots: List<Slot>,
        reservations: List<Reservation>
    ): List<SlotDTO> {
        if (slots.isEmpty()) return emptyList()

        val confirmedReservations = reservations.filter { it.status == "confirmed" }

        // Build lookup maps for reservations
        val reservationBySlotId = confirmedReservations.associateBy { it.slotId }
        val reservationByDateTime = confirmedReservations.associateBy { "${it.date}-${it.startTime}" }

        // Collect all user IDs needed
        val userIds = mutableSetOf<UUID>()
        slots.forEach { slot ->
            slot.assignedUserId?.let { userIds.add(it) }
        }
        confirmedReservations.forEach { res ->
            userIds.add(res.userId)
        }

        // Batch fetch users
        val usersMap = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id }
        } else {
            emptyMap()
        }

        return slots.map { slot ->
            // Match reservation by slotId first, then by date-time
            val reservation = reservationBySlotId[slot.id]
                ?: reservationByDateTime["${slot.date}-${slot.startTime}"]

            val user = (reservation?.userId ?: slot.assignedUserId)?.let { usersMap[it] }

            toDTO(slot, reservation, user)
        }.sortedWith(compareBy({ it.date }, { it.startTime }))
    }

    /**
     * Batch convert slots to admin calendar DTOs.
     */
    fun toAdminCalendarDTOBatch(
        slots: List<Slot>,
        reservations: List<Reservation>,
        today: LocalDate = LocalDate.now(),
        now: LocalTime = LocalTime.now()
    ): List<AdminCalendarSlotDTO> {
        if (slots.isEmpty()) return emptyList()

        val confirmedReservations = reservations.filter { it.status == "confirmed" }
        val reservationMap = confirmedReservations.associateBy { it.slotId }

        // Batch fetch users
        val userIds = confirmedReservations.map { it.userId }.distinct()
        val usersMap = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id }
        } else {
            emptyMap()
        }

        return slots.map { slot ->
            val isPast = slot.date < today || (slot.date == today && slot.startTime.plusMinutes(15) < now)
            val reservation = reservationMap[slot.id]
            val user = reservation?.let { usersMap[it.userId] }

            toAdminCalendarDTO(slot, reservation, user, isPast)
        }.sortedWith(compareBy({ it.date }, { it.startTime }))
    }
}
