package com.fitness.dto

import jakarta.validation.constraints.*

data class ExerciseDTO(
    @field:Size(max = 200, message = "Exercise name too long")
    val name: String,
    @field:Min(0) @field:Max(9999)
    val sets: Int? = null,
    @field:Min(0) @field:Max(9999)
    val reps: Int? = null,
    @field:DecimalMin("0.0") @field:DecimalMax("9999.0")
    val weight: Double? = null,
    @field:Size(max = 100)
    val duration: String? = null,
    @field:Size(max = 1000)
    val notes: String? = null
)

data class CreateWorkoutLogRequest(
    val exercises: List<ExerciseDTO> = emptyList(),

    @field:Size(max = 5000, message = "Notes too long")
    val notes: String? = null
)

data class WorkoutLogDTO(
    val id: String,
    val reservationId: String,
    val exercises: List<ExerciseDTO>,
    val notes: String?,
    val date: String?,
    val createdAt: String,
    val updatedAt: String
)
