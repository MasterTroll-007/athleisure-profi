package com.fitness.dto

import jakarta.validation.constraints.*

data class CreateRecurringReservationRequest(
    @field:NotBlank(message = "Block ID is required")
    val blockId: String,

    @field:Min(value = 1, message = "Day of week must be 1-7")
    @field:Max(value = 7, message = "Day of week must be 1-7")
    val dayOfWeek: Int,

    @field:NotBlank(message = "Start time is required")
    @field:Pattern(regexp = "^\\d{2}:\\d{2}$", message = "Start time must be in HH:mm format")
    val startTime: String,

    @field:NotBlank(message = "End time is required")
    @field:Pattern(regexp = "^\\d{2}:\\d{2}$", message = "End time must be in HH:mm format")
    val endTime: String,

    @field:Min(value = 2, message = "Must be at least 2 weeks")
    @field:Max(value = 12, message = "Cannot exceed 12 weeks")
    val weeksCount: Int,

    val pricingItemId: String? = null
)

data class RecurringReservationDTO(
    val id: String,
    val userId: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val weeksCount: Int,
    val startDate: String,
    val endDate: String,
    val status: String,
    val pricingItemId: String?,
    val reservationIds: List<String>,
    val createdAt: String
)
