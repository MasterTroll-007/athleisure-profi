package com.fitness.mapper

import com.fitness.dto.ReservationCalendarEvent
import com.fitness.dto.ReservationDTO
import com.fitness.entity.AvailabilityBlock
import com.fitness.entity.Reservation
import com.fitness.entity.Slot
import com.fitness.entity.TrainingLocation
import com.fitness.entity.User
import com.fitness.entity.displayName
import com.fitness.repository.AvailabilityBlockRepository
import com.fitness.repository.PricingItemRepository
import com.fitness.repository.SlotRepository
import com.fitness.repository.TrainingLocationRepository
import com.fitness.repository.UserRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class ReservationMapper(
    private val userRepository: UserRepository,
    private val pricingItemRepository: PricingItemRepository,
    private val slotRepository: SlotRepository,
    private val blockRepository: AvailabilityBlockRepository,
    private val locationRepository: TrainingLocationRepository
) {
    /**
     * Convert Reservation entity to ReservationDTO with pre-fetched user data.
     */
    fun toDTO(
        reservation: Reservation,
        firstName: String?,
        lastName: String?,
        email: String?,
        pricingItemName: String?,
        location: TrainingLocation? = null
    ): ReservationDTO {
        val userName = listOfNotNull(firstName, lastName).joinToString(" ").ifEmpty { null }
        return ReservationDTO(
            id = reservation.id.toString(),
            userId = reservation.userId.toString(),
            userName = userName,
            userEmail = email,
            blockId = reservation.blockId?.toString(),
            date = reservation.date.toString(),
            startTime = reservation.startTime.toString(),
            endTime = reservation.endTime.toString(),
            status = reservation.status,
            creditsUsed = reservation.creditsUsed,
            pricingItemId = reservation.pricingItemId?.toString(),
            pricingItemName = pricingItemName,
            createdAt = reservation.createdAt.toString(),
            cancelledAt = reservation.cancelledAt?.toString(),
            completedAt = reservation.completedAt?.toString(),
            note = reservation.note,
            locationId = location?.id?.toString(),
            locationName = location?.nameCs,
            locationAddress = location?.addressCs,
            locationColor = location?.color
        )
    }

    /**
     * Convert Reservation entity to ReservationDTO, fetching user from repository.
     */
    fun toDTO(reservation: Reservation): ReservationDTO {
        val user = userRepository.findById(reservation.userId).orElse(null)
        val pricingItemName = reservation.pricingItemId?.let { pricingItemId ->
            pricingItemRepository.findById(pricingItemId).orElse(null)?.nameCs
        }
        val location = resolveLocation(reservation)
        return toDTO(
            reservation,
            user?.firstName,
            user?.lastName,
            user?.email,
            pricingItemName,
            location
        )
    }

    /**
     * Batch convert reservations to DTOs, efficiently fetching users and pricing items.
     */
    fun toDTOBatch(reservations: List<Reservation>): List<ReservationDTO> {
        if (reservations.isEmpty()) return emptyList()

        // Batch fetch users
        val userIds = reservations.map { it.userId }.distinct()
        val usersMap = userRepository.findAllById(userIds).associateBy { it.id }

        // Batch fetch pricing items
        val pricingItemIds = reservations.mapNotNull { it.pricingItemId }.distinct()
        val pricingItemsMap = if (pricingItemIds.isNotEmpty()) {
            pricingItemRepository.findAllById(pricingItemIds).associateBy { it.id }
        } else {
            emptyMap()
        }

        val locationMap = resolveLocationMap(reservations)

        return reservations.map { reservation ->
            val user = usersMap[reservation.userId]
            val pricingItemName = reservation.pricingItemId?.let { pricingItemsMap[it]?.nameCs }
            toDTO(
                reservation,
                user?.firstName,
                user?.lastName,
                user?.email,
                pricingItemName,
                locationMap[reservation.id]
            )
        }
    }

    private fun resolveLocation(reservation: Reservation): TrainingLocation? {
        val slotLocationId = reservation.slotId?.let { slotRepository.findById(it).orElse(null)?.locationId }
        val locationId = slotLocationId
            ?: reservation.blockId?.let { blockRepository.findById(it).orElse(null)?.locationId }
        return locationId?.let { locationRepository.findById(it).orElse(null) }
    }

    private fun resolveLocationMap(reservations: List<Reservation>): Map<UUID, TrainingLocation> {
        val slotIds = reservations.mapNotNull { it.slotId }.toSet()
        val blockIds = reservations.mapNotNull { it.blockId }.toSet()
        val slotsMap = if (slotIds.isNotEmpty()) {
            slotRepository.findAllById(slotIds).associateBy { it.id }
        } else emptyMap()
        val blocksMap = if (blockIds.isNotEmpty()) {
            blockRepository.findAllById(blockIds).associateBy { it.id }
        } else emptyMap()
        val locationIds = (slotsMap.values.mapNotNull { it.locationId } +
            blocksMap.values.mapNotNull { it.locationId }).toSet()
        val locations = if (locationIds.isNotEmpty()) {
            locationRepository.findAllById(locationIds).associateBy { it.id }
        } else emptyMap()
        return reservations.mapNotNull { reservation ->
            val slotLocId = reservation.slotId?.let { slotsMap[it]?.locationId }
            val locId = slotLocId ?: reservation.blockId?.let { blocksMap[it]?.locationId }
            val loc = locId?.let { locations[it] }
            if (loc != null) reservation.id to loc else null
        }.toMap()
    }

    /**
     * Convert Reservation to calendar event format for admin view.
     */
    fun toCalendarEvent(reservation: Reservation, user: User?): ReservationCalendarEvent {
        val title = user?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim() }
            ?.ifEmpty { user.email }
            ?: "Unknown Client"

        return ReservationCalendarEvent(
            id = reservation.id.toString(),
            title = title,
            start = "${reservation.date}T${reservation.startTime}",
            end = "${reservation.date}T${reservation.endTime}",
            status = reservation.status,
            clientName = user?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim().ifEmpty { null } },
            clientEmail = user?.email
        )
    }

    /**
     * Batch convert reservations to calendar events.
     */
    fun toCalendarEventBatch(reservations: List<Reservation>): List<ReservationCalendarEvent> {
        if (reservations.isEmpty()) return emptyList()

        val userIds = reservations.map { it.userId }.distinct()
        val usersMap = userRepository.findAllById(userIds).associateBy { it.id }

        return reservations.map { reservation ->
            val user = usersMap[reservation.userId]
            toCalendarEvent(reservation, user)
        }
    }
}
