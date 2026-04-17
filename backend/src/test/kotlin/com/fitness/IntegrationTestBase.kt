package com.fitness

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Base class for Spring integration tests.
 *
 * - `@SpringBootTest` boots the full application context (with test profile)
 * - `@ActiveProfiles("test")` picks `application-test.yml` which uses the
 *   Testcontainers JDBC URL — Postgres 15 Alpine starts lazily on first
 *   connection and is reused across tests in the same JVM.
 * - `@Transactional` rolls back DB changes after each test method so tests
 *   are isolated without a manual cleanup step.
 *
 * Subclasses should inherit this instead of repeating the annotations. If a
 * specific test needs an overridden profile or a fresh transaction, use
 * `@DirtiesContext` or `Propagation.NOT_SUPPORTED` explicitly in the method.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Testcontainers(disabledWithoutDocker = true)
abstract class IntegrationTestBase
