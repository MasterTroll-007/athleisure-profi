package com.fitness.security

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimiter {

    private val attempts = ConcurrentHashMap<String, MutableList<Instant>>()
    private val blockedUntil = ConcurrentHashMap<String, Instant>()

    companion object {
        private const val MAX_ATTEMPTS = 10
        private const val WINDOW_MINUTES = 5L
        private const val BLOCK_MINUTES = 5L
    }

    fun isBlocked(key: String): Boolean {
        val blockTime = blockedUntil[key] ?: return false
        if (Instant.now().isAfter(blockTime)) {
            blockedUntil.remove(key)
            attempts.remove(key)
            return false
        }
        return true
    }

    fun recordAttempt(key: String) {
        val now = Instant.now()
        val cutoff = now.minusSeconds(WINDOW_MINUTES * 60)

        val attemptList = attempts.computeIfAbsent(key) { mutableListOf() }

        // Remove old attempts outside the window
        attemptList.removeIf { it.isBefore(cutoff) }

        // Add new attempt
        attemptList.add(now)

        // Check if should be blocked
        if (attemptList.size >= MAX_ATTEMPTS) {
            blockedUntil[key] = now.plusSeconds(BLOCK_MINUTES * 60)
            attemptList.clear()
        }
    }

    fun clearAttempts(key: String) {
        attempts.remove(key)
        blockedUntil.remove(key)
    }

    fun getRemainingBlockTime(key: String): Long {
        val blockTime = blockedUntil[key] ?: return 0
        val remaining = blockTime.epochSecond - Instant.now().epochSecond
        return if (remaining > 0) remaining else 0
    }
}
