package com.fitness.dto

import jakarta.validation.constraints.*

data class RegisterRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    @field:Size(max = 255, message = "Email too long")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}\$",
        message = "Password must contain uppercase, lowercase, number and special character"
    )
    val password: String,

    @field:Size(max = 100, message = "First name too long")
    val firstName: String? = null,

    @field:Size(max = 100, message = "Last name too long")
    val lastName: String? = null,

    @field:Pattern(regexp = "^[+]?[0-9\\s-]{0,20}\$", message = "Invalid phone format")
    val phone: String? = null
)

data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String,

    val rememberMe: Boolean = false
)

data class RefreshRequest(
    @field:NotBlank(message = "Refresh token is required")
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
    @field:Size(max = 100, message = "First name too long")
    val firstName: String? = null,

    @field:Size(max = 100, message = "Last name too long")
    val lastName: String? = null,

    @field:Pattern(regexp = "^[+]?[0-9\\s-]{0,20}\$", message = "Invalid phone format")
    val phone: String? = null,

    @field:Pattern(regexp = "^(cs|en)?\$", message = "Locale must be 'cs' or 'en'")
    val locale: String? = null,

    @field:Pattern(regexp = "^(light|dark)?\$", message = "Theme must be 'light' or 'dark'")
    val theme: String? = null
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}\$",
        message = "Password must contain uppercase, lowercase, number and special character"
    )
    val newPassword: String
)

data class RegisterResponse(
    val message: String,
    val email: String
)

data class VerifyEmailRequest(
    @field:NotBlank(message = "Token is required")
    val token: String
)

data class ResendVerificationRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String
)
