package com.fitness.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Sanity test for [ValidationUtils] — proves the unit test infra (JUnit 4
 * + MockK + plain JVM classpath) compiles and runs. Covers the basic happy
 * + sad paths of each validator.
 */
class ValidationUtilsTest {

    @Test
    fun `validateEmail accepts well-formed email`() {
        assertNull(ValidationUtils.validateEmail("user@example.com"))
    }

    @Test
    fun `validateEmail rejects empty and malformed`() {
        assertNotNull(ValidationUtils.validateEmail(""))
        assertNotNull(ValidationUtils.validateEmail("no-at-sign"))
        assertNotNull(ValidationUtils.validateEmail("double@@at.com"))
        assertNotNull(ValidationUtils.validateEmail(".startsWithDot@example.com"))
    }

    @Test
    fun `validatePassword requires upper lower digit special`() {
        assertNull(ValidationUtils.validatePassword("Strong1!"))
        assertNotNull(ValidationUtils.validatePassword("weak"))
        assertNotNull(ValidationUtils.validatePassword("nouppercase1!"))
        assertNotNull(ValidationUtils.validatePassword("NOLOWERCASE1!"))
        assertNotNull(ValidationUtils.validatePassword("NoDigit!!"))
        assertNotNull(ValidationUtils.validatePassword("NoSpecial1"))
    }

    @Test
    fun `validatePassword rejects common passwords`() {
        assertNotNull(ValidationUtils.validatePassword("password123"))
    }

    @Test
    fun `validateCreditAmount bounds check`() {
        assertNull(ValidationUtils.validateCreditAmount(10))
        assertNull(ValidationUtils.validateCreditAmount(-5))
        assertNotNull(ValidationUtils.validateCreditAmount(0))
        assertNotNull(ValidationUtils.validateCreditAmount(100_000))
        assertNotNull(ValidationUtils.validateCreditAmount(-100_000))
    }

    @Test
    fun `sanitizeText strips control chars and trims`() {
        assertEquals("hello world", ValidationUtils.sanitizeText("  hello\u0007 world  "))
        assertNull(ValidationUtils.sanitizeText(""))
    }
}
