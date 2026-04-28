package com.fitness.service

import com.fitness.IntegrationTestBase
import com.fitness.TestFixtures
import com.fitness.dto.AdminCreateReservationRequest
import com.fitness.dto.CreateFeedbackRequest
import com.fitness.dto.CreateSlotRequest
import com.fitness.entity.Reservation
import com.fitness.entity.SlotPricingItem
import com.fitness.entity.SlotStatus
import com.fitness.repository.PricingItemRepository
import com.fitness.repository.ReservationRepository
import com.fitness.repository.SlotPricingItemRepository
import com.fitness.repository.SlotRepository
import com.fitness.repository.UserRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalTime

class ProductionReleaseGuardsIT : IntegrationTestBase() {

    @Autowired private lateinit var reservationService: ReservationService
    @Autowired private lateinit var slotService: SlotService
    @Autowired private lateinit var feedbackService: FeedbackService
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var slotRepository: SlotRepository
    @Autowired private lateinit var reservationRepository: ReservationRepository
    @Autowired private lateinit var pricingItemRepository: PricingItemRepository
    @Autowired private lateinit var slotPricingItemRepository: SlotPricingItemRepository
    @Autowired private lateinit var entityManager: EntityManager

    @Test
    fun `admin reservation deducts the slot pricing item credits`() {
        val admin = userRepository.save(TestFixtures.adminUser())
        val client = userRepository.save(TestFixtures.user(credits = 10, trainerId = admin.id))
        val slot = slotRepository.save(TestFixtures.slot(adminId = admin.id, status = SlotStatus.UNLOCKED))
        val pricingItem = pricingItemRepository.save(TestFixtures.pricingItem(credits = 2, adminId = admin.id))
        slotPricingItemRepository.save(SlotPricingItem(slotId = slot.id!!, pricingItemId = pricingItem.id!!))

        val created = reservationService.adminCreateReservation(
            AdminCreateReservationRequest(
                userId = client.id!!.toString(),
                date = slot.date.toString(),
                startTime = slot.startTime.toString(),
                endTime = slot.endTime.toString(),
                blockId = slot.id.toString(),
                deductCredits = true
            ),
            admin.id!!.toString()
        )

        entityManager.flush()
        entityManager.clear()
        assertThat(created.creditsUsed).isEqualTo(2)
        assertThat(created.pricingItemId).isEqualTo(pricingItem.id.toString())
        assertThat(userRepository.findById(client.id!!).orElseThrow().credits).isEqualTo(8)
    }

    @Test
    fun `admin reservation rejects a past slot time`() {
        val admin = userRepository.save(TestFixtures.adminUser())
        val client = userRepository.save(TestFixtures.user(credits = 10, trainerId = admin.id))
        val slot = slotRepository.save(
            TestFixtures.slot(
                date = LocalDate.now().minusDays(1),
                adminId = admin.id,
                status = SlotStatus.UNLOCKED
            )
        )

        val ex = assertThrows(IllegalArgumentException::class.java) {
            reservationService.adminCreateReservation(
                AdminCreateReservationRequest(
                    userId = client.id!!.toString(),
                    date = slot.date.toString(),
                    startTime = slot.startTime.toString(),
                    endTime = slot.endTime.toString(),
                    blockId = slot.id!!.toString(),
                    deductCredits = true
                ),
                admin.id!!.toString()
            )
        }
        assertThat(ex.message).contains("past time")
    }

    @Test
    fun `admin slot creation rejects past times`() {
        val admin = userRepository.save(TestFixtures.adminUser())

        val ex = assertThrows(IllegalArgumentException::class.java) {
            slotService.createSlot(
                CreateSlotRequest(
                    date = LocalDate.now().minusDays(1).toString(),
                    startTime = "10:00",
                    durationMinutes = 60
                ),
                admin.id!!
            )
        }
        assertThat(ex.message).contains("past")
    }

    @Test
    fun `feedback rejects future reservations`() {
        val admin = userRepository.save(TestFixtures.adminUser())
        val client = userRepository.save(TestFixtures.user(credits = 10, trainerId = admin.id))
        val slot = slotRepository.save(TestFixtures.slot(adminId = admin.id, status = SlotStatus.UNLOCKED))
        val reservation = reservationRepository.save(
            Reservation(
                userId = client.id!!,
                slotId = slot.id,
                date = slot.date,
                startTime = slot.startTime,
                endTime = slot.endTime
            )
        )

        val ex = assertThrows(IllegalArgumentException::class.java) {
            feedbackService.createFeedback(
                client.id!!.toString(),
                CreateFeedbackRequest(reservationId = reservation.id!!.toString(), rating = 5, comment = "Great")
            )
        }
        assertThat(ex.message).contains("after the reservation ends")
    }

}
