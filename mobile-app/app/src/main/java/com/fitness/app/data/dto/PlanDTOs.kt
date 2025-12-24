package com.fitness.app.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TrainingPlanDTO(
    val id: String,
    val name: String,
    val description: String? = null,
    val credits: Int,
    val price: Double? = null,
    val currency: String? = null,
    val validityDays: Int = 30,
    val sessionsCount: Int? = null,
    val isActive: Boolean = true
) {
    // Alias for UI compatibility
    val creditCost: Int get() = credits
}

@Serializable
data class PurchasedPlanDTO(
    val id: String,
    val planId: String,
    val planName: String,
    val purchasedAt: String,
    val creditsUsed: Int
)

@Serializable
data class SlotTemplateDTO(
    val id: String,
    val name: String,
    val slots: List<TemplateSlotDTO>,
    val isActive: Boolean,
    val createdAt: String? = null
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
    val slots: List<TemplateSlotDTO>
)

@Serializable
data class UpdateTemplateRequest(
    val name: String? = null,
    val slots: List<TemplateSlotDTO>? = null,
    val isActive: Boolean? = null
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
