package com.fitness.dto

data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RefreshRequest(
    val refreshToken: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDTO
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)

data class UserDTO(
    val id: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val phone: String?,
    val role: String,
    val credits: Int,
    val locale: String,
    val theme: String,
    val createdAt: String
)

data class UpdateProfileRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val locale: String? = null,
    val theme: String? = null
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

data class RegisterResponse(
    val message: String,
    val email: String
)

data class VerifyEmailRequest(
    val token: String
)

data class ResendVerificationRequest(
    val email: String
)
