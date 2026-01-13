package com.fitness.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UserExtensionsTest {

    @Test
    fun `displayName returns full name when both first and last name set`() {
        val user = User(
            email = "test@example.com",
            passwordHash = "hash",
            firstName = "John",
            lastName = "Doe"
        )

        assertEquals("John Doe", user.displayName)
    }

    @Test
    fun `displayName returns first name only when last name is null`() {
        val user = User(
            email = "test@example.com",
            passwordHash = "hash",
            firstName = "John",
            lastName = null
        )

        assertEquals("John", user.displayName)
    }

    @Test
    fun `displayName returns last name only when first name is null`() {
        val user = User(
            email = "test@example.com",
            passwordHash = "hash",
            firstName = null,
            lastName = "Doe"
        )

        assertEquals("Doe", user.displayName)
    }

    @Test
    fun `displayName returns email when both names are null`() {
        val user = User(
            email = "test@example.com",
            passwordHash = "hash",
            firstName = null,
            lastName = null
        )

        assertEquals("test@example.com", user.displayName)
    }

    @Test
    fun `fullName returns full name when both first and last name set`() {
        val user = User(
            email = "test@example.com",
            passwordHash = "hash",
            firstName = "John",
            lastName = "Doe"
        )

        assertEquals("John Doe", user.fullName)
    }

    @Test
    fun `fullName returns first name only when last name is null`() {
        val user = User(
            email = "test@example.com",
            passwordHash = "hash",
            firstName = "John",
            lastName = null
        )

        assertEquals("John", user.fullName)
    }

    @Test
    fun `fullName returns null when both names are null`() {
        val user = User(
            email = "test@example.com",
            passwordHash = "hash",
            firstName = null,
            lastName = null
        )

        assertNull(user.fullName)
    }
}
