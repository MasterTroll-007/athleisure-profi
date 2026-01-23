package com.fitness.controller.admin

import com.fitness.dto.*
import com.fitness.service.TemplateService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/admin/templates")
@PreAuthorize("hasRole('ADMIN')")
class AdminTemplateController(
    private val templateService: TemplateService
) {
    @GetMapping
    fun getTemplates(): ResponseEntity<List<SlotTemplateDTO>> {
        val templates = templateService.getAllTemplates()
        return ResponseEntity.ok(templates)
    }

    @GetMapping("/{id}")
    fun getTemplate(@PathVariable id: String): ResponseEntity<Any> {
        return try {
            val template = templateService.getTemplate(UUID.fromString(id))
            ResponseEntity.ok(template)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        }
    }

    @PostMapping
    fun createTemplate(@Valid @RequestBody request: CreateTemplateRequest): ResponseEntity<SlotTemplateDTO> {
        val template = templateService.createTemplate(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(template)
    }

    @PatchMapping("/{id}")
    fun updateTemplate(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateTemplateRequest
    ): ResponseEntity<Any> {
        return try {
            val template = templateService.updateTemplate(UUID.fromString(id), request)
            ResponseEntity.ok(template)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteTemplate(@PathVariable id: String): ResponseEntity<Any> {
        return try {
            templateService.deleteTemplate(UUID.fromString(id))
            ResponseEntity.ok(mapOf("message" to "Template deleted"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        }
    }
}
