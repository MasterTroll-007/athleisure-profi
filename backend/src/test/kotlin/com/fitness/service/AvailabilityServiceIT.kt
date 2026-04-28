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

    private fun createAdmin(adjacentBookingRequired: Boolean = true): User =
        userRepository.save(TestFixtures.adminUser().copy(adjacentBookingRequired = adjacentBookingRequired))

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

        assertThat(slots.map { it.blockId }).contains(
            beforeReserved.id.toString(),
            reserved.id.toString(),
            nonAdjacent.id.toString()
        )
        assertThat(slots.first { it.blockId == nonAdjacent.id.toString() }.isAvailable).isTrue()
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

        assertThat(slots.map { it.blockId }).contains(
            beforeReserved.id.toString(),
            reserved.id.toString()
        )
        assertThat(slots.map { it.blockId }).doesNotContain(nonAdjacent.id.toString())
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

        assertThat(slots.map { it.blockId }).contains(trainerSlot.id.toString())
        assertThat(slots.first { it.blockId == trainerSlot.id.toString() }.isAvailable).isTrue()
    }
}
