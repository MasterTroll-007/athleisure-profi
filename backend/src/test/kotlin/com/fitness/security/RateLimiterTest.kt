package com.fitness.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RateLimiterTest {

    private lateinit var rateLimiter: RateLimiter

    @BeforeEach
    fun setup() {
        rateLimiter = RateLimiter()
    }

    @Test
    fun `new key is not blocked`() {
        assertFalse(rateLimiter.isBlocked("test-key"))
    }

    @Test
    fun `key is not blocked after few attempts`() {
        val key = "test-key"

        repeat(5) {
            rateLimiter.recordAttempt(key)
        }

        assertFalse(rateLimiter.isBlocked(key))
    }

    @Test
    fun `key is blocked after max attempts`() {
        val key = "test-key"

        // Record 10 attempts (MAX_ATTEMPTS)
        repeat(10) {
            rateLimiter.recordAttempt(key)
        }

        assertTrue(rateLimiter.isBlocked(key))
    }

    @Test
    fun `clearAttempts removes block`() {
        val key = "test-key"

        // Block the key
        repeat(10) {
            rateLimiter.recordAttempt(key)
        }
        assertTrue(rateLimiter.isBlocked(key))

        // Clear attempts
        rateLimiter.clearAttempts(key)

        // Should no longer be blocked
        assertFalse(rateLimiter.isBlocked(key))
    }

    @Test
    fun `different keys are independent`() {
        val key1 = "key1"
        val key2 = "key2"

        // Block key1
        repeat(10) {
            rateLimiter.recordAttempt(key1)
        }

        // key1 should be blocked
        assertTrue(rateLimiter.isBlocked(key1))

        // key2 should not be blocked
        assertFalse(rateLimiter.isBlocked(key2))
    }

    @Test
    fun `getRemainingBlockTime returns 0 for non-blocked key`() {
        assertEquals(0, rateLimiter.getRemainingBlockTime("non-existent"))
    }

    @Test
    fun `getRemainingBlockTime returns positive value for blocked key`() {
        val key = "test-key"

        repeat(10) {
            rateLimiter.recordAttempt(key)
        }

        assertTrue(rateLimiter.getRemainingBlockTime(key) > 0)
    }
}
