package com.fitness.mapper

import com.fitness.entity.AvailabilityBlock
import com.fitness.entity.PricingItem
import com.fitness.entity.Reservation
import com.fitness.entity.Slot
import com.fitness.entity.SlotStatus
import com.fitness.entity.TrainingLocation
import com.fitness.entity.User
import com.fitness.repository.AvailabilityBlockRepository
import com.fitness.repository.PricingItemRepository
import com.fitness.repository.SlotRepository
import com.fitness.repository.TrainingLocationRepository
import com.fitness.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class ReservationMapperTest {

    private val userRepository = mockk<UserRepository>()
    private val pricingItemRepository = mockk<PricingItemRepository>()
    private val slotRepository = mockk<SlotRepository>()
    private val blockRepository = mockk<AvailabilityBlockRepository>()
    private val locationRepository = mockk<TrainingLocationRepository>()
    private val mapper = ReservationMapper(
        userRepository,
        pricingItemRepository,
        slotRepository,
        blockRepository,
        locationRepository,
    )

    @Test
    fun `toDTO maps reservation with prefetched user pricing and location data`() {
        val reservationId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val pricingItemId = UUID.randomUUID()
        val slotId = UUID.randomUUID()
        val locationId = UUID.randomUUID()
        val createdAt = Instant.parse("2026-04-18T10:00:00Z")
        val cancelledAt = createdAt.plusSeconds(3600)
        val completedAt = createdAt.plusSeconds(7200)
        val location = trainingLocation(locationId)
        val reservation = reservation(
            id = reservationId,
            userId = userId,
            slotId = slotId,
            pricingItemId = pricingItemId,
            status = "completed",
            creditsUsed = 2,
            createdAt = createdAt,
            cancelledAt = cancelledAt,
            completedAt = completedAt,
            note = "internal note",
        )

        val dto = mapper.toDTO(
            reservation = reservation,
            firstName = "Eva",
            lastName = "Novak",
            email = "client@example.com",
            pricingItemName = "Silovy trenink",
            location = location,
        )

        assertEquals(reservationId.toString(), dto.id)
        assertEquals(userId.toString(), dto.userId)
        assertEquals("Eva Novak", dto.userName)
        assertEquals("client@example.com", dto.userEmail)
        assertEquals(slotId.toString(), dto.slotId)
        assertEquals("2026-04-20", dto.date)
        assertEquals("09:00", dto.startTime)
        assertEquals("10:00", dto.endTime)
        assertEquals("completed", dto.status)
        assertEquals(2, dto.creditsUsed)
        assertEquals(pricingItemId.toString(), dto.pricingItemId)
        assertEquals("Silovy trenink", dto.pricingItemName)
        assertEquals(createdAt.toString(), dto.createdAt)
        assertEquals(cancelledAt.toString(), dto.cancelledAt)
        assertEquals(completedAt.toString(), dto.completedAt)
        assertEquals("internal note", dto.note)
        assertEquals(locationId.toString(), dto.locationId)
        assertEquals("Studio A", dto.locationName)
        assertEquals("Adresa 1", dto.locationAddress)
        assertEquals("#123456", dto.locationColor)
    }

    @Test
    fun `toDTO resolves user pricing and slot location`() {
        val userId = UUID.randomUUID()
        val pricingItemId = UUID.randomUUID()
        val slotId = UUID.randomUUID()
        val locationId = UUID.randomUUID()
        val reservation = reservation(
            id = UUID.randomUUID(),
            userId = userId,
            slotId = slotId,
            pricingItemId = pricingItemId,
        )
        every { userRepository.findById(userId) } returns Optional.of(user(userId, firstName = "Eva", lastName = "Novak"))
        every { pricingItemRepository.findById(pricingItemId) } returns Optional.of(pricingItem(pricingItemId, "Kondicni trenink"))
        every { slotRepository.findById(slotId) } returns Optional.of(slot(slotId, locationId))
        every { locationRepository.findById(locationId) } returns Optional.of(trainingLocation(locationId))

        val dto = mapper.toDTO(reservation)

        assertEquals("Eva Novak", dto.userName)
        assertEquals("client@example.com", dto.userEmail)
        assertEquals("Kondicni trenink", dto.pricingItemName)
        assertEquals(locationId.toString(), dto.locationId)
        assertEquals("Studio A", dto.locationName)
    }

    @Test
    fun `toDTOBatch resolves slot and block locations in batches`() {
        val userOneId = UUID.randomUUID()
        val userTwoId = UUID.randomUUID()
        val slotId = UUID.randomUUID()
        val blockId = UUID.randomUUID()
        val slotLocationId = UUID.randomUUID()
        val blockLocationId = UUID.randomUUID()
        val pricingItemId = UUID.randomUUID()
        val reservations = listOf(
            reservation(
                id = UUID.randomUUID(),
                userId = userOneId,
                slotId = slotId,
                pricingItemId = pricingItemId,
            ),
            reservation(
                id = UUID.randomUUID(),
                userId = userTwoId,
                blockId = blockId,
                pricingItemId = pricingItemId,
            ),
        )
        every { userRepository.findAllById(any<Iterable<UUID>>()) } returns listOf(
            user(userOneId, firstName = "Eva", lastName = "Novak"),
            user(userTwoId, firstName = null, lastName = null, email = "fallback@example.com"),
        )
        every { pricingItemRepository.findAllById(any<Iterable<UUID>>()) } returns listOf(pricingItem(pricingItemId, "Silovy trenink"))
        every { slotRepository.findAllById(any<Iterable<UUID>>()) } returns listOf(slot(slotId, slotLocationId))
        every { blockRepository.findAllById(any<Iterable<UUID>>()) } returns listOf(availabilityBlock(blockId, blockLocationId))
        every { locationRepository.findAllById(any<Iterable<UUID>>()) } returns listOf(
            trainingLocation(slotLocationId, name = "Studio A", color = "#111111"),
            trainingLocation(blockLocationId, name = "Venkovni zona", color = "#222222"),
        )

        val dtos = mapper.toDTOBatch(reservations)

        assertEquals(listOf("Eva Novak", null), dtos.map { it.userName })
        assertEquals(listOf("Studio A", "Venkovni zona"), dtos.map { it.locationName })
        assertEquals(listOf("#111111", "#222222"), dtos.map { it.locationColor })
        assertEquals(listOf("Silovy trenink", "Silovy trenink"), dtos.map { it.pricingItemName })
        verify(exactly = 1) { userRepository.findAllById(any<Iterable<UUID>>()) }
        verify(exactly = 1) { pricingItemRepository.findAllById(any<Iterable<UUID>>()) }
        verify(exactly = 1) { slotRepository.findAllById(any<Iterable<UUID>>()) }
        verify(exactly = 1) { blockRepository.findAllById(any<Iterable<UUID>>()) }
        verify(exactly = 1) { locationRepository.findAllById(any<Iterable<UUID>>()) }
    }

    @Test
    fun `calendar events use client names email fallback and unknown client fallback`() {
        val namedUserId = UUID.randomUUID()
        val emailOnlyUserId = UUID.randomUUID()
        val missingUserId = UUID.randomUUID()
        val namedReservation = reservation(id = UUID.randomUUID(), userId = namedUserId, status = "confirmed")
        val emailOnlyReservation = reservation(id = UUID.randomUUID(), userId = emailOnlyUserId, status = "cancelled")
        val unknownReservation = reservation(id = UUID.randomUUID(), userId = missingUserId, status = "completed")
        every { userRepository.findAllById(any<Iterable<UUID>>()) } returns listOf(
            user(namedUserId, firstName = "Eva", lastName = "Novak"),
            user(emailOnlyUserId, firstName = null, lastName = null, email = "email.only@example.com"),
        )

        val events = mapper.toCalendarEventBatch(listOf(namedReservation, emailOnlyReservation, unknownReservation))

        assertEquals(listOf("Eva Novak", "email.only@example.com", "Unknown Client"), events.map { it.title })
        assertEquals(listOf("Eva Novak", null, null), events.map { it.clientName })
        assertEquals(listOf("client@example.com", "email.only@example.com", null), events.map { it.clientEmail })
        assertEquals("2026-04-20T09:00", events.first().start)
        assertEquals("2026-04-20T10:00", events.first().end)
    }

    @Test
    fun `empty batch methods return empty lists`() {
        assertEquals(emptyList<Any>(), mapper.toDTOBatch(emptyList()))
        assertEquals(emptyList<Any>(), mapper.toCalendarEventBatch(emptyList()))
    }

    @Test
    fun `toCalendarEvent maps unknown direct user to fallback title`() {
        val reservation = reservation(id = UUID.randomUUID(), userId = UUID.randomUUID())

        val event = mapper.toCalendarEvent(reservation, null)

        assertEquals("Unknown Client", event.title)
        assertNull(event.clientName)
        assertNull(event.clientEmail)
    }

    private fun reservation(
        id: UUID,
        userId: UUID,
        blockId: UUID? = null,
        slotId: UUID? = null,
        pricingItemId: UUID? = null,
        status: String = "confirmed",
        creditsUsed: Int = 1,
        createdAt: Instant = Instant.parse("2026-04-18T10:00:00Z"),
        cancelledAt: Instant? = null,
        completedAt: Instant? = null,
        note: String? = null,
    ) = Reservation(
        id = id,
        userId = userId,
        blockId = blockId,
        slotId = slotId,
        date = LocalDate.parse("2026-04-20"),
        startTime = LocalTime.parse("09:00"),
        endTime = LocalTime.parse("10:00"),
        status = status,
        creditsUsed = creditsUsed,
        pricingItemId = pricingItemId,
        createdAt = createdAt,
        cancelledAt = cancelledAt,
        completedAt = completedAt,
        note = note,
    )

    private fun user(
        id: UUID,
        firstName: String?,
        lastName: String?,
        email: String = "client@example.com",
    ) = User(
        id = id,
        email = email,
        passwordHash = "hash",
        firstName = firstName,
        lastName = lastName,
    )

    private fun pricingItem(
        id: UUID,
        name: String,
    ) = PricingItem(
        id = id,
        nameCs = name,
        credits = 1,
    )

    private fun slot(
        id: UUID,
        locationId: UUID?,
    ) = Slot(
        id = id,
        date = LocalDate.parse("2026-04-20"),
        startTime = LocalTime.parse("09:00"),
        endTime = LocalTime.parse("10:00"),
        status = SlotStatus.UNLOCKED,
        adminId = UUID.randomUUID(),
        locationId = locationId,
    )

    private fun availabilityBlock(
        id: UUID,
        locationId: UUID?,
    ) = AvailabilityBlock(
        id = id,
        daysOfWeek = "1",
        startTime = LocalTime.parse("09:00"),
        endTime = LocalTime.parse("10:00"),
        locationId = locationId,
    )

    private fun trainingLocation(
        id: UUID,
        name: String = "Studio A",
        color: String = "#123456",
    ) = TrainingLocation(
        id = id,
        nameCs = name,
        addressCs = "Adresa 1",
        color = color,
    )
}
