package com.fitness.service

import com.fitness.dto.CreateFeedbackRequest
import com.fitness.dto.TrainingFeedbackDTO
import com.fitness.entity.TrainingFeedback
import com.fitness.repository.ReservationRepository
import com.fitness.repository.TrainingFeedbackRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class FeedbackService(
    private val feedbackRepository: TrainingFeedbackRepository,
    private val reservationRepository: ReservationRepository
) {

    fun createFeedback(userId: String, request: CreateFeedbackRequest): TrainingFeedbackDTO {
        val userUUID = UUID.fromString(userId)
        val reservationUUID = UUID.fromString(request.reservationId)

        val reservation = reservationRepository.findById(reservationUUID)
            .orElseThrow { NoSuchElementException("Reservation not found") }

        if (reservation.userId != userUUID) {
            throw IllegalArgumentException("Access denied")
        }

        if (feedbackRepository.existsByReservationId(reservationUUID)) {
            throw IllegalArgumentException("Feedback already submitted for this reservation")
        }

        val feedback = feedbackRepository.save(
            TrainingFeedback(
                reservationId = reservationUUID,
                userId = userUUID,
                rating = request.rating,
                comment = request.comment
            )
        )

        return toDTO(feedback)
    }

    fun getFeedbackForReservation(userId: String, reservationId: String): TrainingFeedbackDTO? {
        val reservationUUID = UUID.fromString(reservationId)

        // Verify the user owns this reservation
        val reservation = reservationRepository.findById(reservationUUID).orElse(null)
        if (reservation != null && reservation.userId.toString() != userId) {
            throw IllegalArgumentException("Access denied")
        }

        return feedbackRepository.findByReservationId(reservationUUID)?.let { toDTO(it) }
    }

    fun getMyFeedback(userId: String): List<TrainingFeedbackDTO> {
        return feedbackRepository.findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId))
            .map { toDTO(it) }
    }

    private fun toDTO(feedback: TrainingFeedback) = TrainingFeedbackDTO(
        id = feedback.id.toString(),
        reservationId = feedback.reservationId.toString(),
        userId = feedback.userId.toString(),
        rating = feedback.rating,
        comment = feedback.comment,
        createdAt = feedback.createdAt.toString()
    )
}
