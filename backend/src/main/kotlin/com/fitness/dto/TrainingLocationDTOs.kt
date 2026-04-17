package com.fitness.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

private const val HEX_COLOR_PATTERN = "^#[0-9A-Fa-f]{6}$"

data class TrainingLocationDTO(
    val id: String,
    val nameCs: String,
    val nameEn: String?,
    val addressCs: String?,
    val addressEn: String?,
    val color: String,
    val isActive: Boolean,
    val createdAt: String
)

data class CreateTrainingLocationRequest(
    @field:NotBlank(message = "Czech name is required")
    @field:Size(max = 100, message = "Name too long")
    val nameCs: String,

    @field:Size(max = 100, message = "Name too long")
    val nameEn: String? = null,

    val addressCs: String? = null,

    val addressEn: String? = null,

    @field:NotBlank(message = "Color is required")
    @field:Pattern(regexp = HEX_COLOR_PATTERN, message = "Color must be a hex string like #RRGGBB")
    val color: String,

    val isActive: Boolean = true
)

data class UpdateTrainingLocationRequest(
    @field:Size(max = 100, message = "Name too long")
    val nameCs: String? = null,

    @field:Size(max = 100, message = "Name too long")
    val nameEn: String? = null,

    val addressCs: String? = null,

    val addressEn: String? = null,

    @field:Pattern(regexp = HEX_COLOR_PATTERN, message = "Color must be a hex string like #RRGGBB")
    val color: String? = null,

    val isActive: Boolean? = null
)
