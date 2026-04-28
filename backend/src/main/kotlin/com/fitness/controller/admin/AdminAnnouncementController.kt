package com.fitness.controller.admin

import com.fitness.dto.AnnouncementDTO
import com.fitness.dto.CreateAnnouncementRequest
import com.fitness.dto.PageDTO
import com.fitness.security.UserPrincipal
import com.fitness.service.AnnouncementService
import com.fitness.util.pageRequest
import jakarta.validation.Valid
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/announcements")
@PreAuthorize("hasRole('ADMIN')")
class AdminAnnouncementController(
    private val announcementService: AnnouncementService
) {
    @PostMapping
    fun createAnnouncement(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateAnnouncementRequest
    ): ResponseEntity<Any> {
        return try {
            val announcement = announcementService.createAnnouncement(principal.userId, request)
            ResponseEntity.status(HttpStatus.CREATED).body(announcement)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message))
        }
    }

    @GetMapping
    fun getAnnouncements(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageDTO<AnnouncementDTO>> {
        val announcements = announcementService.getAnnouncementsPage(
            principal.userId,
            pageRequest(page, size, Sort.by("createdAt").descending())
        )
        return ResponseEntity.ok(announcements)
    }
}
