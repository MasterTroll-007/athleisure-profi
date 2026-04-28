package com.fitness.controller

import com.fitness.dto.TrainingLocationDTO
import com.fitness.mapper.TrainingLocationMapper
import com.fitness.repository.TrainingLocationRepository
import com.fitness.repository.UserRepository
import com.fitness.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/locations")
class TrainingLocationController(
    private val locationRepository: TrainingLocationRepository,
    private val locationMapper: TrainingLocationMapper,
    private val userRepository: UserRepository
) {
    @GetMapping
    fun getActiveLocations(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<TrainingLocationDTO>> {
        val userId = UUID.fromString(principal.userId)
        val trainerId = if (principal.role == "admin") {
            userId
        } else {
            userRepository.findById(userId).orElse(null)?.trainerId
                ?: return ResponseEntity.ok(emptyList())
        }
        val locations = locationRepository.findByAdminIdAndIsActiveTrue(trainerId)
        return ResponseEntity.ok(locationMapper.toDTOBatch(locations))
    }
}
