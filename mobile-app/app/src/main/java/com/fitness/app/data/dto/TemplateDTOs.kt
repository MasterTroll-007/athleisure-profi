package com.fitness.app.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SlotTemplateDTO(
    val id: String,
    val name: String,
    val slots: List<TemplateSlotDTO>,
    val isActive: Boolean,
    val createdAt: String? = null,
    val locationId: String? = null,
    val locationName: String? = null,
    val locationColor: String? = null
)

@Serializable
data class TemplateSlotDTO(
    val id: String? = null,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int
)

@Serializable
data class CreateTemplateRequest(
    val name: String,
    val slots: List<TemplateSlotDTO>,
    val locationId: String? = null
)

@Serializable
data class UpdateTemplateRequest(
    val name: String? = null,
    val slots: List<TemplateSlotDTO>? = null,
    val isActive: Boolean? = null,
    val locationId: String? = null,
    val clearLocation: Boolean? = null
)

@Serializable
data class ApplyTemplateRequest(
    val templateId: String,
    val weekStartDate: String
)

@Serializable
data class ApplyTemplateResponse(
    val createdSlots: Int
)
