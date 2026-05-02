package com.fitness.service

import com.fitness.entity.CancellationPolicy
import com.fitness.entity.PricingItem
import com.fitness.entity.Reservation
import com.fitness.entity.Slot
import com.fitness.entity.SlotPricingItem
import com.fitness.entity.SlotStatus
import com.fitness.entity.User
import com.fitness.repository.PricingItemRepository
import com.fitness.repository.ReservationRepository
import com.fitness.repository.SlotPricingItemRepository
import com.fitness.repository.SlotRepository
import com.fitness.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.security.access.AccessDeniedException
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class ReservationPolicyServiceTest {
    private val reservationRepository = mockk<ReservationRepository>()
    private val userRepository = mockk<UserRepository>()
    private val slotRepository = mockk<SlotRepository>()
    private val pricingItemRepository = mockk<PricingItemRepository>()
    private val slotPricingItemRepository = mockk<SlotPricingItemRepository>()

    private val service = ReservationPolicyService(
        reservationRepository,
        userRepository,
        slotRepository,
        pricingItemRepository,
        slotPricingItemRepository
    )

    @Test
    fun `reservation ownership is accepted through slot admin or client trainer`() {
        val adminId = UUID.randomUUID()
        val slotId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        every { slotRepository.findById(slotId) } returns Optional.of(slot(id = slotId, adminId = adminId))
        every { userRepository.findById(userId) } returns Optional.of(user(id = userId, trainerId = adminId))

        service.requireReservationOwnedByAdmin(reservation(userId = userId, slotId = slotId), adminId)
        service.requireReservationOwnedByAdmin(reservation(userId = userId, slotId = null), adminId)
    }

    @Test
    fun `reservation ownership rejects foreign slot and foreign client`() {
        val adminId = UUID.randomUUID()
        val otherAdminId = UUID.randomUUID()
        val slotId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        every { slotRepository.findById(slotId) } returns Optional.of(slot(id = slotId, adminId = otherAdminId))
        every { userRepository.findById(userId) } returns Optional.of(user(id = userId, trainerId = otherAdminId))

        assertThatThrownBy {
            service.requireReservationOwnedByAdmin(reservation(userId = userId, slotId = slotId), adminId)
        }.isInstanceOf(AccessDeniedException::class.java)

        assertThatThrownBy {
            service.requireReservationOwnedByAdmin(reservation(userId = userId, slotId = null), adminId)
        }.isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `slot request time must exactly match selected slot`() {
        val date = LocalDate.of(2026, 5, 10)
        val start = LocalTime.of(10, 0)
        val end = LocalTime.of(11, 0)

        service.requireSlotMatchesRequest(date, start, end, date, start, end)

        assertThatThrownBy {
            service.requireSlotMatchesRequest(date, start, end, date, start.plusMinutes(15), end.plusMinutes(15))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("does not match")
    }

    @Test
    fun `credits default to one when slot has no pricing items`() {
        val slotId = UUID.randomUUID()
        every { slotPricingItemRepository.findBySlotId(slotId) } returns emptyList()

        assertThat(service.resolveCreditsNeeded(slotId, null, UUID.randomUUID())).isEqualTo(1 to null)
    }

    @Test
    fun `single slot pricing item is selected automatically`() {
        val trainerId = UUID.randomUUID()
        val slotId = UUID.randomUUID()
        val pricingItemId = UUID.randomUUID()
        every { slotPricingItemRepository.findBySlotId(slotId) } returns listOf(
            SlotPricingItem(slotId = slotId, pricingItemId = pricingItemId)
        )
        every { pricingItemRepository.findById(pricingItemId) } returns Optional.of(
            PricingItem(id = pricingItemId, nameCs = "Training", credits = 3, adminId = trainerId)
        )

        assertThat(service.resolveCreditsNeeded(slotId, null, trainerId)).isEqualTo(3 to pricingItemId)
    }

    @Test
    fun `multiple slot pricing items require explicit selection`() {
        val slotId = UUID.randomUUID()
        every { slotPricingItemRepository.findBySlotId(slotId) } returns listOf(
            SlotPricingItem(slotId = slotId, pricingItemId = UUID.randomUUID()),
            SlotPricingItem(slotId = slotId, pricingItemId = UUID.randomUUID())
        )

        assertThatThrownBy {
            service.resolveCreditsNeeded(slotId, null, UUID.randomUUID())
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("select")
    }

    @Test
    fun `selected pricing item must be active owned by trainer and available on slot`() {
        val trainerId = UUID.randomUUID()
        val slotId = UUID.randomUUID()
        val availablePricingItemId = UUID.randomUUID()
        val requestedPricingItemId = UUID.randomUUID()
        every { slotPricingItemRepository.findBySlotId(slotId) } returns listOf(
            SlotPricingItem(slotId = slotId, pricingItemId = availablePricingItemId)
        )
        every { pricingItemRepository.findById(requestedPricingItemId) } returns Optional.of(
            PricingItem(id = requestedPricingItemId, nameCs = "Foreign", credits = 2, adminId = trainerId)
        )

        assertThatThrownBy {
            service.resolveCreditsNeeded(slotId, requestedPricingItemId.toString(), trainerId)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("not available")

        every { slotPricingItemRepository.findBySlotId(slotId) } returns listOf(
            SlotPricingItem(slotId = slotId, pricingItemId = requestedPricingItemId)
        )
        every { pricingItemRepository.findById(requestedPricingItemId) } returns Optional.of(
            PricingItem(id = requestedPricingItemId, nameCs = "Inactive", credits = 2, adminId = trainerId, isActive = false)
        )
        assertThatThrownBy {
            service.resolveCreditsNeeded(slotId, requestedPricingItemId.toString(), trainerId)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("no longer active")

        every { pricingItemRepository.findById(requestedPricingItemId) } returns Optional.of(
            PricingItem(id = requestedPricingItemId, nameCs = "Other trainer", credits = 2, adminId = UUID.randomUUID())
        )
        assertThatThrownBy {
            service.resolveCreditsNeeded(slotId, requestedPricingItemId.toString(), trainerId)
        }.isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `confirmed reservations for trainer date are loaded only when trainer has slots`() {
        val trainerId = UUID.randomUUID()
        val date = LocalDate.of(2026, 5, 10)
        val slotId = UUID.randomUUID()
        val expected = listOf(reservation(slotId = slotId))
        every { slotRepository.findByDateAndAdminId(date, trainerId) } returns listOf(slot(id = slotId, adminId = trainerId))
        every { reservationRepository.findConfirmedByDateAndSlotIdIn(date, listOf(slotId)) } returns expected

        assertThat(service.findConfirmedReservationsForTrainerDate(date, trainerId)).isEqualTo(expected)

        every { slotRepository.findByDateAndAdminId(date, trainerId) } returns emptyList()
        assertThat(service.findConfirmedReservationsForTrainerDate(date, trainerId)).isEmpty()
        verify(exactly = 1) { reservationRepository.findConfirmedByDateAndSlotIdIn(date, listOf(slotId)) }
    }

    @Test
    fun `adjacent slot rule allows empty same slot previous end and next start only`() {
        val slotId = UUID.randomUUID()
        val existingSlotId = UUID.randomUUID()
        val target = slot(id = slotId, start = LocalTime.of(10, 0), durationMinutes = 60)
        val sameSlot = reservation(slotId = slotId, start = LocalTime.of(10, 0), end = LocalTime.of(11, 0))
        val previous = reservation(slotId = existingSlotId, start = LocalTime.of(9, 0), end = LocalTime.of(10, 0))
        val next = reservation(slotId = existingSlotId, start = LocalTime.of(11, 0), end = LocalTime.of(12, 0))
        val nonAdjacent = reservation(slotId = existingSlotId, start = LocalTime.of(12, 0), end = LocalTime.of(13, 0))

        assertThat(service.isAdjacentSlotAllowed(target, emptyList())).isTrue()
        assertThat(service.isAdjacentSlotAllowed(target, listOf(sameSlot))).isTrue()
        assertThat(service.isAdjacentSlotAllowed(target, listOf(previous))).isTrue()
        assertThat(service.isAdjacentSlotAllowed(target, listOf(next))).isTrue()
        assertThat(service.isAdjacentSlotAllowed(target, listOf(nonAdjacent))).isFalse()
    }

    @Test
    fun `refund percentage follows inactive full partial and no refund windows`() {
        val trainerId = UUID.randomUUID()
        val policy = CancellationPolicy(
            trainerId = trainerId,
            fullRefundHours = 24,
            partialRefundHours = 6,
            partialRefundPercentage = 50,
            noRefundHours = 0
        )

        assertThat(service.calculateRefundPercentage(null, 1.0)).isEqualTo(100 to "NO_POLICY")
        assertThat(service.calculateRefundPercentage(policy.copy(isActive = false), 1.0)).isEqualTo(100 to "NO_POLICY")
        assertThat(service.calculateRefundPercentage(policy, 30.0)).isEqualTo(100 to "FULL_REFUND")
        assertThat(service.calculateRefundPercentage(policy, 6.0)).isEqualTo(50 to "PARTIAL_REFUND")
        assertThat(service.calculateRefundPercentage(policy, 2.0)).isEqualTo(0 to "NO_REFUND")
    }

    private fun slot(
        id: UUID = UUID.randomUUID(),
        adminId: UUID? = UUID.randomUUID(),
        start: LocalTime = LocalTime.of(10, 0),
        durationMinutes: Int = 60
    ) = Slot(
        id = id,
        date = LocalDate.of(2026, 5, 10),
        startTime = start,
        endTime = start.plusMinutes(durationMinutes.toLong()),
        durationMinutes = durationMinutes,
        status = SlotStatus.UNLOCKED,
        adminId = adminId
    )

    private fun reservation(
        userId: UUID = UUID.randomUUID(),
        slotId: UUID? = UUID.randomUUID(),
        start: LocalTime = LocalTime.of(10, 0),
        end: LocalTime = LocalTime.of(11, 0)
    ) = Reservation(
        userId = userId,
        slotId = slotId,
        date = LocalDate.of(2026, 5, 10),
        startTime = start,
        endTime = end
    )

    private fun user(id: UUID, trainerId: UUID?) = User(
        id = id,
        email = "$id@test.com",
        passwordHash = "hash",
        trainerId = trainerId
    )
}
