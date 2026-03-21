package com.fitness.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateMeasurementRequest(
    @field:NotBlank(message = "Date is required")
    @field:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date must be in YYYY-MM-DD format")
    val date: String,

    val weight: Double? = null,
    val bodyFat: Double? = null,
    val chest: Double? = null,
    val waist: Double? = null,
    val hips: Double? = null,
    val bicep: Double? = null,
    val thigh: Double? = null,

    @field:Size(max = 2000, message = "Notes too long")
    val notes: String? = null
)

data class MeasurementDTO(
    val id: String,
    val userId: String,
    val date: String,
    val weight: Double?,
    val bodyFat: Double?,
    val chest: Double?,
    val waist: Double?,
    val hips: Double?,
    val bicep: Double?,
    val thigh: Double?,
    val notes: String?,
    val createdAt: String
)
