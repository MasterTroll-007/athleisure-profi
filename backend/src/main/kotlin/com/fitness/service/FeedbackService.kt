package com.fitness.service

import com.fitness.dto.*
import com.fitness.entity.TrainingFeedback
import com.fitness.entity.displayName
import com.fitness.repository.ReservationRepository
import com.fitness.repository.TrainingFeedbackRepository
import com.fitness.repository.UserRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class FeedbackService(
    private val feedbackRepository: TrainingFeedbackRepository,
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository
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

    fun getTrainerFeedbackSummary(trainerId: UUID): FeedbackSummaryDTO {
        val avg = feedbackRepository.getAverageRatingForTrainer(trainerId)
        val count = feedbackRepository.countForTrainer(trainerId)
        val distRaw = feedbackRepository.getRatingDistributionForTrainer(trainerId)
        val distribution = distRaw.associate { row ->
            (row[0] as Number).toInt() to (row[1] as Number).toLong()
        }
        return FeedbackSummaryDTO(
            averageRating = avg,
            totalCount = count,
            distribution = distribution
        )
    }

    fun getAllFeedbackForTrainer(trainerId: UUID): List<AdminFeedbackDTO> {
        val feedbackList = feedbackRepository.findAllForTrainer(trainerId)
        if (feedbackList.isEmpty()) return emptyList()

        val userIds = feedbackList.map { it.userId }.distinct()
        val usersMap = userRepository.findAllById(userIds).associateBy { it.id }
        val reservationIds = feedbackList.map { it.reservationId }.distinct()
        val reservationsMap = reservationRepository.findAllById(reservationIds).associateBy { it.id }

        return feedbackList.map { f ->
            val user = usersMap[f.userId]
            val reservation = reservationsMap[f.reservationId]
            AdminFeedbackDTO(
                id = f.id.toString(),
                reservationId = f.reservationId.toString(),
                userId = f.userId.toString(),
                userName = user?.displayName,
                rating = f.rating,
                comment = f.comment,
                date = reservation?.date?.toString(),
                createdAt = f.createdAt.toString()
            )
        }
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
