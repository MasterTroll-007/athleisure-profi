package com.fitness.service

import com.fitness.IntegrationTestBase
import com.fitness.TestFixtures
import com.fitness.dto.AdminCreateReservationRequest
import com.fitness.dto.CreateFeedbackRequest
import com.fitness.dto.CreateReservationRequest
import com.fitness.dto.CreateSlotRequest
import com.fitness.dto.TemplateSlotDTO
import com.fitness.dto.UpdateAdminSettingsRequest
import com.fitness.entity.Reservation
import com.fitness.entity.SlotPricingItem
import com.fitness.entity.SlotStatus
import com.fitness.entity.TransactionType
import com.fitness.repository.CreditTransactionRepository
import com.fitness.repository.AuditLogRepository
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
import org.springframework.data.domain.PageRequest
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class ProductionReleaseGuardsIT : IntegrationTestBase() {

    @Autowired private lateinit var adminSettingsService: AdminSettingsService
    @Autowired private lateinit var reservationService: ReservationService
    @Autowired private lateinit var slotService: SlotService
    @Autowired private lateinit var feedbackService: FeedbackService
    @Autowired private lateinit var auditLogRepository: AuditLogRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var slotRepository: SlotRepository
    @Autowired private lateinit var reservationRepository: ReservationRepository
    @Autowired private lateinit var creditTransactionRepository: CreditTransactionRepository
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
                slotId = slot.id.toString(),
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
                    slotId = slot.id!!.toString(),
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
    fun `reservation creation and cancellation are persisted in admin audit log`() {
        val admin = userRepository.save(TestFixtures.adminUser())
        val client = userRepository.save(TestFixtures.user(credits = 10, trainerId = admin.id))
        val slot = slotRepository.save(TestFixtures.slot(adminId = admin.id, status = SlotStatus.UNLOCKED))

        val created = reservationService.adminCreateReservation(
            AdminCreateReservationRequest(
                userId = client.id!!.toString(),
                date = slot.date.toString(),
                startTime = slot.startTime.toString(),
                endTime = slot.endTime.toString(),
                slotId = slot.id!!.toString(),
                deductCredits = true
            ),
            admin.id!!.toString(),
            admin.email
        )
        reservationService.adminCancelReservation(created.id, true, admin.id!!.toString(), admin.email)

        val logs = auditLogRepository.findForAdmin(admin.id!!, null, null, PageRequest.of(0, 10)).content

        assertThat(logs.map { it.action }).contains("RESERVATION_CREATED", "RESERVATION_CANCELLED")
        val createdLog = logs.first { it.action == "RESERVATION_CREATED" }
        val cancelledLog = logs.first { it.action == "RESERVATION_CANCELLED" }
        assertThat(createdLog.actorId).isEqualTo(admin.id)
        assertThat(createdLog.clientId).isEqualTo(client.id)
        assertThat(createdLog.creditsChange).isEqualTo(-1)
        assertThat(cancelledLog.actorId).isEqualTo(admin.id)
        assertThat(cancelledLog.clientId).isEqualTo(client.id)
        assertThat(cancelledLog.creditsChange).isEqualTo(1)
    }

    @Test
    fun `client reservation actions are visible in trainer audit log`() {
        val admin = userRepository.save(TestFixtures.adminUser())
        val client = userRepository.save(TestFixtures.user(credits = 10, trainerId = admin.id))
        val slot = slotRepository.save(TestFixtures.slot(adminId = admin.id, status = SlotStatus.UNLOCKED))

        val created = reservationService.createReservation(
            client.id!!.toString(),
            CreateReservationRequest(
                date = slot.date.toString(),
                startTime = slot.startTime.toString(),
                endTime = slot.endTime.toString(),
                slotId = slot.id!!.toString()
            )
        )
        reservationService.cancelReservation(client.id!!.toString(), created.id)

        val logs = auditLogRepository.findForAdmin(admin.id!!, client.id, null, PageRequest.of(0, 10)).content

        assertThat(logs.map { it.action }).contains("RESERVATION_CREATED", "RESERVATION_CANCELLED")
        assertThat(logs.all { it.actorId == client.id && it.clientId == client.id }).isTrue()
    }

    @Test
    fun `admin slot creation rejects slots outside calendar hours`() {
        val admin = userRepository.save(
            TestFixtures.adminUser().copy(calendarStartHour = 10, calendarEndHour = 21)
        )

        val ex = assertThrows(IllegalArgumentException::class.java) {
            slotService.createSlot(
                CreateSlotRequest(
                    date = LocalDate.now().plusDays(7).toString(),
                    startTime = "09:00",
                    durationMinutes = 60
                ),
                admin.id!!
            )
        }

        assertThat(ex.message).contains("calendar hours 10:00-21:00")
    }

    @Test
    fun `template application skips slots outside calendar hours`() {
        val admin = userRepository.save(
            TestFixtures.adminUser().copy(calendarStartHour = 10, calendarEndHour = 21)
        )
        val weekStart = LocalDate.now().plusDays(7).with(DayOfWeek.MONDAY)

        val created = slotService.applyTemplate(
            templateId = UUID.randomUUID(),
            weekStartDate = weekStart,
            templateSlots = listOf(
                TemplateSlotDTO(dayOfWeek = 1, startTime = "09:00", endTime = "10:00"),
                TemplateSlotDTO(dayOfWeek = 1, startTime = "10:00", endTime = "11:00")
            ),
            adminId = admin.id!!
        )

        assertThat(created).hasSize(1)
        assertThat(created.single().startTime).isEqualTo("10:00")
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

    @Test
    fun `calendar range save removes future hidden slots and refunds hidden reservations`() {
        val admin = userRepository.save(TestFixtures.adminUser().copy(calendarStartHour = 10, calendarEndHour = 22))
        val client = userRepository.save(TestFixtures.user(credits = 5, trainerId = admin.id))
        val futureDate = LocalDate.now().plusDays(7)

        val freeOutsideStart = slotRepository.save(
            TestFixtures.slot(date = futureDate, start = LocalTime.of(8, 0), adminId = admin.id, status = SlotStatus.UNLOCKED)
        )
        val freeOutsideEnd = slotRepository.save(
            TestFixtures.slot(date = futureDate, start = LocalTime.of(21, 30), durationMinutes = 60, adminId = admin.id, status = SlotStatus.UNLOCKED)
        )
        val reservedOutside = slotRepository.save(
            TestFixtures.slot(date = futureDate, start = LocalTime.of(7, 0), adminId = admin.id, status = SlotStatus.RESERVED)
        )
        val insideRange = slotRepository.save(
            TestFixtures.slot(date = futureDate, start = LocalTime.of(10, 0), adminId = admin.id, status = SlotStatus.UNLOCKED)
        )
        val pastOutside = slotRepository.save(
            TestFixtures.slot(date = LocalDate.now().minusDays(1), start = LocalTime.of(7, 0), adminId = admin.id, status = SlotStatus.UNLOCKED)
        )
        val reservation = reservationRepository.save(
            Reservation(
                userId = client.id!!,
                slotId = reservedOutside.id,
                date = reservedOutside.date,
                startTime = reservedOutside.startTime,
                endTime = reservedOutside.endTime,
                creditsUsed = 2
            )
        )

        adminSettingsService.updateSettings(
            admin.id!!,
            UpdateAdminSettingsRequest(calendarStartHour = 10, calendarEndHour = 22)
        )

        entityManager.flush()
        entityManager.clear()

        assertThat(slotRepository.findById(freeOutsideStart.id!!)).isEmpty()
        assertThat(slotRepository.findById(freeOutsideEnd.id!!)).isEmpty()
        assertThat(slotRepository.findById(reservedOutside.id!!)).isEmpty()
        assertThat(slotRepository.findById(insideRange.id!!)).isPresent()
        assertThat(slotRepository.findById(pastOutside.id!!)).isPresent()

        val cancelledReservation = reservationRepository.findById(reservation.id!!).orElseThrow()
        assertThat(cancelledReservation.status).isEqualTo("cancelled")
        assertThat(cancelledReservation.cancelledAt).isNotNull()
        assertThat(userRepository.findById(client.id!!).orElseThrow().credits).isEqualTo(7)

        val refund = creditTransactionRepository.findByUserIdOrderByCreatedAtDesc(client.id!!).first()
        assertThat(refund.type).isEqualTo(TransactionType.REFUND.value)
        assertThat(refund.amount).isEqualTo(2)
        assertThat(refund.referenceId).isEqualTo(reservation.id)
    }

}
