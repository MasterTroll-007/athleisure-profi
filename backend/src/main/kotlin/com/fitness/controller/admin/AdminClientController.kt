package com.fitness.controller.admin

import com.fitness.dto.*
import com.fitness.entity.ClientNote
import com.fitness.mapper.ClientNoteMapper
import com.fitness.mapper.UserMapper
import com.fitness.repository.ClientNoteRepository
import com.fitness.repository.UserRepository
import com.fitness.security.UserPrincipal
import com.fitness.service.ReservationService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/admin/clients")
@PreAuthorize("hasRole('ADMIN')")
class AdminClientController(
    private val userRepository: UserRepository,
    private val clientNoteRepository: ClientNoteRepository,
    private val reservationService: ReservationService,
    private val userMapper: UserMapper,
    private val clientNoteMapper: ClientNoteMapper
) {
    @GetMapping
    fun getClients(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageDTO<UserDTO>> {
        val adminId = UUID.fromString(principal.userId)
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        // Only show clients assigned to this admin (trainer)
        val clientsPage = userRepository.findByTrainerId(adminId, pageable)

        val admin = userRepository.findById(adminId).orElse(null)
        val trainerName = userMapper.formatTrainerName(admin)

        val pageDTO = PageDTO(
            content = clientsPage.content.map { user ->
                userMapper.toDTOWithTrainerName(user, trainerName)
            },
            totalElements = clientsPage.totalElements,
            totalPages = clientsPage.totalPages,
            page = clientsPage.number,
            size = clientsPage.size,
            hasNext = clientsPage.hasNext(),
            hasPrevious = clientsPage.hasPrevious()
        )
        return ResponseEntity.ok(pageDTO)
    }

    @GetMapping("/search")
    fun searchClients(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam q: String
    ): ResponseEntity<List<UserDTO>> {
        // Security: Limit query length to prevent abuse
        val maxQueryLength = 100
        if (q.length > maxQueryLength) {
            throw IllegalArgumentException("Search query too long (max $maxQueryLength characters)")
        }
        
        // Security: Sanitize SQL LIKE wildcard characters to prevent SQL injection
        // These characters have special meaning in LIKE clauses
        val sanitizedQuery = sanitizeSearchQuery(q)
        
        val adminId = UUID.fromString(principal.userId)
        val admin = userRepository.findById(adminId).orElse(null)
        val trainerName = userMapper.formatTrainerName(admin)

        val clients = userRepository.searchClientsByTrainer(sanitizedQuery, adminId).map { user ->
            userMapper.toDTOWithTrainerName(user, trainerName)
        }
        return ResponseEntity.ok(clients)
    }
    
    /**
     * Sanitize search query by escaping SQL LIKE wildcard characters.
     * Prevents attackers from using % or _ to manipulate search patterns.
     */
    private fun sanitizeSearchQuery(query: String): String {
        return query
            .replace("\\", "\\\\")  // Escape backslash first
            .replace("%", "\\%")    // Escape percent sign
            .replace("_", "\\_")    // Escape underscore
    }

    @GetMapping("/{id}")
    fun getClient(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<UserDTO> {
        val adminId = UUID.fromString(principal.userId)
        val user = userRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Client not found") }

        // Verify client belongs to this admin
        if (user.trainerId != adminId) {
            throw AccessDeniedException("Access denied")
        }

        val admin = userRepository.findById(adminId).orElse(null)
        val trainerName = userMapper.formatTrainerName(admin)

        return ResponseEntity.ok(userMapper.toDTOWithTrainerName(user, trainerName))
    }

    @GetMapping("/{id}/reservations")
    fun getClientReservations(@PathVariable id: String): ResponseEntity<List<ReservationDTO>> {
        val reservations = reservationService.getUserReservations(id)
        return ResponseEntity.ok(reservations)
    }

    @GetMapping("/{id}/notes")
    fun getClientNotes(@PathVariable id: String): ResponseEntity<List<ClientNoteDTO>> {
        val clientId = UUID.fromString(id)
        val notes = clientNoteRepository.findByClientIdOrderByCreatedAtDesc(clientId)
        return ResponseEntity.ok(clientNoteMapper.toDTOBatch(notes))
    }

    @PostMapping("/{id}/notes")
    fun createClientNote(
        @PathVariable id: String,
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateClientNoteRequest
    ): ResponseEntity<ClientNoteDTO> {
        val clientId = UUID.fromString(id)
        val adminId = UUID.fromString(principal.userId)
        val note = ClientNote(
            clientId = clientId,
            adminId = adminId,
            content = request.content
        )
        val saved = clientNoteRepository.save(note)
        val admin = userRepository.findById(adminId).orElse(null)
        val adminName = clientNoteMapper.formatAdminName(admin)
        return ResponseEntity.status(HttpStatus.CREATED).body(clientNoteMapper.toDTO(saved, adminName))
    }

    @PostMapping("/{id}/assign-trainer")
    fun assignTrainer(
        @PathVariable id: String,
        @Valid @RequestBody request: AssignTrainerRequest
    ): ResponseEntity<UserDTO> {
        val client = userRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Client not found") }

        val trainerId = if (request.trainerId.isBlank()) null else UUID.fromString(request.trainerId)

        // Verify trainer exists and is admin
        if (trainerId != null) {
            val trainer = userRepository.findById(trainerId)
                .orElseThrow { NoSuchElementException("Trainer not found") }
            if (trainer.role != "admin") {
                throw IllegalArgumentException("Selected user is not a trainer")
            }
        }

        val updated = client.copy(trainerId = trainerId)
        val saved = userRepository.save(updated)

        val trainerName = saved.trainerId?.let { tid ->
            userRepository.findById(tid).orElse(null)?.let { t ->
                userMapper.formatTrainerName(t)
            }
        }

        return ResponseEntity.ok(userMapper.toDTOWithTrainerName(saved, trainerName))
    }
}

// Note: The /notes/{id} DELETE endpoint is kept at the parent level
// since it doesn't have /clients prefix in the original API
@RestController
@RequestMapping("/api/admin/notes")
@PreAuthorize("hasRole('ADMIN')")
class AdminNoteController(
    private val clientNoteRepository: ClientNoteRepository
) {
    @DeleteMapping("/{id}")
    fun deleteClientNote(@PathVariable id: String): ResponseEntity<Map<String, String>> {
        val noteId = UUID.fromString(id)
        if (!clientNoteRepository.existsById(noteId)) {
            throw NoSuchElementException("Note not found")
        }
        clientNoteRepository.deleteById(noteId)
        return ResponseEntity.ok(mapOf("message" to "Note deleted"))
    }
}
