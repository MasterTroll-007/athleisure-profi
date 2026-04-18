package com.fitness.service

import com.fitness.IntegrationTestBase
import com.fitness.TestFixtures
import com.fitness.dto.CreateReservationRequest
import com.fitness.repository.ReservationRepository
import com.fitness.repository.SlotRepository
import com.fitness.repository.UserRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalTime

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

    @Test
    fun `happy path deducts one credit and creates confirmed reservation`() {
        val user = userRepository.save(TestFixtures.user(credits = 5))
        val slot = slotRepository.save(TestFixtures.slot())

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
        val user = userRepository.save(TestFixtures.user(credits = 0))
        val slot = slotRepository.save(TestFixtures.slot())

        assertThrows(IllegalArgumentException::class.java) {
            service.createReservation(user.id!!.toString(),
                createReq(slot.id!!.toString(), slot.date, slot.startTime))
        }

        assertThat(reservationRepository.findByUserId(user.id!!)).isEmpty()
    }

    @Test
    fun `past date is rejected`() {
        val user = userRepository.save(TestFixtures.user())
        val slot = slotRepository.save(TestFixtures.slot(date = LocalDate.now().minusDays(1)))

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.createReservation(user.id!!.toString(),
                createReq(slot.id!!.toString(), slot.date, slot.startTime))
        }
        assertThat(ex.message).contains("past date")
    }

    @Test
    fun `blocked user cannot create reservation`() {
        val user = userRepository.save(TestFixtures.user(isBlocked = true))
        val slot = slotRepository.save(TestFixtures.slot())

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.createReservation(user.id!!.toString(),
                createReq(slot.id!!.toString(), slot.date, slot.startTime))
        }
        assertThat(ex.message).contains("blocked")
    }

    @Test
    fun `booking further than 90 days ahead is rejected`() {
        val user = userRepository.save(TestFixtures.user())
        val slot = slotRepository.save(
            TestFixtures.slot(date = LocalDate.now().plusDays(200))
        )

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.createReservation(user.id!!.toString(),
                createReq(slot.id!!.toString(), slot.date, slot.startTime))
        }
        assertThat(ex.message).contains("90 days")
    }

    @Test
    fun `second booking on the same day is rejected`() {
        val user = userRepository.save(TestFixtures.user(credits = 10))
        val date = LocalDate.now().plusDays(7)
        val s1 = slotRepository.save(TestFixtures.slot(date = date, start = LocalTime.of(9, 0)))
        val s2 = slotRepository.save(TestFixtures.slot(date = date, start = LocalTime.of(11, 0)))

        service.createReservation(user.id!!.toString(),
            createReq(s1.id!!.toString(), date, s1.startTime))

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.createReservation(user.id!!.toString(),
                createReq(s2.id!!.toString(), date, s2.startTime))
        }
        assertThat(ex.message).contains("rezervaci na tento den")
    }
}
