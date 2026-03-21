package com.fitness.controller.admin

import com.fitness.dto.CreateWorkoutLogRequest
import com.fitness.dto.WorkoutLogDTO
import com.fitness.security.UserPrincipal
import com.fitness.service.WorkoutLogService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/reservations")
@PreAuthorize("hasRole('ADMIN')")
class AdminWorkoutController(
    private val workoutLogService: WorkoutLogService
) {
    @PostMapping("/{id}/workout")
    fun createWorkoutLog(
        @PathVariable id: String,
        @Valid @RequestBody request: CreateWorkoutLogRequest
    ): ResponseEntity<Any> {
        return try {
            val log = workoutLogService.createOrUpdateWorkoutLog(id, request)
            ResponseEntity.status(HttpStatus.CREATED).body(log)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/{id}/workout")
    fun getWorkoutLog(@PathVariable id: String): ResponseEntity<WorkoutLogDTO?> {
        val log = workoutLogService.getWorkoutLog(id)
        return ResponseEntity.ok(log)
    }

    @PutMapping("/{id}/workout")
    fun updateWorkoutLog(
        @PathVariable id: String,
        @Valid @RequestBody request: CreateWorkoutLogRequest
    ): ResponseEntity<Any> {
        return try {
            val log = workoutLogService.createOrUpdateWorkoutLog(id, request)
            ResponseEntity.ok(log)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        }
    }
}
