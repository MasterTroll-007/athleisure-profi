package com.fitness.dto

import jakarta.validation.constraints.*

data class AvailabilityBlockDTO(
    val id: String,
    val name: String?,
    val daysOfWeek: List<Int>,
    val dayOfWeek: String?,
    val specificDate: String?,
    val startTime: String,
    val endTime: String,
    val slotDurationMinutes: Int?,
    val isRecurring: Boolean?,
    val isBlocked: Boolean?,
    val isActive: Boolean?,
    val createdAt: String
)

data class CreateAvailabilityBlockRequest(
    @field:Size(max = 100, message = "Name too long")
    val name: String? = null,

    @field:Size(min = 1, max = 7, message = "Days of week must have 1-7 elements")
    val daysOfWeek: List<Int> = listOf(1),

    val dayOfWeek: String? = null,

    @field:Pattern(regexp = "^(\\d{4}-\\d{2}-\\d{2})?\$", message = "Date must be in YYYY-MM-DD format")
    val specificDate: String? = null,

    @field:NotBlank(message = "Start time is required")
    @field:Pattern(regexp = "^\\d{2}:\\d{2}\$", message = "Start time must be in HH:mm format")
    val startTime: String,

    @field:NotBlank(message = "End time is required")
    @field:Pattern(regexp = "^\\d{2}:\\d{2}\$", message = "End time must be in HH:mm format")
    val endTime: String,

    @field:Min(value = 15, message = "Slot duration must be at least 15 minutes")
    @field:Max(value = 480, message = "Slot duration cannot exceed 480 minutes")
    val slotDurationMinutes: Int = 60,

    val isRecurring: Boolean = true,
    val isBlocked: Boolean = false
)

data class UpdateAvailabilityBlockRequest(
    val name: String? = null,
    val daysOfWeek: List<Int>? = null,
    val dayOfWeek: String? = null,
    val specificDate: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val slotDurationMinutes: Int? = null,
    val isRecurring: Boolean? = null,
    val isBlocked: Boolean? = null,
    val isActive: Boolean? = null
)

// Admin calendar slot with reservation info
data class AdminCalendarSlotDTO(
    val id: String,  // unique ID for this slot (blockId-date-time)
    val blockId: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val status: String,  // "available", "reserved", "blocked", "past"
    val reservation: SlotReservationDTO? = null
)

data class SlotReservationDTO(
    val id: String,
    val userName: String?,
    val userEmail: String?,
    val status: String,
    val note: String? = null
)

data class BlockSlotRequest(
    @field:NotBlank(message = "Date is required")
    @field:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}\$", message = "Date must be in YYYY-MM-DD format")
    val date: String,

    @field:NotBlank(message = "Start time is required")
    @field:Pattern(regexp = "^\\d{2}:\\d{2}\$", message = "Start time must be in HH:mm format")
    val startTime: String,

    @field:NotBlank(message = "End time is required")
    @field:Pattern(regexp = "^\\d{2}:\\d{2}\$", message = "End time must be in HH:mm format")
    val endTime: String,

    val blockId: String? = null,
    val isBlocked: Boolean = true
)
