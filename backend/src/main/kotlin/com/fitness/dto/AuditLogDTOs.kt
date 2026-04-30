package com.fitness.dto

data class AuditLogDTO(
    val id: String,
    val adminId: String,
    val actorId: String?,
    val actorEmail: String?,
    val actorName: String?,
    val actorRole: String,
    val action: String,
    val targetType: String,
    val targetId: String?,
    val clientId: String?,
    val clientEmail: String?,
    val clientName: String?,
    val reservationId: String?,
    val slotId: String?,
    val date: String?,
    val startTime: String?,
    val endTime: String?,
    val creditsChange: Int?,
    val refundCredits: Boolean?,
    val details: String?,
    val createdAt: String
)
