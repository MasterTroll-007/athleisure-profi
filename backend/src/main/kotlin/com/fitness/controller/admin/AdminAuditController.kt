package com.fitness.controller.admin

import com.fitness.dto.AuditLogDTO
import com.fitness.dto.PageDTO
import com.fitness.security.UserPrincipal
import com.fitness.service.AuditService
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
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
class AdminAuditController(
    private val auditService: AuditService
) {
    @GetMapping
    fun getAuditLogs(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) clientId: String?,
        @RequestParam(required = false) action: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PageDTO<AuditLogDTO>> {
        val logs = auditService.getAdminAuditLogs(
            adminId = UUID.fromString(principal.userId),
            clientId = clientId?.let { UUID.fromString(it) },
            action = action,
            pageable = pageRequest(page, size, Sort.by("createdAt").descending())
        )
        return ResponseEntity.ok(logs)
    }
}
