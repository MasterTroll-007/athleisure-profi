package com.fitness.app.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminStatsDTO(
    val totalClients: Int,
    val todayReservations: Int,
    val weekReservations: Int,
    val todayList: List<TodayReservationDTO>? = null
)

@Serializable
data class TodayReservationDTO(
    val id: String,
    val userName: String? = null,
    val userEmail: String? = null,
    val startTime: String,
    val endTime: String,
    val status: String
)

@Serializable
data class ClientDTO(
    val id: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val credits: Int,
    val role: String,
    val emailVerified: Boolean,
    val createdAt: String
)

@Serializable
data class ClientsPageDTO(
    val content: List<ClientDTO>,
    val totalElements: Int,
    val totalPages: Int,
    val number: Int,
    val size: Int
)

@Serializable
data class AdminCreateReservationRequest(
    val userId: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val blockId: String,
    val deductCredits: Boolean,
    val note: String? = null
)

@Serializable
data class UpdateReservationNoteRequest(
    val note: String?
)

@Serializable
data class GopayPaymentDTO(
    val id: String,
    val userId: String,
    val gopayId: Long? = null,
    val amount: Double,
    val currency: String,
    val state: String,
    val creditPackageId: String? = null,
    val createdAt: String,
    val updatedAt: String? = null
)
