package com.fitness.controller

import com.fitness.dto.*
import com.fitness.security.UserPrincipal
import com.fitness.service.auth.AuthenticationService
import com.fitness.service.auth.ProfileService
import com.fitness.service.auth.RegistrationService
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authenticationService: AuthenticationService,
    private val registrationService: RegistrationService,
    private val profileService: ProfileService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<RegisterResponse> {
        val response = registrationService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/trainer/{code}")
    fun getTrainerByCode(@PathVariable code: String): ResponseEntity<TrainerInfoDTO> {
        val trainer = registrationService.getTrainerByCode(code)
        return ResponseEntity.ok(trainer)
    }

    @PostMapping("/verify-email")
    fun verifyEmail(@Valid @RequestBody request: VerifyEmailRequest): ResponseEntity<AuthResponse> {
        val response = registrationService.verifyEmail(request.token)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/resend-verification")
    fun resendVerification(@Valid @RequestBody request: ResendVerificationRequest): ResponseEntity<Map<String, String>> {
        registrationService.resendVerificationEmail(request.email)
        return ResponseEntity.ok(mapOf("message" to "Verification email sent"))
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpResponse: HttpServletResponse
    ): ResponseEntity<AuthResponse> {
        val response = authenticationService.login(request)

        // Set refresh token in HttpOnly cookie for web clients
        val cookie = Cookie("refreshToken", response.refreshToken)
        cookie.isHttpOnly = true
        cookie.secure = true  // Only sent over HTTPS
        cookie.path = "/api/auth"
        cookie.maxAge = if (request.rememberMe) 30 * 24 * 60 * 60 else 7 * 24 * 60 * 60  // 30 days or 7 days in seconds
        cookie.setAttribute("SameSite", "Strict")
        httpResponse.addCookie(cookie)

        // Return response with refresh token for mobile compatibility
        return ResponseEntity.ok(response)
    }

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody(required = false) request: RefreshRequest?,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse
    ): ResponseEntity<TokenResponse> {
        // Try to get refresh token from cookie first (web), then from body (mobile)
        val refreshToken = httpRequest.cookies?.firstOrNull { it.name == "refreshToken" }?.value
            ?: request?.refreshToken
            ?: throw IllegalArgumentException("Refresh token is required")

        val response = authenticationService.refresh(refreshToken)

        // Update cookie with new refresh token for web clients
        val cookie = Cookie("refreshToken", response.refreshToken)
        cookie.isHttpOnly = true
        cookie.secure = true
        cookie.path = "/api/auth"
        // Preserve maxAge from original cookie if present, otherwise use default 7 days
        val originalCookie = httpRequest.cookies?.firstOrNull { it.name == "refreshToken" }
        cookie.maxAge = originalCookie?.maxAge ?: (7 * 24 * 60 * 60)
        cookie.setAttribute("SameSite", "Strict")
        httpResponse.addCookie(cookie)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/logout")
    fun logout(
        @RequestBody(required = false) request: RefreshRequest?,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse
    ): ResponseEntity<Map<String, String>> {
        // Try to get refresh token from cookie first (web), then from body (mobile)
        val refreshToken = httpRequest.cookies?.firstOrNull { it.name == "refreshToken" }?.value
            ?: request?.refreshToken

        if (refreshToken != null) {
            authenticationService.logout(refreshToken)
        }

        // Clear the refresh token cookie
        val cookie = Cookie("refreshToken", "")
        cookie.isHttpOnly = true
        cookie.secure = true
        cookie.path = "/api/auth"
        cookie.maxAge = 0  // Delete cookie
        cookie.setAttribute("SameSite", "Strict")
        httpResponse.addCookie(cookie)

        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }

    @GetMapping("/me")
    fun getMe(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<UserDTO> {
        val user = profileService.getMe(principal.userId)
        return ResponseEntity.ok(user)
    }

    @PatchMapping("/me")
    fun updateProfile(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<UserDTO> {
        val user = profileService.updateProfile(principal.userId, request)
        return ResponseEntity.ok(user)
    }

    @PostMapping("/change-password")
    fun changePassword(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<Map<String, String>> {
        profileService.changePassword(principal.userId, request)
        return ResponseEntity.ok(mapOf("message" to "Password changed successfully"))
    }
}
