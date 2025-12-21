package com.fitness.controller

import com.fitness.dto.*
import com.fitness.security.UserPrincipal
import com.fitness.service.PlanService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/plans")
class PlanController(
    private val planService: PlanService
) {

    @GetMapping
    fun getPlans(): ResponseEntity<List<TrainingPlanDTO>> {
        val plans = planService.getPlans()
        return ResponseEntity.ok(plans)
    }

    @GetMapping("/my")
    fun getMyPlans(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<PurchasedPlanDTO>> {
        val plans = planService.getUserPlans(principal.userId)
        return ResponseEntity.ok(plans)
    }
}
