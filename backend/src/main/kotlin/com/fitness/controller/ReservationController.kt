package com.fitness.controller

import com.fitness.dto.*
import com.fitness.security.UserPrincipal
import com.fitness.service.AvailabilityService
import com.fitness.service.ReservationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/reservations")
class ReservationController(
    private val reservationService: ReservationService,
    private val availabilityService: AvailabilityService
) {

    @GetMapping("/available/{date}")
    fun getAvailableSlots(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable date: String
    ): ResponseEntity<AvailableSlotsResponse> {
        val localDate = LocalDate.parse(date)
        val slots = availabilityService.getAvailableSlots(localDate, principal.userId)
        return ResponseEntity.ok(AvailableSlotsResponse(slots = slots))
    }

    @GetMapping("/available")
    fun getAvailableSlotsRange(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam start: String,
        @RequestParam end: String
    ): ResponseEntity<AvailableSlotsResponse> {
        val startDate = LocalDate.parse(start)
        val endDate = LocalDate.parse(end)
        val slots = availabilityService.getAvailableSlotsRange(startDate, endDate, principal.userId)
        return ResponseEntity.ok(AvailableSlotsResponse(slots = slots))
    }

    @PostMapping
    fun createReservation(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateReservationRequest
    ): ResponseEntity<ReservationDTO> {
        val reservation = reservationService.createReservation(principal.userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation)
    }

    @GetMapping
    fun getMyReservations(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<ReservationDTO>> {
        val reservations = reservationService.getUserReservations(principal.userId)
        return ResponseEntity.ok(reservations)
    }

    @GetMapping("/upcoming")
    fun getUpcomingReservations(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<ReservationDTO>> {
        val reservations = reservationService.getUpcomingReservations(principal.userId)
        return ResponseEntity.ok(reservations)
    }

    @GetMapping("/{id}")
    fun getReservation(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<ReservationDTO> {
        val reservation = reservationService.getReservationById(id)

        if (reservation.userId != principal.userId && principal.role != "admin") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        return ResponseEntity.ok(reservation)
    }

    @DeleteMapping("/{id}")
    fun cancelReservation(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<CancellationResultDTO> {
        val result = reservationService.cancelReservation(principal.userId, id)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{id}/refund-preview")
    fun getRefundPreview(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<CancellationRefundPreviewDTO> {
        val preview = reservationService.getRefundPreview(principal.userId, id)
        return ResponseEntity.ok(preview)
    }
}
