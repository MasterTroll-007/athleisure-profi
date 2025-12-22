package com.fitness.controller

import com.fitness.dto.*
import com.fitness.security.UserPrincipal
import com.fitness.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<RegisterResponse> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/verify-email")
    fun verifyEmail(@RequestBody request: VerifyEmailRequest): ResponseEntity<AuthResponse> {
        val response = authService.verifyEmail(request.token)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/resend-verification")
    fun resendVerification(@RequestBody request: ResendVerificationRequest): ResponseEntity<Map<String, String>> {
        authService.resendVerificationEmail(request.email)
        return ResponseEntity.ok(mapOf("message" to "Verification email sent"))
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshRequest): ResponseEntity<TokenResponse> {
        val response = authService.refresh(request.refreshToken)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/logout")
    fun logout(@RequestBody request: RefreshRequest): ResponseEntity<Map<String, String>> {
        authService.logout(request.refreshToken)
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }

    @GetMapping("/me")
    fun getMe(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<UserDTO> {
        val user = authService.getMe(principal.userId)
        return ResponseEntity.ok(user)
    }

    @PatchMapping("/me")
    fun updateProfile(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<UserDTO> {
        val user = authService.updateProfile(principal.userId, request)
        return ResponseEntity.ok(user)
    }

    @PostMapping("/change-password")
    fun changePassword(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<Map<String, String>> {
        authService.changePassword(principal.userId, request)
        return ResponseEntity.ok(mapOf("message" to "Password changed successfully"))
    }
}
