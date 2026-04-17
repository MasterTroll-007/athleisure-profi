package com.fitness.controller

import com.fitness.dto.TrainingLocationDTO
import com.fitness.mapper.TrainingLocationMapper
import com.fitness.repository.TrainingLocationRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/locations")
class TrainingLocationController(
    private val locationRepository: TrainingLocationRepository,
    private val locationMapper: TrainingLocationMapper
) {
    @GetMapping
    fun getActiveLocations(): ResponseEntity<List<TrainingLocationDTO>> {
        val locations = locationRepository.findByIsActiveTrue()
        return ResponseEntity.ok(locationMapper.toDTOBatch(locations))
    }
}
