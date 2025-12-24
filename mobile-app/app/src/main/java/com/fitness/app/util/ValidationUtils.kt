package com.fitness.app.util

import android.util.Patterns

/**
 * Utility object for input validation across the app.
 */
object ValidationUtils {

    private const val MIN_PASSWORD_LENGTH = 8
    private const val MAX_PASSWORD_LENGTH = 128
    private const val MAX_EMAIL_LENGTH = 254
    private const val MAX_NAME_LENGTH = 100
    private const val MAX_NOTE_LENGTH = 1000
    private const val MAX_PHONE_LENGTH = 20

    // Common weak passwords to reject
    private val commonPasswords = setOf(
        "password1", "password123", "12345678", "qwerty123",
        "abcd1234", "admin123", "welcome1", "test1234",
        "letmein1", "monkey123", "dragon123", "master123"
    )

    // Email validation regex (stricter than android.util.Patterns)
    private val EMAIL_REGEX = Regex(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    )

    // Phone validation - allows various formats
    private val PHONE_REGEX = Regex(
        "^[+]?[0-9\\s-]{6,20}$"
    )

    /**
     * Validates email format.
     * @return null if valid, error message if invalid
     */
    fun validateEmail(email: String?): String? {
        if (email.isNullOrBlank()) {
            return "Email is required"
        }

        val trimmed = email.trim()

        if (trimmed.length > MAX_EMAIL_LENGTH) {
            return "Email is too long"
        }

        if (!EMAIL_REGEX.matches(trimmed)) {
            return "Invalid email format"
        }

        // Additional checks
        val parts = trimmed.split("@")
        if (parts.size != 2) {
            return "Invalid email format"
        }

        val localPart = parts[0]
        val domain = parts[1]

        if (localPart.isEmpty() || localPart.length > 64) {
            return "Invalid email format"
        }

        if (localPart.startsWith(".") || localPart.endsWith(".") || localPart.contains("..")) {
            return "Invalid email format"
        }

        if (domain.isEmpty() || domain.length > 253) {
            return "Invalid email format"
        }

        if (domain.startsWith(".") || domain.endsWith(".") || domain.contains("..")) {
            return "Invalid email format"
        }

        return null
    }

    /**
     * Validates password strength.
     * @return null if valid, error message if invalid
     */
    fun validatePassword(password: String?): String? {
        if (password.isNullOrEmpty()) {
            return "Password is required"
        }

        if (password.length < MIN_PASSWORD_LENGTH) {
            return "Password must be at least $MIN_PASSWORD_LENGTH characters"
        }

        if (password.length > MAX_PASSWORD_LENGTH) {
            return "Password is too long"
        }

        if (!password.any { it.isUpperCase() }) {
            return "Password must contain an uppercase letter"
        }

        if (!password.any { it.isLowerCase() }) {
            return "Password must contain a lowercase letter"
        }

        if (!password.any { it.isDigit() }) {
            return "Password must contain a number"
        }

        if (!password.any { !it.isLetterOrDigit() }) {
            return "Password must contain a special character"
        }

        // Check for common passwords
        if (commonPasswords.any { it.equals(password, ignoreCase = true) }) {
            return "Password is too common, please choose a stronger one"
        }

        // Check for repeating characters (3+ in a row)
        if (password.windowed(3).any { window ->
                window[0] == window[1] && window[1] == window[2]
            }) {
            return "Password contains too many repeating characters"
        }

        return null
    }

    /**
     * Validates password for login (less strict than registration).
     * @return null if valid, error message if invalid
     */
    fun validateLoginPassword(password: String?): String? {
        if (password.isNullOrEmpty()) {
            return "Password is required"
        }
        if (password.length > MAX_PASSWORD_LENGTH) {
            return "Password is too long"
        }
        return null
    }

    /**
     * Validates name field (first name, last name).
     * @return null if valid, error message if invalid
     */
    fun validateName(name: String?, fieldName: String = "Name"): String? {
        if (name.isNullOrBlank()) {
            return null // Names are optional
        }

        val trimmed = name.trim()

        if (trimmed.length > MAX_NAME_LENGTH) {
            return "$fieldName is too long"
        }

        // Only allow letters, spaces, hyphens, apostrophes
        if (!trimmed.matches(Regex("^[\\p{L}\\s'-]+$"))) {
            return "$fieldName contains invalid characters"
        }

        return null
    }

    /**
     * Validates phone number format.
     * @return null if valid, error message if invalid
     */
    fun validatePhone(phone: String?): String? {
        if (phone.isNullOrBlank()) {
            return null // Phone is optional
        }

        val trimmed = phone.trim()

        if (trimmed.length > MAX_PHONE_LENGTH) {
            return "Phone number is too long"
        }

        if (!PHONE_REGEX.matches(trimmed)) {
            return "Invalid phone number format"
        }

        return null
    }

    /**
     * Sanitizes text input by removing control characters.
     * @return sanitized string or null if input is null/blank
     */
    fun sanitizeText(text: String?, maxLength: Int = MAX_NOTE_LENGTH): String? {
        if (text.isNullOrBlank()) return null

        val trimmed = text.trim()
        val limited = if (trimmed.length > maxLength) {
            trimmed.take(maxLength)
        } else {
            trimmed
        }

        // Remove control characters except newline, carriage return, tab
        return limited.replace(Regex("[\\p{Cntrl}&&[^\r\n\t]]"), "")
    }

    /**
     * Validates credit adjustment amount.
     * @return null if valid, error message if invalid
     */
    fun validateCreditAmount(amount: Int): String? {
        if (amount == 0) {
            return "Amount cannot be zero"
        }
        if (amount < -10000 || amount > 10000) {
            return "Amount is out of valid range"
        }
        return null
    }

    /**
     * Validates note/reason text.
     * @return null if valid, error message if invalid
     */
    fun validateNote(note: String?, required: Boolean = false): String? {
        if (note.isNullOrBlank()) {
            return if (required) "This field is required" else null
        }

        if (note.trim().length > MAX_NOTE_LENGTH) {
            return "Text is too long (max $MAX_NOTE_LENGTH characters)"
        }

        return null
    }

    /**
     * Checks if passwords match.
     * @return null if they match, error message if they don't
     */
    fun validatePasswordsMatch(password: String, confirmPassword: String): String? {
        if (password != confirmPassword) {
            return "Passwords do not match"
        }
        return null
    }

    /**
     * Quick check if email is valid (for real-time validation).
     */
    fun isValidEmail(email: String): Boolean = validateEmail(email) == null

    /**
     * Quick check if password is valid (for real-time validation).
     */
    fun isValidPassword(password: String): Boolean = validatePassword(password) == null
}
