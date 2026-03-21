package com.fitness.dto

import jakarta.validation.constraints.*

data class CreateMeasurementRequest(
    @field:NotBlank(message = "Date is required")
    @field:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date must be in YYYY-MM-DD format")
    val date: String,

    @field:DecimalMin("0.0") @field:DecimalMax("500.0")
    val weight: Double? = null,
    @field:DecimalMin("0.0") @field:DecimalMax("100.0")
    val bodyFat: Double? = null,
    @field:DecimalMin("0.0") @field:DecimalMax("300.0")
    val chest: Double? = null,
    @field:DecimalMin("0.0") @field:DecimalMax("300.0")
    val waist: Double? = null,
    @field:DecimalMin("0.0") @field:DecimalMax("300.0")
    val hips: Double? = null,
    @field:DecimalMin("0.0") @field:DecimalMax("200.0")
    val bicep: Double? = null,
    @field:DecimalMin("0.0") @field:DecimalMax("200.0")
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
