package com.fitness.service

import com.fitness.dto.UpdateCancellationPolicyRequest
import com.fitness.entity.CancellationPolicy
import com.fitness.entity.Reservation
import com.fitness.entity.Slot
import com.fitness.entity.SlotStatus
import com.fitness.repository.CancellationPolicyRepository
import com.fitness.repository.ReservationRepository
import com.fitness.repository.SlotRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class CancellationPolicyServiceTest {
    private val cancellationPolicyRepository = mockk<CancellationPolicyRepository>()
    private val reservationRepository = mockk<ReservationRepository>()
    private val slotRepository = mockk<SlotRepository>()
    private val service = CancellationPolicyService(cancellationPolicyRepository, reservationRepository, slotRepository)

    @Test
    fun `get or create returns existing policy or saves default policy`() {
        val trainerId = UUID.randomUUID()
        val existing = policy(trainerId, fullRefundHours = 48)
        every { cancellationPolicyRepository.findByTrainerId(trainerId) } returns existing

        assertThat(service.getOrCreatePolicyForTrainer(trainerId).fullRefundHours).isEqualTo(48)

        val newTrainerId = UUID.randomUUID()
        every { cancellationPolicyRepository.findByTrainerId(newTrainerId) } returns null
        every { cancellationPolicyRepository.save(any()) } answers { firstArg() }

        val created = service.getOrCreatePolicyForTrainer(newTrainerId)

        assertThat(created.fullRefundHours).isEqualTo(24)
        assertThat(created.noRefundHours).isEqualTo(0)
        assertThat(created.isActive).isTrue()
    }

    @Test
    fun `update policy preserves omitted fields and can clear partial refund`() {
        val trainerId = UUID.randomUUID()
        val existing = policy(
            trainerId,
            fullRefundHours = 24,
            partialRefundHours = 6,
            partialRefundPercentage = 50,
            noRefundHours = 1
        )
        every { cancellationPolicyRepository.findByTrainerId(trainerId) } returns existing
        every { cancellationPolicyRepository.save(any()) } answers { firstArg() }

        val updated = service.updatePolicy(
            trainerId,
            UpdateCancellationPolicyRequest(fullRefundHours = 36, clearPartialRefund = true, isActive = false)
        )

        assertThat(updated.fullRefundHours).isEqualTo(36)
        assertThat(updated.partialRefundHours).isNull()
        assertThat(updated.partialRefundPercentage).isNull()
        assertThat(updated.noRefundHours).isEqualTo(1)
        assertThat(updated.isActive).isFalse()
    }

    @Test
    fun `refund percentage covers no policy inactive full partial and no refund`() {
        val trainerId = UUID.randomUUID()
        val active = policy(
            trainerId,
            fullRefundHours = 24,
            partialRefundHours = 6,
            partialRefundPercentage = 50,
            noRefundHours = 0
        )

        assertThat(service.calculateRefundPercentage(null, 1.0)).isEqualTo(100 to "NO_POLICY")
        assertThat(service.calculateRefundPercentage(active.copy(isActive = false), 1.0)).isEqualTo(100 to "NO_POLICY")
        assertThat(service.calculateRefundPercentage(active, 24.0)).isEqualTo(100 to "FULL_REFUND")
        assertThat(service.calculateRefundPercentage(active, 6.0)).isEqualTo(50 to "PARTIAL_REFUND")
        assertThat(service.calculateRefundPercentage(active, 1.0)).isEqualTo(0 to "NO_REFUND")
    }

    @Test
    fun `calculate refund preview returns credit amount from policy percentage`() {
        val trainerId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val reservation = Reservation(
            id = reservationId,
            userId = UUID.randomUUID(),
            slotId = UUID.randomUUID(),
            date = LocalDate.now().plusDays(2),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            creditsUsed = 4
        )
        every { reservationRepository.findById(reservationId) } returns Optional.of(reservation)
        every { cancellationPolicyRepository.findByTrainerId(trainerId) } returns policy(
            trainerId,
            fullRefundHours = 24,
            partialRefundHours = 6,
            partialRefundPercentage = 50
        )

        val preview = service.calculateRefund(reservationId, trainerId)

        assertThat(preview.reservationId).isEqualTo(reservationId.toString())
        assertThat(preview.creditsUsed).isEqualTo(4)
        assertThat(preview.refundPercentage).isEqualTo(100)
        assertThat(preview.refundAmount).isEqualTo(4)
        assertThat(preview.policyApplied).isEqualTo("FULL_REFUND")
    }

    @Test
    fun `trainer id for reservation is derived from slot when present`() {
        val reservationId = UUID.randomUUID()
        val slotId = UUID.randomUUID()
        val trainerId = UUID.randomUUID()
        every { reservationRepository.findById(reservationId) } returns Optional.of(
            Reservation(
                id = reservationId,
                userId = UUID.randomUUID(),
                slotId = slotId,
                date = LocalDate.of(2026, 5, 10),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0)
            )
        )
        every { slotRepository.findById(slotId) } returns Optional.of(
            Slot(
                id = slotId,
                date = LocalDate.of(2026, 5, 10),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
                status = SlotStatus.RESERVED,
                adminId = trainerId
            )
        )

        assertThat(service.getTrainerIdForReservation(reservationId)).isEqualTo(trainerId)
    }

    private fun policy(
        trainerId: UUID,
        fullRefundHours: Int = 24,
        partialRefundHours: Int? = null,
        partialRefundPercentage: Int? = null,
        noRefundHours: Int = 0
    ) = CancellationPolicy(
        id = UUID.randomUUID(),
        trainerId = trainerId,
        fullRefundHours = fullRefundHours,
        partialRefundHours = partialRefundHours,
        partialRefundPercentage = partialRefundPercentage,
        noRefundHours = noRefundHours
    )
}
