package com.fitness.service

import com.fitness.dto.PricingItemSummary
import com.fitness.entity.Reservation
import com.fitness.entity.Slot
import com.fitness.entity.SlotStatus
import com.fitness.entity.TrainingLocation
import com.fitness.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class SlotDtoAssemblerTest {
    private val assembler = SlotDtoAssembler()

    @Test
    fun `calendar assigned user name puts last name above first name`() {
        val user = User(
            id = UUID.randomUUID(),
            email = "client@test.com",
            passwordHash = "hash",
            firstName = "Jana",
            lastName = "Novakova"
        )

        assertThat(assembler.calendarAssignedUserName(user)).isEqualTo("Novakova\nJana")
    }

    @Test
    fun `dto keeps reservation note cancellation timestamp pricing and location metadata`() {
        val slotId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val locationId = UUID.randomUUID()
        val cancelledAt = Instant.parse("2026-05-01T10:15:30Z")
        val slot = Slot(
            id = slotId,
            date = LocalDate.of(2026, 5, 5),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            durationMinutes = 60,
            status = SlotStatus.UNLOCKED,
            note = "Slot note",
            locationId = locationId,
            capacity = 2,
            createdAt = Instant.parse("2026-05-01T08:00:00Z")
        )
        val reservation = Reservation(
            id = UUID.randomUUID(),
            userId = userId,
            slotId = slotId,
            date = slot.date,
            startTime = slot.startTime,
            endTime = slot.endTime,
            note = "Reservation note"
        )
        val cancelledReservation = reservation.copy(status = "cancelled", cancelledAt = cancelledAt)
        val location = TrainingLocation(
            id = locationId,
            nameCs = "Gym centrum",
            addressCs = "Testovaci 1",
            color = "#123456"
        )

        val dto = assembler.toDto(
            slot = slot,
            status = "cancelled",
            user = User(id = userId, email = "client@test.com", passwordHash = "hash"),
            reservation = reservation,
            cancelledReservation = cancelledReservation,
            pricingItems = listOf(PricingItemSummary("price-1", "Trenink", "Training", 2)),
            currentBookings = 1,
            location = location
        )

        assertThat(dto.id).isEqualTo(slotId.toString())
        assertThat(dto.date).isEqualTo("2026-05-05")
        assertThat(dto.startTime).isEqualTo("09:00")
        assertThat(dto.endTime).isEqualTo("10:00")
        assertThat(dto.status).isEqualTo("cancelled")
        assertThat(dto.note).isEqualTo("Reservation note")
        assertThat(dto.cancelledAt).isEqualTo(cancelledAt.toString())
        assertThat(dto.pricingItems).hasSize(1)
        assertThat(dto.capacity).isEqualTo(2)
        assertThat(dto.currentBookings).isEqualTo(1)
        assertThat(dto.locationName).isEqualTo("Gym centrum")
        assertThat(dto.locationAddress).isEqualTo("Testovaci 1")
        assertThat(dto.locationColor).isEqualTo("#123456")
    }
}
