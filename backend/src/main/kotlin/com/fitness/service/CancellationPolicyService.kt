package com.fitness.service

import com.fitness.dto.CancellationPolicyDTO
import com.fitness.dto.CancellationRefundPreviewDTO
import com.fitness.dto.UpdateCancellationPolicyRequest
import com.fitness.entity.CancellationPolicy
import com.fitness.repository.CancellationPolicyRepository
import com.fitness.repository.ReservationRepository
import com.fitness.repository.SlotRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class CancellationPolicyService(
    private val cancellationPolicyRepository: CancellationPolicyRepository,
    private val reservationRepository: ReservationRepository,
    private val slotRepository: SlotRepository
) {

    fun getPolicyForTrainer(trainerId: UUID): CancellationPolicyDTO? {
        return cancellationPolicyRepository.findByTrainerId(trainerId)?.toDTO()
    }

    fun getOrCreatePolicyForTrainer(trainerId: UUID): CancellationPolicyDTO {
        val existing = cancellationPolicyRepository.findByTrainerId(trainerId)
        if (existing != null) {
            return existing.toDTO()
        }

        val defaultPolicy = CancellationPolicy(
            trainerId = trainerId,
            fullRefundHours = 24,
            partialRefundHours = null,
            partialRefundPercentage = null,
            noRefundHours = 0,
            isActive = true
        )
        return cancellationPolicyRepository.save(defaultPolicy).toDTO()
    }

    @Transactional
    fun updatePolicy(trainerId: UUID, request: UpdateCancellationPolicyRequest): CancellationPolicyDTO {
        val existing = cancellationPolicyRepository.findByTrainerId(trainerId)

        val policy = if (existing != null) {
            existing.copy(
                fullRefundHours = request.fullRefundHours ?: existing.fullRefundHours,
                partialRefundHours = request.partialRefundHours ?: existing.partialRefundHours,
                partialRefundPercentage = request.partialRefundPercentage ?: existing.partialRefundPercentage,
                noRefundHours = request.noRefundHours ?: existing.noRefundHours,
                isActive = request.isActive ?: existing.isActive,
                updatedAt = Instant.now()
            )
        } else {
            CancellationPolicy(
                trainerId = trainerId,
                fullRefundHours = request.fullRefundHours ?: 24,
                partialRefundHours = request.partialRefundHours,
                partialRefundPercentage = request.partialRefundPercentage,
                noRefundHours = request.noRefundHours ?: 0,
                isActive = request.isActive ?: true
            )
        }

        return cancellationPolicyRepository.save(policy).toDTO()
    }

    fun calculateRefund(reservationId: UUID, trainerId: UUID): CancellationRefundPreviewDTO {
        val reservation = reservationRepository.findById(reservationId)
            .orElseThrow { NoSuchElementException("Reservation not found") }

        val policy = cancellationPolicyRepository.findByTrainerId(trainerId)

        val reservationDateTime = LocalDateTime.of(reservation.date, reservation.startTime)
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val hoursUntil = ChronoUnit.MINUTES.between(now, reservationDateTime) / 60.0

        val (refundPercentage, policyApplied) = calculateRefundPercentage(policy, hoursUntil)
        val refundAmount = (reservation.creditsUsed * refundPercentage / 100.0).toInt()

        return CancellationRefundPreviewDTO(
            reservationId = reservationId.toString(),
            creditsUsed = reservation.creditsUsed,
            refundPercentage = refundPercentage,
            refundAmount = refundAmount,
            hoursUntilReservation = hoursUntil,
            policyApplied = policyApplied
        )
    }

    fun calculateRefundPercentage(policy: CancellationPolicy?, hoursUntil: Double): Pair<Int, String> {
        if (policy == null || !policy.isActive) {
            return Pair(100, "NO_POLICY")
        }

        return when {
            hoursUntil >= policy.fullRefundHours -> Pair(100, "FULL_REFUND")
            policy.partialRefundHours != null &&
                policy.partialRefundPercentage != null &&
                hoursUntil >= policy.partialRefundHours ->
                    Pair(policy.partialRefundPercentage, "PARTIAL_REFUND")
            hoursUntil >= policy.noRefundHours -> Pair(0, "NO_REFUND")
            else -> Pair(0, "NO_REFUND")
        }
    }

    fun getTrainerIdForReservation(reservationId: UUID): UUID? {
        val reservation = reservationRepository.findById(reservationId)
            .orElseThrow { NoSuchElementException("Reservation not found") }

        val slotId = reservation.slotId ?: return null
        val slot = slotRepository.findById(slotId).orElse(null) ?: return null

        return slot.adminId
    }

    private fun CancellationPolicy.toDTO() = CancellationPolicyDTO(
        id = this.id.toString(),
        fullRefundHours = this.fullRefundHours,
        partialRefundHours = this.partialRefundHours,
        partialRefundPercentage = this.partialRefundPercentage,
        noRefundHours = this.noRefundHours,
        isActive = this.isActive
    )
}
