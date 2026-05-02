package com.fitness.controller.admin

import com.fitness.dto.CancellationPolicyDTO
import com.fitness.dto.UpdateCancellationPolicyRequest
import com.fitness.security.UserPrincipal
import com.fitness.service.CancellationPolicyService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/settings/cancellation-policy")
@PreAuthorize("hasRole('ADMIN')")
class AdminCancellationPolicyController(
    private val cancellationPolicyService: CancellationPolicyService
) {
    @GetMapping
    fun getCancellationPolicy(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<CancellationPolicyDTO> {
        val policy = cancellationPolicyService.getOrCreatePolicyForTrainer(
            UUID.fromString(principal.userId)
        )
        return ResponseEntity.ok(policy)
    }

    @PatchMapping
    fun updateCancellationPolicy(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: UpdateCancellationPolicyRequest
    ): ResponseEntity<CancellationPolicyDTO> {
        val policy = cancellationPolicyService.updatePolicy(
            UUID.fromString(principal.userId),
            request
        )
        return ResponseEntity.ok(policy)
    }
}
