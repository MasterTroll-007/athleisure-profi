package com.fitness.app.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val rememberMe: Boolean = false
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDTO
)

@Serializable
data class UserDTO(
    val id: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val role: String,
    val credits: Int = 0,
    val locale: String? = null,
    val theme: String? = null,
    val emailVerified: Boolean = false
) {
    val creditBalance: Int get() = credits
    val fullName: String get() = listOfNotNull(firstName, lastName).joinToString(" ").ifEmpty { email }
}

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String
)

@Serializable
data class UpdateProfileRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val locale: String? = null,
    val theme: String? = null
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class VerifyEmailRequest(
    val token: String
)

@Serializable
data class ResendVerificationRequest(
    val email: String
)

@Serializable
data class MessageResponse(
    val message: String
)

@Serializable
data class ErrorResponse(
    val error: String? = null,
    val message: String? = null
)
