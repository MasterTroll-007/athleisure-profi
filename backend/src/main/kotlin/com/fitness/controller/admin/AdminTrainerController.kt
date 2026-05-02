package com.fitness.controller.admin

import com.fitness.dto.TrainerDTO
import com.fitness.mapper.UserMapper
import com.fitness.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/trainers")
@PreAuthorize("hasRole('ADMIN')")
class AdminTrainerController(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper
) {
    @GetMapping
    fun getTrainers(): ResponseEntity<List<TrainerDTO>> {
        val trainers = userRepository.findByRole("admin").map { trainer ->
            userMapper.toTrainerDTO(trainer)
        }
        return ResponseEntity.ok(trainers)
    }
}
