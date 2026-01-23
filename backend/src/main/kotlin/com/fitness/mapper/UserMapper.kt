package com.fitness.mapper

import com.fitness.dto.TrainerDTO
import com.fitness.dto.TrainerInfoDTO
import com.fitness.dto.UserDTO
import com.fitness.entity.User
import com.fitness.entity.displayName
import com.fitness.repository.UserRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class UserMapper(
    private val userRepository: UserRepository
) {
    /**
     * Convert User entity to UserDTO.
     * Fetches trainer name if trainerId is present.
     */
    fun toDTO(user: User): UserDTO {
        val trainerName = user.trainerId?.let { trainerId ->
            userRepository.findById(trainerId).orElse(null)?.let { formatTrainerName(it) }
        }
        return toDTOWithTrainerName(user, trainerName)
    }

    /**
     * Convert User entity to UserDTO with pre-fetched trainer name.
     * Use this when trainer name is already available to avoid extra DB lookup.
     */
    fun toDTOWithTrainerName(user: User, trainerName: String?): UserDTO {
        return UserDTO(
            id = user.id.toString(),
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            phone = user.phone,
            role = user.role,
            credits = user.credits,
            locale = user.locale,
            theme = user.theme,
            trainerId = user.trainerId?.toString(),
            trainerName = trainerName,
            calendarStartHour = user.calendarStartHour,
            calendarEndHour = user.calendarEndHour,
            createdAt = user.createdAt.toString()
        )
    }

    /**
     * Batch convert users to DTOs, efficiently fetching trainer names.
     * Avoids N+1 by batch-fetching all trainers in a single query.
     */
    fun toDTOBatch(users: List<User>): List<UserDTO> {
        val trainerIds = users.mapNotNull { it.trainerId }.distinct()
        val trainersMap = if (trainerIds.isNotEmpty()) {
            userRepository.findAllById(trainerIds).associateBy { it.id }
        } else {
            emptyMap()
        }

        return users.map { user ->
            val trainerName = user.trainerId?.let { trainersMap[it] }?.let { formatTrainerName(it) }
            toDTOWithTrainerName(user, trainerName)
        }
    }

    /**
     * Convert User entity to TrainerDTO.
     */
    fun toTrainerDTO(user: User): TrainerDTO {
        return TrainerDTO(
            id = user.id.toString(),
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            calendarStartHour = user.calendarStartHour,
            calendarEndHour = user.calendarEndHour
        )
    }

    /**
     * Convert User entity to TrainerInfoDTO (minimal info for registration page).
     */
    fun toTrainerInfoDTO(user: User): TrainerInfoDTO {
        return TrainerInfoDTO(
            firstName = user.firstName,
            lastName = user.lastName
        )
    }

    /**
     * Format trainer name from User entity.
     * Returns "FirstName LastName" or email if name is empty.
     */
    fun formatTrainerName(trainer: User?): String? {
        return trainer?.let {
            listOfNotNull(it.firstName, it.lastName).joinToString(" ").ifEmpty { it.email }
        }
    }

    /**
     * Resolve trainer names for a set of trainer IDs.
     * Returns a map of trainerId -> formatted trainer name.
     */
    fun resolveTrainerNames(trainerIds: Set<UUID>): Map<UUID, String> {
        if (trainerIds.isEmpty()) return emptyMap()

        return userRepository.findAllById(trainerIds)
            .associate { it.id to (formatTrainerName(it) ?: it.email) }
    }
}
