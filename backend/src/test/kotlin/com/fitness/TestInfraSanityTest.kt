package com.fitness

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Pure-JVM sanity test — proves the test stack (JUnit 5 + MockK) is wired
 * correctly without requiring Docker / Spring context. Runs in milliseconds
 * so it's safe to include in every CI pipeline.
 *
 * The actual integration tests inherit from [IntegrationTestBase] and need
 * Docker for Testcontainers Postgres.
 */
class TestInfraSanityTest {

    interface Greeter {
        fun greet(name: String): String
    }

    @Test
    fun `mockk verifies stub behaviour`() {
        val greeter = mockk<Greeter>()
        every { greeter.greet("Jana") } returns "Ahoj, Jana"

        assertEquals("Ahoj, Jana", greeter.greet("Jana"))
        verify(exactly = 1) { greeter.greet("Jana") }
    }
}
