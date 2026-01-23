package com.fitness.mapper

import com.fitness.dto.ClientNoteDTO
import com.fitness.entity.ClientNote
import com.fitness.entity.User
import com.fitness.repository.UserRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class ClientNoteMapper(
    private val userRepository: UserRepository
) {
    /**
     * Convert ClientNote entity to ClientNoteDTO with pre-fetched admin data.
     */
    fun toDTO(note: ClientNote, adminName: String?): ClientNoteDTO {
        return ClientNoteDTO(
            id = note.id.toString(),
            clientId = note.clientId.toString(),
            adminId = note.adminId.toString(),
            adminName = adminName,
            content = note.content,
            createdAt = note.createdAt.toString(),
            updatedAt = note.updatedAt.toString()
        )
    }

    /**
     * Convert ClientNote entity to ClientNoteDTO, fetching admin from repository.
     */
    fun toDTO(note: ClientNote): ClientNoteDTO {
        val admin = userRepository.findById(note.adminId).orElse(null)
        val adminName = admin?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim().ifEmpty { null } }
        return toDTO(note, adminName)
    }

    /**
     * Batch convert notes to DTOs, efficiently fetching admin names.
     */
    fun toDTOBatch(notes: List<ClientNote>): List<ClientNoteDTO> {
        if (notes.isEmpty()) return emptyList()

        // Batch fetch all admins
        val adminIds = notes.map { it.adminId }.distinct()
        val adminsMap = userRepository.findAllById(adminIds).associateBy { it.id }

        return notes.map { note ->
            val admin = adminsMap[note.adminId]
            val adminName = admin?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim().ifEmpty { null } }
            toDTO(note, adminName)
        }
    }

    /**
     * Format admin name from User entity.
     */
    fun formatAdminName(admin: User?): String? {
        return admin?.let {
            "${it.firstName ?: ""} ${it.lastName ?: ""}".trim().ifEmpty { null }
        }
    }
}
