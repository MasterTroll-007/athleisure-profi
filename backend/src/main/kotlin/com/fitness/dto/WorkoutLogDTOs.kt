package com.fitness.dto

import jakarta.validation.constraints.Size

data class ExerciseDTO(
    val name: String,
    val sets: Int? = null,
    val reps: Int? = null,
    val weight: Double? = null,
    val duration: String? = null,
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
