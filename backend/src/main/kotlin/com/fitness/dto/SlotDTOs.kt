package com.fitness.dto

import jakarta.validation.constraints.*

// Slot DTOs
data class SlotDTO(
    val id: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int,
    val status: String,  // "locked", "unlocked", "reserved", "blocked", "cancelled"
    val assignedUserId: String? = null,
    val assignedUserName: String? = null,
    val assignedUserEmail: String? = null,
    val note: String? = null,
    val reservationId: String? = null,
    val createdAt: String,
    val cancelledAt: String? = null
)

data class CreateSlotRequest(
    @field:NotBlank(message = "Date is required")
    @field:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}\$", message = "Date must be in YYYY-MM-DD format")
    val date: String,

    @field:NotBlank(message = "Start time is required")
    @field:Pattern(regexp = "^\\d{2}:\\d{2}\$", message = "Start time must be in HH:mm format")
    val startTime: String,

    @field:Min(value = 15, message = "Duration must be at least 15 minutes")
    @field:Max(value = 480, message = "Duration cannot exceed 480 minutes")
    val durationMinutes: Int = 60,

    @field:Size(max = 500, message = "Note too long")
    val note: String? = null,

    val assignedUserId: String? = null
)

data class UpdateSlotRequest(
    @field:Pattern(regexp = "^(locked|unlocked|reserved|blocked)?\$", message = "Invalid status")
    val status: String? = null,

    @field:Size(max = 500, message = "Note too long")
    val note: String? = null,

    val assignedUserId: String? = null,

    @field:Pattern(regexp = "^(\\d{4}-\\d{2}-\\d{2})?\$", message = "Date must be in YYYY-MM-DD format")
    val date: String? = null,

    @field:Pattern(regexp = "^(\\d{2}:\\d{2})?\$", message = "Start time must be in HH:mm format")
    val startTime: String? = null,

    @field:Pattern(regexp = "^(\\d{2}:\\d{2})?\$", message = "End time must be in HH:mm format")
    val endTime: String? = null
)

data class UnlockWeekRequest(
    @field:NotBlank(message = "Week start date is required")
    @field:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}\$", message = "Date must be in YYYY-MM-DD format")
    val weekStartDate: String  // Monday of the week (YYYY-MM-DD)
)

data class ApplyTemplateRequest(
    @field:NotBlank(message = "Template ID is required")
    val templateId: String,

    @field:NotBlank(message = "Week start date is required")
    @field:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}\$", message = "Date must be in YYYY-MM-DD format")
    val weekStartDate: String  // Monday of the week (YYYY-MM-DD)
)

// Template DTOs
data class SlotTemplateDTO(
    val id: String,
    val name: String,
    val slots: List<TemplateSlotDTO>,
    val isActive: Boolean,
    val createdAt: String
)

data class TemplateSlotDTO(
    val id: String? = null,
    val dayOfWeek: Int,  // 1=Monday, 7=Sunday
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int = 60
)

data class CreateTemplateRequest(
    @field:NotBlank(message = "Template name is required")
    @field:Size(max = 100, message = "Name too long")
    val name: String,

    @field:NotEmpty(message = "Template must have at least one slot")
    val slots: List<TemplateSlotDTO>
)

data class UpdateTemplateRequest(
    @field:Size(max = 100, message = "Name too long")
    val name: String? = null,

    val slots: List<TemplateSlotDTO>? = null,

    val isActive: Boolean? = null
)
