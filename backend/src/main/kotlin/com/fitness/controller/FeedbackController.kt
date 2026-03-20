package com.fitness.controller

import com.fitness.dto.*
import com.fitness.security.UserPrincipal
import com.fitness.service.FeedbackService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/feedback")
class FeedbackController(
    private val feedbackService: FeedbackService
) {

    @PostMapping
    fun createFeedback(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateFeedbackRequest
    ): ResponseEntity<TrainingFeedbackDTO> {
        val feedback = feedbackService.createFeedback(principal.userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(feedback)
    }

    @GetMapping("/reservation/{reservationId}")
    fun getFeedbackForReservation(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable reservationId: String
    ): ResponseEntity<TrainingFeedbackDTO?> {
        val feedback = feedbackService.getFeedbackForReservation(principal.userId, reservationId)
        return ResponseEntity.ok(feedback)
    }

    @GetMapping("/my")
    fun getMyFeedback(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<List<TrainingFeedbackDTO>> {
        val feedback = feedbackService.getMyFeedback(principal.userId)
        return ResponseEntity.ok(feedback)
    }
}
