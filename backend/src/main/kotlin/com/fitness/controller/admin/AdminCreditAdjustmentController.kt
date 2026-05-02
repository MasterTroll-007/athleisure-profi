package com.fitness.controller.admin

import com.fitness.dto.AdminAdjustCreditsRequest
import com.fitness.dto.CreditBalanceResponse
import com.fitness.security.UserPrincipal
import com.fitness.service.CreditService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/credits")
@PreAuthorize("hasRole('ADMIN')")
class AdminCreditAdjustmentController(
    private val creditService: CreditService
) {
    @PostMapping("/adjust")
    fun adjustCredits(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: AdminAdjustCreditsRequest
    ): ResponseEntity<CreditBalanceResponse> {
        val balance = creditService.adjustCredits(principal.userId, principal.email, request)
        return ResponseEntity.ok(balance)
    }
}
