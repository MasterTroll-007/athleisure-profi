package com.fitness.controller.admin

import com.fitness.dto.AdminFeedbackDTO
import com.fitness.dto.FeedbackSummaryDTO
import com.fitness.dto.PageDTO
import com.fitness.security.UserPrincipal
import com.fitness.service.FeedbackService
import com.fitness.util.pageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/feedback")
@PreAuthorize("hasRole('ADMIN')")
class AdminFeedbackController(
    private val feedbackService: FeedbackService
) {
    @GetMapping("/summary")
    fun getFeedbackSummary(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<FeedbackSummaryDTO> {
        val summary = feedbackService.getTrainerFeedbackSummary(UUID.fromString(principal.userId))
        return ResponseEntity.ok(summary)
    }

    @GetMapping
    fun getAllFeedback(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageDTO<AdminFeedbackDTO>> {
        val feedback = feedbackService.getAllFeedbackForTrainerPage(
            UUID.fromString(principal.userId),
            pageRequest(page, size, Sort.by("createdAt").descending())
        )
        return ResponseEntity.ok(feedback)
    }
}
