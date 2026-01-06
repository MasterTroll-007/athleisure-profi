package com.fitness.dto

import jakarta.validation.constraints.*

data class AdminSettingsDTO(
    val calendarStartHour: Int,
    val calendarEndHour: Int,
    val inviteCode: String?,
    val inviteLink: String?
)

data class UpdateAdminSettingsRequest(
    @field:Min(value = 0, message = "Start hour must be 0-23")
    @field:Max(value = 23, message = "Start hour must be 0-23")
    val calendarStartHour: Int? = null,

    @field:Min(value = 0, message = "End hour must be 0-23")
    @field:Max(value = 23, message = "End hour must be 0-23")
    val calendarEndHour: Int? = null
)

data class AssignTrainerRequest(
    @field:NotBlank(message = "Trainer ID is required")
    val trainerId: String
)

data class TrainerDTO(
    val id: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val calendarStartHour: Int,
    val calendarEndHour: Int
)

// Public DTO for registration page - minimal info
data class TrainerInfoDTO(
    val firstName: String?,
    val lastName: String?
)
