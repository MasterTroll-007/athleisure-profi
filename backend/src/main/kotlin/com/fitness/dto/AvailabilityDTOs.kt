package com.fitness.dto

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
    val name: String? = null,
    val daysOfWeek: List<Int> = listOf(1),
    val dayOfWeek: String? = null,
    val specificDate: String? = null,
    val startTime: String,
    val endTime: String,
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
