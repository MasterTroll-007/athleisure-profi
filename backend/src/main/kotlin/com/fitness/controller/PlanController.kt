package com.fitness.controller

import com.fitness.dto.*
import com.fitness.security.UserPrincipal
import com.fitness.service.FileStorageService
import com.fitness.service.PlanService
import com.fitness.util.pageRequest
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.data.domain.Sort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/plans")
class PlanController(
    private val planService: PlanService,
    private val fileStorageService: FileStorageService
) {

    @GetMapping
    fun getPlans(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageDTO<TrainingPlanDTO>> {
        val plans = planService.getPlansPage(pageRequest(page, size, Sort.by("sortOrder").ascending()))
        return ResponseEntity.ok(plans)
    }

    @GetMapping("/{id}")
    fun getPlanDetail(@PathVariable id: String): ResponseEntity<PlanDetailDTO> {
        val plan = planService.getPlanDetail(id)
        return ResponseEntity.ok(plan)
    }

    @GetMapping("/my")
    fun getMyPlans(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageDTO<PurchasedPlanDTO>> {
        val plans = planService.getUserPlansPage(
            principal.userId,
            pageRequest(page, size, Sort.by("createdAt").descending())
        )
        return ResponseEntity.ok(plans)
    }

    @PostMapping("/{id}/purchase")
    fun purchasePlan(
        @PathVariable id: String,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<PurchasePlanResponse> {
        val response = planService.purchasePlan(principal.userId, id)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}/check-purchase")
    fun checkPurchase(
        @PathVariable id: String,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<CheckPurchaseResponse> {
        val response = planService.checkPurchase(principal.userId, id)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}/download")
    fun downloadPlan(
        @PathVariable id: String,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<Resource> {
        // Verify user has purchased the plan
        val check = planService.checkPurchase(principal.userId, id)
        if (!check.purchased) {
            throw IllegalArgumentException("You must purchase this plan before downloading")
        }

        val plan = planService.getPlanDetail(id)
        if (!plan.hasFile) {
            throw NoSuchElementException("This plan does not have a downloadable file")
        }

        val filePath = fileStorageService.getPlanFilePath("plans/$id.pdf")
        val resource = UrlResource(filePath.toUri())

        if (!resource.exists()) {
            throw NoSuchElementException("File not found")
        }

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${plan.name.replace(Regex("[^a-zA-Z0-9 _-]"), "_")}.pdf\"")
            .body(resource)
    }
}
