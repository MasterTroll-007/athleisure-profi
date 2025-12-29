package com.fitness.app.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdminStatsDTO(
    val totalClients: Int,
    val todayReservations: Int,
    val weekReservations: Int,
    val todayList: List<TodayReservationDTO>? = null
) {
    val weeklyReservations: Int get() = weekReservations
    val weeklyRevenue: Int get() = 0
}

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
    val emailVerified: Boolean = true,
    val createdAt: String
) {
    val creditBalance: Int get() = credits
    val fullName: String get() = listOfNotNull(firstName, lastName).joinToString(" ").ifEmpty { email }
}

@Serializable
data class ClientsPageDTO(
    val content: List<ClientDTO>,
    val totalElements: Int,
    val totalPages: Int,
    val page: Int,
    val size: Int,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
) {
    val number: Int get() = page
}

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
data class PaymentDTO(
    val id: String,
    val userId: String? = null,
    val userName: String? = null,
    val gopayId: Long? = null,
    val stripeSessionId: String? = null,
    val amount: Double,
    val currency: String,
    val state: String,
    val creditPackageId: String? = null,
    val creditPackageName: String? = null,
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class UnlockWeekRequest(
    val weekStartDate: String
)

// Client Notes DTOs
@Serializable
data class ClientNoteDTO(
    val id: String,
    val clientId: String,
    val adminId: String,
    val adminName: String? = null,
    val content: String,
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class CreateClientNoteRequest(
    val content: String
)
