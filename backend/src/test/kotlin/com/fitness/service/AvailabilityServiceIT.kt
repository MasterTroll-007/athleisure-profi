package com.fitness.service

import com.fitness.IntegrationTestBase
import com.fitness.TestFixtures
import com.fitness.entity.Reservation
import com.fitness.entity.Slot
import com.fitness.entity.SlotStatus
import com.fitness.entity.User
import com.fitness.repository.ReservationRepository
import com.fitness.repository.SlotRepository
import com.fitness.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class AvailabilityServiceIT : IntegrationTestBase() {

    @Autowired private lateinit var availabilityService: AvailabilityService
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var slotRepository: SlotRepository
    @Autowired private lateinit var reservationRepository: ReservationRepository

    private fun createAdmin(
        adjacentBookingRequired: Boolean = true,
        calendarStartHour: Int = 6,
        calendarEndHour: Int = 22
    ): User =
        userRepository.save(
            TestFixtures.adminUser().copy(
                adjacentBookingRequired = adjacentBookingRequired,
                calendarStartHour = calendarStartHour,
                calendarEndHour = calendarEndHour
            )
        )

    private fun createClient(adminId: UUID): User =
        userRepository.save(TestFixtures.user(trainerId = adminId))

    private fun createSlot(adminId: UUID, date: LocalDate, start: LocalTime, status: SlotStatus = SlotStatus.UNLOCKED): Slot =
        slotRepository.save(TestFixtures.slot(date = date, start = start, status = status, adminId = adminId))

    private fun reserveSlot(user: User, slot: Slot) {
        reservationRepository.save(
            Reservation(
                userId = user.id!!,
                slotId = slot.id!!,
                date = slot.date,
                startTime = slot.startTime,
                endTime = slot.endTime,
                creditsUsed = 1
            )
        )
    }

    @Test
    fun `disabled adjacent booking returns non adjacent free slots`() {
        val admin = createAdmin(adjacentBookingRequired = false)
        val client = createClient(admin.id!!)
        val otherClient = createClient(admin.id!!)
        val date = LocalDate.now().plusDays(7)
        val beforeReserved = createSlot(admin.id!!, date, LocalTime.of(9, 0))
        val reserved = createSlot(admin.id!!, date, LocalTime.of(10, 0), SlotStatus.RESERVED)
        val nonAdjacent = createSlot(admin.id!!, date, LocalTime.of(13, 0))
        reserveSlot(otherClient, reserved)

        val slots = availabilityService.getAvailableSlots(date, client.id!!.toString())

        assertThat(slots.map { it.slotId }).contains(
            beforeReserved.id.toString(),
            reserved.id.toString(),
            nonAdjacent.id.toString()
        )
        assertThat(slots.first { it.slotId == nonAdjacent.id.toString() }.isAvailable).isTrue()
    }

    @Test
    fun `disabled adjacent booking keeps free slots visible after user's reservation`() {
        val admin = createAdmin(adjacentBookingRequired = false)
        val client = createClient(admin.id!!)
        val date = LocalDate.now().plusDays(7)
        val reserved = createSlot(admin.id!!, date, LocalTime.of(10, 0), SlotStatus.RESERVED)
        val nonAdjacent = createSlot(admin.id!!, date, LocalTime.of(13, 0))
        reserveSlot(client, reserved)

        val slots = availabilityService.getAvailableSlots(date, client.id!!.toString())

        assertThat(slots.map { it.slotId }).contains(nonAdjacent.id.toString())
        assertThat(slots.first { it.slotId == nonAdjacent.id.toString() }.isAvailable).isTrue()
    }

    @Test
    fun `enabled adjacent booking keeps adjacent free slots visible after user's reservation`() {
        val admin = createAdmin(adjacentBookingRequired = true)
        val client = createClient(admin.id!!)
        val date = LocalDate.now().plusDays(7)
        val reserved = createSlot(admin.id!!, date, LocalTime.of(10, 0), SlotStatus.RESERVED)
        val adjacent = createSlot(admin.id!!, date, LocalTime.of(11, 0))
        val nonAdjacent = createSlot(admin.id!!, date, LocalTime.of(13, 0))
        reserveSlot(client, reserved)

        val slots = availabilityService.getAvailableSlots(date, client.id!!.toString())

        assertThat(slots.map { it.slotId }).contains(adjacent.id.toString())
        assertThat(slots.first { it.slotId == adjacent.id.toString() }.isAvailable).isTrue()
        assertThat(slots.map { it.slotId }).doesNotContain(nonAdjacent.id.toString())
    }

    @Test
    fun `enabled adjacent booking hides non adjacent free slots`() {
        val admin = createAdmin(adjacentBookingRequired = true)
        val client = createClient(admin.id!!)
        val otherClient = createClient(admin.id!!)
        val date = LocalDate.now().plusDays(7)
        val beforeReserved = createSlot(admin.id!!, date, LocalTime.of(9, 0))
        val reserved = createSlot(admin.id!!, date, LocalTime.of(10, 0), SlotStatus.RESERVED)
        val nonAdjacent = createSlot(admin.id!!, date, LocalTime.of(13, 0))
        reserveSlot(otherClient, reserved)

        val slots = availabilityService.getAvailableSlots(date, client.id!!.toString())

        assertThat(slots.map { it.slotId }).contains(
            beforeReserved.id.toString(),
            reserved.id.toString()
        )
        assertThat(slots.map { it.slotId }).doesNotContain(nonAdjacent.id.toString())
    }

    @Test
    fun `adjacent booking ignores reservations from other trainers`() {
        val admin = createAdmin(adjacentBookingRequired = true)
        val client = createClient(admin.id!!)
        val otherAdmin = createAdmin(adjacentBookingRequired = true)
        val otherClient = createClient(otherAdmin.id!!)
        val date = LocalDate.now().plusDays(7)
        val trainerSlot = createSlot(admin.id!!, date, LocalTime.of(13, 0))
        val otherTrainerReserved = createSlot(otherAdmin.id!!, date, LocalTime.of(10, 0), SlotStatus.RESERVED)
        reserveSlot(otherClient, otherTrainerReserved)

        val slots = availabilityService.getAvailableSlots(date, client.id!!.toString())

        assertThat(slots.map { it.slotId }).contains(trainerSlot.id.toString())
        assertThat(slots.first { it.slotId == trainerSlot.id.toString() }.isAvailable).isTrue()
    }

    @Test
    fun `admin calendar returns only slots for the authenticated trainer`() {
        val admin = createAdmin(calendarStartHour = 10, calendarEndHour = 21)
        val otherAdmin = createAdmin()
        val date = LocalDate.now().plusDays(7)
        val trainerSlot = createSlot(admin.id!!, date, LocalTime.of(10, 0))
        val outsideRangeSlot = createSlot(admin.id!!, date, LocalTime.of(8, 0))
        val otherTrainerSlot = createSlot(otherAdmin.id!!, date, LocalTime.of(10, 0))

        val slots = availabilityService.getAdminCalendarSlots(date, date, admin.id!!)

        assertThat(slots.map { it.slotId }).contains(trainerSlot.id.toString())
        assertThat(slots.map { it.slotId }).doesNotContain(outsideRangeSlot.id.toString())
        assertThat(slots.map { it.slotId }).doesNotContain(otherTrainerSlot.id.toString())
    }

    @Test
    fun `client availability follows trainer calendar hours`() {
        val admin = createAdmin(adjacentBookingRequired = false, calendarStartHour = 10, calendarEndHour = 21)
        val client = createClient(admin.id!!)
        val date = LocalDate.now().plusDays(7)
        val visibleSlot = createSlot(admin.id!!, date, LocalTime.of(10, 0))
        val hiddenSlot = createSlot(admin.id!!, date, LocalTime.of(8, 0))

        val slots = availabilityService.getAvailableSlots(date, client.id!!.toString())

        assertThat(slots.map { it.slotId }).contains(visibleSlot.id.toString())
        assertThat(slots.map { it.slotId }).doesNotContain(hiddenSlot.id.toString())
    }
}
