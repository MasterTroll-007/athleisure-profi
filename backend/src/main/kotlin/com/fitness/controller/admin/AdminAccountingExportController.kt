package com.fitness.controller.admin

import com.fitness.security.UserPrincipal
import com.fitness.service.AccountingExportService
import com.fitness.service.StripeAccountingService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

@RestController
@RequestMapping("/api/admin/export/accounting")
@PreAuthorize("hasRole('ADMIN')")
class AdminAccountingExportController(
    private val accountingExportService: AccountingExportService,
    private val stripeAccountingService: StripeAccountingService
) {
    private val zone = ZoneId.of("Europe/Prague")

    @GetMapping("/monthly")
    fun exportMonthlyAccounting(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) month: Int?,
        @RequestParam(defaultValue = "true") syncStripe: Boolean
    ): ResponseEntity<ByteArray> {
        val targetMonth = when {
            year != null && month != null -> YearMonth.of(year, month)
            year == null && month == null -> YearMonth.now(zone).minusMonths(1)
            else -> throw IllegalArgumentException("Both year and month must be provided")
        }

        val from = targetMonth.atDay(1).atStartOfDay(zone).toInstant()
        val to = targetMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
        if (syncStripe) {
            stripeAccountingService.syncPeriod(from, to)
        }

        val report = accountingExportService.buildMonthlyPackage(
            trainerId = UUID.fromString(principal.userId),
            month = targetMonth
        )

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${report.filename}\"")
            .contentType(MediaType.parseMediaType("application/zip"))
            .body(report.bytes)
    }
}
