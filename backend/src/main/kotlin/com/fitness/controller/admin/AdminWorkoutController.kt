package com.fitness.controller.admin

import com.fitness.dto.CreateWorkoutLogRequest
import com.fitness.dto.WorkoutLogDTO
import com.fitness.repository.ReservationRepository
import com.fitness.repository.UserRepository
import com.fitness.security.UserPrincipal
import com.fitness.service.WorkoutLogService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/admin/reservations")
@PreAuthorize("hasRole('ADMIN')")
class AdminWorkoutController(
    private val workoutLogService: WorkoutLogService,
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository
) {
    private fun verifyReservationBelongsToAdmin(reservationId: String, adminId: String) {
        val reservation = reservationRepository.findById(UUID.fromString(reservationId))
            .orElseThrow { NoSuchElementException("Reservation not found") }
        val client = userRepository.findById(reservation.userId).orElse(null)
        if (client?.trainerId?.toString() != adminId) {
            throw AccessDeniedException("Access denied")
        }
    }

    @PostMapping("/{id}/workout")
    fun createWorkoutLog(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String,
        @Valid @RequestBody request: CreateWorkoutLogRequest
    ): ResponseEntity<Any> {
        return try {
            verifyReservationBelongsToAdmin(id, principal.userId)
            val log = workoutLogService.createOrUpdateWorkoutLog(id, request)
            ResponseEntity.status(HttpStatus.CREATED).body(log)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        } catch (e: AccessDeniedException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/{id}/workout")
    fun getWorkoutLog(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<Any> {
        return try {
            verifyReservationBelongsToAdmin(id, principal.userId)
            val log = workoutLogService.getWorkoutLog(id)
            ResponseEntity.ok(log)
        } catch (e: AccessDeniedException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to e.message))
        }
    }

    @PutMapping("/{id}/workout")
    fun updateWorkoutLog(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String,
        @Valid @RequestBody request: CreateWorkoutLogRequest
    ): ResponseEntity<Any> {
        return try {
            verifyReservationBelongsToAdmin(id, principal.userId)
            val log = workoutLogService.createOrUpdateWorkoutLog(id, request)
            ResponseEntity.ok(log)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        } catch (e: AccessDeniedException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to e.message))
        }
    }
}
