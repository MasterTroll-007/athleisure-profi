package com.fitness.service

import com.fitness.entity.displayName
import com.fitness.repository.*
import com.fitness.repository.RefreshTokenRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class GdprService(
    private val userRepository: UserRepository,
    private val reservationRepository: ReservationRepository,
    private val creditTransactionRepository: CreditTransactionRepository,
    private val feedbackRepository: TrainingFeedbackRepository,
    private val workoutLogRepository: WorkoutLogRepository,
    private val measurementRepository: BodyMeasurementRepository,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    private val logger = LoggerFactory.getLogger(GdprService::class.java)
    private val objectMapper = jacksonObjectMapper().writerWithDefaultPrettyPrinter()

    fun exportUserData(userId: String): String {
        val userUUID = UUID.fromString(userId)
        val user = userRepository.findById(userUUID)
            .orElseThrow { NoSuchElementException("User not found") }

        val reservations = reservationRepository.findByUserId(userUUID)
        val transactions = creditTransactionRepository.findByUserIdOrderByCreatedAtDesc(userUUID)
        val feedback = feedbackRepository.findByUserIdOrderByCreatedAtDesc(userUUID)
        val workoutLogs = workoutLogRepository.findByUserId(userUUID)
        val measurements = measurementRepository.findByUserIdOrderByDateDesc(userUUID)

        val exportData = mapOf(
            "exportDate" to java.time.Instant.now().toString(),
            "profile" to mapOf(
                "id" to user.id.toString(),
                "email" to user.email,
                "firstName" to user.firstName,
                "lastName" to user.lastName,
                "phone" to user.phone,
                "role" to user.role,
                "credits" to user.credits,
                "locale" to user.locale,
                "createdAt" to user.createdAt.toString()
            ),
            "reservations" to reservations.map { r ->
                mapOf(
                    "id" to r.id.toString(),
                    "date" to r.date.toString(),
                    "startTime" to r.startTime.toString(),
                    "endTime" to r.endTime.toString(),
                    "status" to r.status,
                    "creditsUsed" to r.creditsUsed,
                    "createdAt" to r.createdAt.toString()
                )
            },
            "creditTransactions" to transactions.map { t ->
                mapOf(
                    "id" to t.id.toString(),
                    "amount" to t.amount,
                    "type" to t.type,
                    "note" to t.note,
                    "createdAt" to t.createdAt.toString()
                )
            },
            "feedback" to feedback.map { f ->
                mapOf(
                    "id" to f.id.toString(),
                    "rating" to f.rating,
                    "comment" to f.comment,
                    "createdAt" to f.createdAt.toString()
                )
            },
            "workoutLogs" to workoutLogs.map { w ->
                mapOf(
                    "id" to w.id.toString(),
                    "exercises" to w.exercises,
                    "notes" to w.notes,
                    "createdAt" to w.createdAt.toString()
                )
            },
            "bodyMeasurements" to measurements.map { m ->
                mapOf(
                    "id" to m.id.toString(),
                    "date" to m.date.toString(),
                    "weight" to m.weight,
                    "bodyFat" to m.bodyFat,
                    "chest" to m.chest,
                    "waist" to m.waist,
                    "hips" to m.hips,
                    "bicep" to m.bicep,
                    "thigh" to m.thigh,
                    "notes" to m.notes
                )
            }
        )

        return objectMapper.writeValueAsString(exportData)
    }

    @Transactional
    fun deleteAccount(userId: String) {
        val userUUID = UUID.fromString(userId)
        val user = userRepository.findById(userUUID)
            .orElseThrow { NoSuchElementException("User not found") }

        logger.info("GDPR account deletion requested for user ${user.id} (${user.email})")

        // Revoke all active sessions
        refreshTokenRepository.deleteByUserId(userUUID)

        // Anonymize user data - keep records for accounting but remove PII
        val anonymized = user.copy(
            email = "deleted_${user.id}@deleted",
            firstName = null,
            lastName = null,
            phone = null,
            avatarPath = null,
            passwordHash = "DELETED",
            emailVerified = false,
            isBlocked = true
        )
        userRepository.save(anonymized)

        logger.info("GDPR account deletion completed for user ${user.id}")
    }
}
