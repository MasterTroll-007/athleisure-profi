package com.fitness

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

/**
 * Smoke test — proves the Spring context starts with the test profile and
 * the Testcontainers Postgres bootstrap works end-to-end. Any bean wiring
 * regression will fail this test first.
 */
class ApplicationContextTest : IntegrationTestBase() {

    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun `context loads`() {
        assertNotNull(context)
        assertNotNull(context.getBean("availabilityService"))
        assertNotNull(context.getBean("reservationService"))
    }
}
