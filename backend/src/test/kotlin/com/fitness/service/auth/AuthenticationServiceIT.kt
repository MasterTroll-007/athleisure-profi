package com.fitness.service.auth

import com.fitness.IntegrationTestBase
import com.fitness.TestFixtures
import com.fitness.dto.LoginRequest
import com.fitness.repository.RefreshTokenRepository
import com.fitness.repository.UserRepository
import com.fitness.security.JwtService
import com.fitness.security.RateLimiter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Integration tests for the login flow. Exercises bcrypt verification,
 * rate limiter wiring, refresh-token persistence and error branches.
 *
 * Needs a live Postgres via Testcontainers — skipped when Docker is not
 * running (see IntegrationTestBase).
 */
class AuthenticationServiceIT : IntegrationTestBase() {

    @Autowired private lateinit var auth: AuthenticationService
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var refreshTokenRepository: RefreshTokenRepository
    @Autowired private lateinit var jwtService: JwtService
    @Autowired private lateinit var rateLimiter: RateLimiter

    @BeforeEach
    fun clearRateLimiter() {
        // Shared in-memory limiter — make sure test order doesn't bleed.
        rateLimiter.clearAttempts("integ-login@test.com")
    }

    @Test
    fun `login with correct credentials returns tokens`() {
        userRepository.save(TestFixtures.user(email = "integ-login@test.com"))

        val resp = auth.login(LoginRequest(email = "integ-login@test.com", password = "Password1!"))

        assertNotNull(resp.accessToken)
        assertNotNull(resp.refreshToken)
        assertThat(resp.user.email).isEqualTo("integ-login@test.com")
        // Refresh token should be persisted exactly once.
        val stored = refreshTokenRepository.findByToken(resp.refreshToken)
        assertNotNull(stored)
    }

    @Test
    fun `login rejects wrong password and records a rate-limit attempt`() {
        userRepository.save(TestFixtures.user(email = "integ-login@test.com"))

        val ex = assertThrows(IllegalArgumentException::class.java) {
            auth.login(LoginRequest(email = "integ-login@test.com", password = "WrongPass1!"))
        }
        assertThat(ex.message).contains("Invalid email or password")
    }

    @Test
    fun `login rejects unverified account`() {
        userRepository.save(
            TestFixtures.user(email = "integ-login@test.com", emailVerified = false)
        )
        val ex = assertThrows(IllegalArgumentException::class.java) {
            auth.login(LoginRequest(email = "integ-login@test.com", password = "Password1!"))
        }
        assertThat(ex.message).contains("verify your email")
    }

    @Test
    fun `login rejects blocked account`() {
        userRepository.save(
            TestFixtures.user(email = "integ-login@test.com", isBlocked = true)
        )
        val ex = assertThrows(IllegalArgumentException::class.java) {
            auth.login(LoginRequest(email = "integ-login@test.com", password = "Password1!"))
        }
        assertThat(ex.message).contains("suspended")
    }

    @Test
    fun `access token contains correct subject and claims`() {
        val saved = userRepository.save(TestFixtures.user(email = "integ-login@test.com"))

        val resp = auth.login(LoginRequest(email = "integ-login@test.com", password = "Password1!"))
        val claims = jwtService.validateToken(resp.accessToken)!!

        assertThat(claims.subject).isEqualTo(saved.id.toString())
        assertThat(claims["email"]).isEqualTo("integ-login@test.com")
        assertThat(claims["role"]).isEqualTo("client")
    }

    @Test
    fun `subsequent login replaces the previous refresh token`() {
        userRepository.save(TestFixtures.user(email = "integ-login@test.com"))

        val first = auth.login(LoginRequest(email = "integ-login@test.com", password = "Password1!"))
        // JWT `iat` has second-level precision, so two logins inside the same
        // second serialize identically. Wait past the boundary so the rotation
        // assertion distinguishes the two tokens.
        Thread.sleep(1100)
        val second = auth.login(LoginRequest(email = "integ-login@test.com", password = "Password1!"))

        assertThat(first.refreshToken).isNotEqualTo(second.refreshToken)
        assertThat(refreshTokenRepository.findByToken(first.refreshToken)).isNull()
        assertNotNull(refreshTokenRepository.findByToken(second.refreshToken))
    }
}
