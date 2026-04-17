package com.fitness.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pure-JVM unit test — no Spring, no Docker. Exercises token generation +
 * validation round-trips, wrong-secret rejection, and claim extraction.
 */
class JwtServiceTest {

    private val secret = "this-is-a-very-long-test-secret-that-passes-256-bit-hmac-minimum-xxxxxxxx"
    private val accessExp = 900_000L
    private val refreshExp = 604_800_000L
    private val rememberMeExp = 2_592_000_000L
    private val jwt = JwtService(secret, accessExp, refreshExp, rememberMeExp)

    @Test
    fun `access token round-trips user id email and role`() {
        val token = jwt.generateAccessToken("user-1", "u@e.com", "client")
        val claims = jwt.validateToken(token)
        assertNotNull(claims)
        assertEquals("user-1", claims!!.subject)
        assertEquals("u@e.com", claims["email"])
        assertEquals("client", claims["role"])
        assertEquals("access", claims["type"])
    }

    @Test
    fun `refresh token carries rememberMe flag`() {
        val short = jwt.generateRefreshToken("user-1", rememberMe = false)
        val long = jwt.generateRefreshToken("user-1", rememberMe = true)

        val shortClaims = jwt.validateToken(short)!!
        val longClaims = jwt.validateToken(long)!!

        assertFalse(shortClaims["rememberMe"] as Boolean)
        assertTrue(longClaims["rememberMe"] as Boolean)
    }

    @Test
    fun `tampered token returns null`() {
        val token = jwt.generateAccessToken("user-1", "u@e.com", "client")
        val tampered = token.dropLast(5) + "abcde"
        assertNull(jwt.validateToken(tampered))
    }

    @Test
    fun `token signed with different secret fails validation`() {
        val other = JwtService("entirely-different-secret-also-long-enough-for-hmac-sha256-yyyyyyyyy",
            accessExp, refreshExp, rememberMeExp)
        val token = other.generateAccessToken("user-1", "u@e.com", "client")
        assertNull(jwt.validateToken(token))
    }

    @Test
    fun `getUserIdFromToken extracts subject`() {
        val token = jwt.generateAccessToken("user-xyz", "x@y.com", "admin")
        assertEquals("user-xyz", jwt.getUserIdFromToken(token))
    }
}
