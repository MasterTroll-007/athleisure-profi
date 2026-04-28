package com.fitness.service

import com.fitness.dto.CreateWorkoutLogRequest
import com.fitness.dto.ExerciseDTO
import com.fitness.dto.WorkoutLogDTO
import com.fitness.entity.WorkoutLog
import com.fitness.repository.ReservationRepository
import com.fitness.repository.WorkoutLogRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fitness.dto.PageDTO
import com.fitness.dto.toPageDTO
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class WorkoutLogService(
    private val workoutLogRepository: WorkoutLogRepository,
    private val reservationRepository: ReservationRepository
) {
    private val objectMapper = jacksonObjectMapper()

    @Transactional
    fun createOrUpdateWorkoutLog(reservationId: String, request: CreateWorkoutLogRequest): WorkoutLogDTO {
        val reservationUUID = UUID.fromString(reservationId)
        val reservation = reservationRepository.findById(reservationUUID)
            .orElseThrow { NoSuchElementException("Reservation not found") }

        val exercisesJson = objectMapper.writeValueAsString(request.exercises)

        val existing = workoutLogRepository.findByReservationId(reservationUUID)
        val workoutLog = if (existing != null) {
            existing.updatedAt = Instant.now()
            workoutLogRepository.save(existing.copy(
                exercises = exercisesJson,
                notes = request.notes,
                updatedAt = Instant.now()
            ))
        } else {
            workoutLogRepository.save(WorkoutLog(
                reservationId = reservationUUID,
                exercises = exercisesJson,
                notes = request.notes
            ))
        }

        return toDTO(workoutLog, reservation.date.toString())
    }

    fun getWorkoutLog(reservationId: String): WorkoutLogDTO? {
        val reservationUUID = UUID.fromString(reservationId)
        val log = workoutLogRepository.findByReservationId(reservationUUID) ?: return null
        val reservation = reservationRepository.findById(reservationUUID).orElse(null)
        return toDTO(log, reservation?.date?.toString())
    }

    fun getMyWorkoutLogs(userId: String): List<WorkoutLogDTO> {
        val userUUID = UUID.fromString(userId)
        val logs = workoutLogRepository.findByUserId(userUUID)
        if (logs.isEmpty()) return emptyList()

        val reservationIds = logs.map { it.reservationId }.distinct()
        val reservationsMap = reservationRepository.findAllById(reservationIds).associateBy { it.id }

        return logs.map { log ->
            val reservation = reservationsMap[log.reservationId]
            toDTO(log, reservation?.date?.toString())
        }
    }

    fun getMyWorkoutLogsPage(userId: String, pageable: Pageable): PageDTO<WorkoutLogDTO> {
        val userUUID = UUID.fromString(userId)
        val page = workoutLogRepository.findByUserId(userUUID, pageable)
        val reservationIds = page.content.map { it.reservationId }.distinct()
        val reservationsMap = if (reservationIds.isNotEmpty()) {
            reservationRepository.findAllById(reservationIds).associateBy { it.id }
        } else emptyMap()

        return page.toPageDTO { log ->
            val reservation = reservationsMap[log.reservationId]
            toDTO(log, reservation?.date?.toString())
        }
    }

    private fun toDTO(log: WorkoutLog, date: String?): WorkoutLogDTO {
        val exercises: List<ExerciseDTO> = try {
            log.exercises?.let { objectMapper.readValue(it) } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        return WorkoutLogDTO(
            id = log.id.toString(),
            reservationId = log.reservationId.toString(),
            exercises = exercises,
            notes = log.notes,
            date = date,
            createdAt = log.createdAt.toString(),
            updatedAt = log.updatedAt.toString()
        )
    }
}
