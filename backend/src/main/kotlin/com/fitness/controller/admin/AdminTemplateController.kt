package com.fitness.controller.admin

import com.fitness.dto.*
import com.fitness.security.UserPrincipal
import com.fitness.service.TemplateService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/admin/templates")
@PreAuthorize("hasRole('ADMIN')")
class AdminTemplateController(
    private val templateService: TemplateService
) {
    @GetMapping
    fun getTemplates(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<SlotTemplateDTO>> {
        val templates = templateService.getAllTemplates(UUID.fromString(principal.userId))
        return ResponseEntity.ok(templates)
    }

    @GetMapping("/{id}")
    fun getTemplate(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<Any> {
        return try {
            val template = templateService.getTemplate(UUID.fromString(id), UUID.fromString(principal.userId))
            ResponseEntity.ok(template)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        }
    }

    @PostMapping
    fun createTemplate(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateTemplateRequest
    ): ResponseEntity<SlotTemplateDTO> {
        val template = templateService.createTemplate(request, UUID.fromString(principal.userId))
        return ResponseEntity.status(HttpStatus.CREATED).body(template)
    }

    @PatchMapping("/{id}")
    fun updateTemplate(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateTemplateRequest
    ): ResponseEntity<Any> {
        return try {
            val template = templateService.updateTemplate(UUID.fromString(id), request, UUID.fromString(principal.userId))
            ResponseEntity.ok(template)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteTemplate(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<Any> {
        return try {
            templateService.deleteTemplate(UUID.fromString(id), UUID.fromString(principal.userId))
            ResponseEntity.ok(mapOf("message" to "Template deleted"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        }
    }
}
