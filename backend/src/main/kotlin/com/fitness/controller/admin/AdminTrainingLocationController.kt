package com.fitness.controller.admin

import com.fitness.dto.CreateTrainingLocationRequest
import com.fitness.dto.TrainingLocationDTO
import com.fitness.dto.UpdateTrainingLocationRequest
import com.fitness.entity.TrainingLocation
import com.fitness.mapper.TrainingLocationMapper
import com.fitness.repository.TrainingLocationRepository
import com.fitness.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/admin/locations")
@PreAuthorize("hasRole('ADMIN')")
class AdminTrainingLocationController(
    private val locationRepository: TrainingLocationRepository,
    private val locationMapper: TrainingLocationMapper
) {
    @GetMapping
    fun getLocations(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<TrainingLocationDTO>> {
        val adminId = UUID.fromString(principal.userId)
        val locations = locationRepository.findByAdminId(adminId)
        return ResponseEntity.ok(locationMapper.toDTOBatch(locations))
    }

    @GetMapping("/{id}")
    fun getLocation(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<TrainingLocationDTO> {
        val adminId = UUID.fromString(principal.userId)
        val location = locationRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Training location not found") }
        if (location.adminId != adminId) {
            throw AccessDeniedException("Access denied")
        }
        return ResponseEntity.ok(locationMapper.toDTO(location))
    }

    @PostMapping
    fun createLocation(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateTrainingLocationRequest
    ): ResponseEntity<TrainingLocationDTO> {
        val adminId = UUID.fromString(principal.userId)
        val location = TrainingLocation(
            nameCs = request.nameCs,
            nameEn = request.nameEn,
            addressCs = request.addressCs,
            addressEn = request.addressEn,
            color = request.color,
            isActive = request.isActive,
            adminId = adminId
        )
        val saved = locationRepository.save(location)
        return ResponseEntity.status(HttpStatus.CREATED).body(locationMapper.toDTO(saved))
    }

    @RequestMapping(value = ["/{id}"], method = [RequestMethod.PUT, RequestMethod.PATCH])
    fun updateLocation(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateTrainingLocationRequest
    ): ResponseEntity<TrainingLocationDTO> {
        val adminId = UUID.fromString(principal.userId)
        val existing = locationRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Training location not found") }

        if (existing.adminId != adminId) {
            throw AccessDeniedException("Access denied")
        }

        val updated = existing.copy(
            nameCs = request.nameCs ?: existing.nameCs,
            nameEn = request.nameEn ?: existing.nameEn,
            addressCs = request.addressCs ?: existing.addressCs,
            addressEn = request.addressEn ?: existing.addressEn,
            color = request.color ?: existing.color,
            isActive = request.isActive ?: existing.isActive
        )
        val saved = locationRepository.save(updated)
        return ResponseEntity.ok(locationMapper.toDTO(saved))
    }

    @DeleteMapping("/{id}")
    fun deleteLocation(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<Map<String, String>> {
        val adminId = UUID.fromString(principal.userId)
        val uuid = UUID.fromString(id)
        val existing = locationRepository.findById(uuid)
            .orElseThrow { NoSuchElementException("Training location not found") }

        if (existing.adminId != adminId) {
            throw AccessDeniedException("Access denied")
        }

        locationRepository.deleteById(uuid)
        return ResponseEntity.ok(mapOf("message" to "Training location deleted"))
    }
}
