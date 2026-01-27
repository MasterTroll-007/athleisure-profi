package com.fitness.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ClientNoteDTO(
    val id: String,
    val clientId: String,
    val adminId: String,
    val adminName: String?,
    val content: String,
    val createdAt: String,
    val updatedAt: String
)

data class CreateClientNoteRequest(
    @field:NotBlank(message = "Content is required")
    @field:Size(max = 10000, message = "Content must not exceed 10000 characters")
    val content: String
)
