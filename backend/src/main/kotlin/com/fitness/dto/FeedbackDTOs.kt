package com.fitness.dto

import jakarta.validation.constraints.*

data class TrainingFeedbackDTO(
    val id: String,
    val reservationId: String,
    val userId: String,
    val rating: Int,
    val comment: String?,
    val createdAt: String
)

data class CreateFeedbackRequest(
    @field:NotBlank(message = "Reservation ID is required")
    val reservationId: String,

    @field:Min(value = 1, message = "Rating must be between 1 and 5")
    @field:Max(value = 5, message = "Rating must be between 1 and 5")
    val rating: Int,

    @field:Size(max = 2000, message = "Comment too long")
    val comment: String? = null
)
