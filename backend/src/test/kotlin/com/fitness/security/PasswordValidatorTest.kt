package com.fitness.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PasswordValidatorTest {

    @Test
    fun `accepts passwords meeting all requirements`() {
        assertTrue(PasswordValidator.isValid("Strong123"))
        assertTrue(PasswordValidator.isValid("LongEnough9"))
    }

    @Test
    fun `rejects too short`() {
        assertFalse(PasswordValidator.isValid("Short1"))
        assertFalse(PasswordValidator.isValid(""))
    }

    @Test
    fun `rejects missing upper`() {
        assertFalse(PasswordValidator.isValid("alllowercase1"))
    }

    @Test
    fun `rejects missing lower`() {
        assertFalse(PasswordValidator.isValid("ALLUPPERCASE1"))
    }

    @Test
    fun `rejects missing digit`() {
        assertFalse(PasswordValidator.isValid("NoDigitsHere"))
    }
}
