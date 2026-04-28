package com.fitness.service

import com.fitness.IntegrationTestBase
import com.fitness.TestFixtures
import com.fitness.dto.AdminCreateReservationRequest
import com.fitness.dto.CreateReservationRequest
import com.fitness.entity.Slot
import com.fitness.entity.SlotStatus
import com.fitness.entity.User
import com.fitness.repository.ReservationRepository
import com.fitness.repository.SlotRepository
import com.fitness.repository.UserRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Integration tests for credit deduction, booking constraints and the
 * pessimistic-lock overbooking guard. Requires Docker for Testcontainers.
 */
class ReservationServiceIT : IntegrationTestBase() {

    @Autowired private lateinit var service: ReservationService
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var slotRepository: SlotRepository
    @Autowired private lateinit var reservationRepository: ReservationRepository
    @Autowired private lateinit var entityManager: EntityManager

    private fun createReq(slotId: String, date: LocalDate, time: LocalTime) =
        CreateReservationRequest(
            date = date.toString(),
            startTime = time.toString(),
            endTime = time.plusMinutes(60).toString(),
            blockId = slotId,
            pricingItemId = null,
        )

    private fun createAdmin(adjacentBookingRequired: Boolean = true) =
        userRepository.save(TestFixtures.adminUser().copy(adjacentBookingRequired = adjacentBookingRequired))

    private fun createClient(adminId: UUID, credits: Int = 10, isBlocked: Boolean = false): User =
        userRepository.save(TestFixtures.user(credits = credits, isBlocked = isBlocked, trainerId = adminId))

    private fun createSlot(
        adminId: UUID,
        date: LocalDate = LocalDate.now().plusDays(7),
        start: LocalTime = LocalTime.of(10, 0),
        status: SlotStatus = SlotStatus.UNLOCKED,
        capacity: Int = 1,
    ): Slot =
        slotRepository.save(
            TestFixtures.slot(date = date, start = start, status = status, adminId = adminId, capacity = capacity)
        )

    private fun createAdminReq(userId: String, slot: Slot) =
        AdminCreateReservationRequest(
            userId = userId,
            date = slot.date.toString(),
            startTime = slot.startTime.toString(),
            endTime = slot.endTime.toString(),
            blockId = slot.id!!.toString(),
            deductCredits = false,
        )

    @Test
    fun `happy path deducts one credit and creates confirmed reservation`() {
        val admin = createAdmin()
        val user = createClient(admin.id!!, credits = 5)
        val slot = createSlot(admin.id!!)

        val dto = service.createReservation(user.id!!.toString(),
            createReq(slot.id!!.toString(), slot.date, slot.startTime))

        assertThat(dto.status).isEqualTo("confirmed")
        assertThat(dto.creditsUsed).isEqualTo(1)

        // deductCreditsIfSufficient is an @Modifying query that bypasses the
        // first-level cache; clear it so the findById below hits the DB.
        entityManager.flush()
        entityManager.clear()
        val refreshed = userRepository.findById(user.id!!).orElseThrow()
        assertThat(refreshed.credits).isEqualTo(4)
    }

    @Test
    fun `insufficient credits throws and does not persist reservation`() {
        val admin = createAdmin()
        val user = createClient(admin.id!!, credits = 0)
        val slot = createSlot(admin.id!!)

        assertThrows(IllegalArgumentException::class.java) {
            service.createReservation(user.id!!.toString(),
                createReq(slot.id!!.toString(), slot.date, slot.startTime))
        }

        assertThat(reservationRepository.findByUserId(user.id!!)).isEmpty()
    }

