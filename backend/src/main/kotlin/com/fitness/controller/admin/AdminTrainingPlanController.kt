package com.fitness.controller.admin

import com.fitness.dto.*
import com.fitness.entity.TrainingPlan
import com.fitness.mapper.TrainingPlanMapper
import com.fitness.repository.TrainingPlanRepository
import com.fitness.service.FileStorageService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.util.*

@RestController
@RequestMapping("/api/admin/plans")
@PreAuthorize("hasRole('ADMIN')")
class AdminTrainingPlanController(
    private val trainingPlanRepository: TrainingPlanRepository,
    private val fileStorageService: FileStorageService,
    private val trainingPlanMapper: TrainingPlanMapper
) {
    @GetMapping
    fun getPlans(): ResponseEntity<List<AdminTrainingPlanDTO>> {
        val plans = trainingPlanRepository.findAll()
        return ResponseEntity.ok(trainingPlanMapper.toAdminDTOBatch(plans))
    }

    @GetMapping("/{id}")
    fun getPlan(@PathVariable id: String): ResponseEntity<AdminTrainingPlanDTO> {
        val plan = trainingPlanRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Plan not found") }
        return ResponseEntity.ok(trainingPlanMapper.toAdminDTO(plan))
    }

    @PostMapping
    fun createPlan(@Valid @RequestBody request: CreateTrainingPlanRequest): ResponseEntity<AdminTrainingPlanDTO> {
        val plan = TrainingPlan(
            name = request.nameCs,
            nameCs = request.nameCs,
            nameEn = request.nameEn,
            description = request.descriptionCs,
            descriptionCs = request.descriptionCs,
            descriptionEn = request.descriptionEn,
            credits = request.credits,
            price = BigDecimal.ZERO,
            isActive = request.isActive
        )
        val saved = trainingPlanRepository.save(plan)
        return ResponseEntity.status(HttpStatus.CREATED).body(trainingPlanMapper.toAdminDTO(saved))
    }

    @RequestMapping(value = ["/{id}"], method = [RequestMethod.PUT, RequestMethod.PATCH])
    fun updatePlan(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateTrainingPlanRequest
    ): ResponseEntity<AdminTrainingPlanDTO> {
        val existing = trainingPlanRepository.findById(UUID.fromString(id))
            .orElseThrow { NoSuchElementException("Plan not found") }

        val updated = existing.copy(
            name = request.nameCs ?: existing.name,
            nameCs = request.nameCs ?: existing.nameCs,
            nameEn = request.nameEn ?: existing.nameEn,
            description = request.descriptionCs ?: existing.description,
            descriptionCs = request.descriptionCs ?: existing.descriptionCs,
            descriptionEn = request.descriptionEn ?: existing.descriptionEn,
            credits = request.credits ?: existing.credits,
            isActive = request.isActive ?: existing.isActive
        )

        val saved = trainingPlanRepository.save(updated)
        return ResponseEntity.ok(trainingPlanMapper.toAdminDTO(saved))
    }

    @DeleteMapping("/{id}")
    fun deletePlan(@PathVariable id: String): ResponseEntity<Map<String, String>> {
        val uuid = UUID.fromString(id)
        val plan = trainingPlanRepository.findById(uuid)
            .orElseThrow { NoSuchElementException("Plan not found") }

        // Delete associated file if exists
        fileStorageService.deletePlanFile(plan.filePath)

        trainingPlanRepository.deleteById(uuid)
        return ResponseEntity.ok(mapOf("message" to "Plan deleted"))
    }

    @PostMapping("/{id}/upload", consumes = ["multipart/form-data"])
    fun uploadPlanFile(
        @PathVariable id: String,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<AdminTrainingPlanDTO> {
        val uuid = UUID.fromString(id)
        val existing = trainingPlanRepository.findById(uuid)
            .orElseThrow { NoSuchElementException("Plan not found") }

        // Delete old file if exists
        fileStorageService.deletePlanFile(existing.filePath)

        // Store new file (validation happens in FileStorageService via magic bytes)
        val filePath = fileStorageService.storePlanFile(file, uuid)

        // Update plan with file path
        val updated = existing.copy(filePath = filePath)
        val saved = trainingPlanRepository.save(updated)

        return ResponseEntity.ok(trainingPlanMapper.toAdminDTO(saved))
    }

    @DeleteMapping("/{id}/file")
    fun deletePlanFile(@PathVariable id: String): ResponseEntity<AdminTrainingPlanDTO> {
        val uuid = UUID.fromString(id)
        val existing = trainingPlanRepository.findById(uuid)
            .orElseThrow { NoSuchElementException("Plan not found") }

        // Delete file
        fileStorageService.deletePlanFile(existing.filePath)

        // Update plan
        val updated = existing.copy(filePath = null)
        val saved = trainingPlanRepository.save(updated)

        return ResponseEntity.ok(trainingPlanMapper.toAdminDTO(saved))
    }
}
