package com.fitness.service.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.fitness.dto.ChangePasswordRequest
import com.fitness.dto.UpdateProfileRequest
import com.fitness.dto.UserDTO
import com.fitness.mapper.UserMapper
import com.fitness.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class ProfileService(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper
) {
    fun getMe(userId: String): UserDTO {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { NoSuchElementException("User not found") }
        return userMapper.toDTO(user)
    }

    @Transactional
    fun updateProfile(userId: String, request: UpdateProfileRequest): UserDTO {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { NoSuchElementException("User not found") }

        val updated = user.copy(
            firstName = request.firstName ?: user.firstName,
            lastName = request.lastName ?: user.lastName,
            phone = request.phone ?: user.phone,
            locale = request.locale ?: user.locale,
            theme = request.theme ?: user.theme,
            updatedAt = Instant.now()
        )

        return userMapper.toDTO(userRepository.save(updated))
    }

    @Transactional
    fun changePassword(userId: String, request: ChangePasswordRequest) {
        val user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow { NoSuchElementException("User not found") }

        val result = BCrypt.verifyer().verify(request.currentPassword.toCharArray(), user.passwordHash)
        if (!result.verified) {
            throw IllegalArgumentException("Current password is incorrect")
        }

        if (!isValidPassword(request.newPassword)) {
            throw IllegalArgumentException("Password must be at least 8 characters and contain uppercase, lowercase and number")
        }

        val newHash = BCrypt.withDefaults().hashToString(10, request.newPassword.toCharArray())
        val updated = user.copy(passwordHash = newHash, updatedAt = Instant.now())
        userRepository.save(updated)
    }

    private fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        return hasUppercase && hasLowercase && hasDigit
    }
}
