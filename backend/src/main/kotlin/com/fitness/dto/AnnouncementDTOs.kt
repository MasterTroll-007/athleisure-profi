package com.fitness.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateAnnouncementRequest(
    @field:NotBlank(message = "Subject is required")
    @field:Size(max = 200, message = "Subject too long")
    @field:Pattern(regexp = "^[^\\r\\n]*$", message = "Subject must not contain line breaks")
    val subject: String,

    @field:NotBlank(message = "Message is required")
    @field:Size(max = 5000, message = "Message too long")
    val message: String
)

data class AnnouncementDTO(
    val id: String,
    val subject: String,
    val message: String,
    val recipientsCount: Int,
    val createdAt: String
)
