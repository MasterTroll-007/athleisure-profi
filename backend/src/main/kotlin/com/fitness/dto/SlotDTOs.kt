package com.fitness.dto

// Slot DTOs
data class SlotDTO(
    val id: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int,
    val status: String,  // "locked", "unlocked", "reserved", "blocked"
    val assignedUserId: String? = null,
    val assignedUserName: String? = null,
    val assignedUserEmail: String? = null,
    val note: String? = null,
    val reservationId: String? = null,
    val createdAt: String
)

data class CreateSlotRequest(
    val date: String,
    val startTime: String,
    val durationMinutes: Int = 60,
    val note: String? = null,
    val assignedUserId: String? = null
)

data class UpdateSlotRequest(
    val status: String? = null,
    val note: String? = null,
    val assignedUserId: String? = null,
    val date: String? = null,
    val startTime: String? = null,
    val endTime: String? = null
)

data class UnlockWeekRequest(
    val weekStartDate: String  // Monday of the week (YYYY-MM-DD)
)

data class ApplyTemplateRequest(
    val templateId: String,
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
    val name: String,
    val slots: List<TemplateSlotDTO>
)

data class UpdateTemplateRequest(
    val name: String? = null,
    val slots: List<TemplateSlotDTO>? = null,
    val isActive: Boolean? = null
)