    @Test
    fun `past date is rejected`() {
        val admin = createAdmin()
        val user = createClient(admin.id!!)
        val slot = createSlot(admin.id!!, date = LocalDate.now().minusDays(1))

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.createReservation(user.id!!.toString(),
                createReq(slot.id!!.toString(), slot.date, slot.startTime))
        }
        assertThat(ex.message).contains("past date")
    }

    @Test
    fun `blocked user cannot create reservation`() {
        val admin = createAdmin()
        val user = createClient(admin.id!!, isBlocked = true)
        val slot = createSlot(admin.id!!)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.createReservation(user.id!!.toString(),
                createReq(slot.id!!.toString(), slot.date, slot.startTime))
        }
        assertThat(ex.message).contains("blocked")
    }

    @Test
    fun `booking further than 90 days ahead is rejected`() {
        val admin = createAdmin()
        val user = createClient(admin.id!!)
        val slot = createSlot(admin.id!!, date = LocalDate.now().plusDays(200))

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.createReservation(user.id!!.toString(),
                createReq(slot.id!!.toString(), slot.date, slot.startTime))
        }
        assertThat(ex.message).contains("90 days")
    }

    @Test
    fun `second booking on the same day is rejected`() {
        val admin = createAdmin()
        val user = createClient(admin.id!!, credits = 10)
        val date = LocalDate.now().plusDays(7)
        val s1 = createSlot(admin.id!!, date = date, start = LocalTime.of(9, 0))
        val s2 = createSlot(admin.id!!, date = date, start = LocalTime.of(11, 0))

        service.createReservation(user.id!!.toString(),
            createReq(s1.id!!.toString(), date, s1.startTime))

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.createReservation(user.id!!.toString(),
                createReq(s2.id!!.toString(), date, s2.startTime))
        }
        assertThat(ex.message).contains("rezervaci na tento den")
    }

    @Test
    fun `second booking on the same day is allowed when adjacent booking is disabled`() {
        val admin = createAdmin(adjacentBookingRequired = false)
        val user = createClient(admin.id!!, credits = 10)
        val date = LocalDate.now().plusDays(7)
        val s1 = createSlot(admin.id!!, date = date, start = LocalTime.of(9, 0))
        val s2 = createSlot(admin.id!!, date = date, start = LocalTime.of(13, 0))

        service.createReservation(user.id!!.toString(),
            createReq(s1.id!!.toString(), date, s1.startTime))
        val dto = service.createReservation(user.id!!.toString(),
            createReq(s2.id!!.toString(), date, s2.startTime))

        assertThat(dto.status).isEqualTo("confirmed")
        assertThat(reservationRepository.findByUserId(user.id!!)).hasSize(2)
    }

    @Test
    fun `non adjacent direct booking is rejected when adjacent booking is enabled`() {
        val admin = createAdmin(adjacentBookingRequired = true)
        val client = createClient(admin.id!!, credits = 10)
        val otherClient = createClient(admin.id!!, credits = 10)
        val date = LocalDate.now().plusDays(7)
        val reservedSlot = createSlot(admin.id!!, date = date, start = LocalTime.of(10, 0))
        val nonAdjacentSlot = createSlot(admin.id!!, date = date, start = LocalTime.of(13, 0))

        service.createReservation(otherClient.id!!.toString(),
            createReq(reservedSlot.id!!.toString(), date, reservedSlot.startTime))

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.createReservation(client.id!!.toString(),
                createReq(nonAdjacentSlot.id!!.toString(), date, nonAdjacentSlot.startTime))
        }
        assertThat(ex.message).contains("adjacent")
    }

    @Test
    fun `non adjacent direct booking is allowed when adjacent booking is disabled`() {
        val admin = createAdmin(adjacentBookingRequired = false)
        val client = createClient(admin.id!!, credits = 10)
        val otherClient = createClient(admin.id!!, credits = 10)
        val date = LocalDate.now().plusDays(7)
        val reservedSlot = createSlot(admin.id!!, date = date, start = LocalTime.of(10, 0))
        val nonAdjacentSlot = createSlot(admin.id!!, date = date, start = LocalTime.of(13, 0))

        service.createReservation(otherClient.id!!.toString(),
            createReq(reservedSlot.id!!.toString(), date, reservedSlot.startTime))
        val dto = service.createReservation(client.id!!.toString(),
            createReq(nonAdjacentSlot.id!!.toString(), date, nonAdjacentSlot.startTime))

        assertThat(dto.status).isEqualTo("confirmed")
    }

    @Test
    fun `reserved slot cannot be booked by client`() {
        val admin = createAdmin()
        val user = createClient(admin.id!!)
        val slot = createSlot(admin.id!!, status = SlotStatus.RESERVED)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.createReservation(user.id!!.toString(),
                createReq(slot.id!!.toString(), slot.date, slot.startTime))
        }
        assertThat(ex.message).contains("not available")
    }

    @Test
    fun `request time must match selected slot`() {
        val admin = createAdmin()
        val user = createClient(admin.id!!)
        val slot = createSlot(admin.id!!)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.createReservation(user.id!!.toString(),
                createReq(slot.id!!.toString(), slot.date, slot.startTime.plusHours(1)))
        }
        assertThat(ex.message).contains("does not match")
    }

    @Test
    fun `client cannot book another trainers slot`() {
        val ownAdmin = createAdmin()
        val otherAdmin = createAdmin()
        val user = createClient(ownAdmin.id!!)
        val slot = createSlot(otherAdmin.id!!)

        assertThrows(AccessDeniedException::class.java) {
            service.createReservation(user.id!!.toString(),
                createReq(slot.id!!.toString(), slot.date, slot.startTime))
        }
    }

    @Test
    fun `admin reservation keeps multi capacity slot open until full`() {
        val admin = createAdmin()
        val firstUser = createClient(admin.id!!)
        val secondUser = createClient(admin.id!!)
        val slot = createSlot(admin.id!!, capacity = 2)

        service.adminCreateReservation(
            createAdminReq(firstUser.id!!.toString(), slot),
            admin.id!!.toString()
        )

        entityManager.flush()
        entityManager.clear()
        assertThat(slotRepository.findById(slot.id!!).orElseThrow().status).isEqualTo(SlotStatus.UNLOCKED)

        service.adminCreateReservation(
            createAdminReq(secondUser.id!!.toString(), slot),
            admin.id!!.toString()
        )

        entityManager.flush()
        entityManager.clear()
        assertThat(slotRepository.findById(slot.id!!).orElseThrow().status).isEqualTo(SlotStatus.RESERVED)
    }
}
