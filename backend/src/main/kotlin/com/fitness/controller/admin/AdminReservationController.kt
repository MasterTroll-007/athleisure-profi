package com.fitness.controller.admin

import com.fitness.dto.*
import com.fitness.repository.UserRepository
import com.fitness.security.UserPrincipal
import com.fitness.service.ReservationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/admin/reservations")
@PreAuthorize("hasRole('ADMIN')")
class AdminReservationController(
    private val reservationService: ReservationService,
    private val userRepository: UserRepository
) {
    @GetMapping
    fun getReservations(
        @RequestParam(required = false) start: String?,
        @RequestParam(required = false) end: String?
    ): ResponseEntity<List<ReservationCalendarEvent>> {
        val startDate = start?.let { LocalDate.parse(it) } ?: LocalDate.now().minusMonths(1)
        val endDate = end?.let { LocalDate.parse(it) } ?: LocalDate.now().plusMonths(2)
        val reservations = reservationService.getAllReservations(startDate, endDate)
        return ResponseEntity.ok(reservations)
    }

    @PostMapping
    fun adminCreateReservation(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: AdminCreateReservationRequest
    ): ResponseEntity<Any> {
        return try {
            val admin = userRepository.findById(UUID.fromString(principal.userId)).orElse(null)
            val reservation = reservationService.adminCreateReservation(request, principal.userId, admin?.email)
            ResponseEntity.status(HttpStatus.CREATED).body(reservation)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/{id}")
    fun adminCancelReservation(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String,
        @RequestParam(defaultValue = "true") refundCredits: Boolean
    ): ResponseEntity<Any> {
        return try {
            val admin = userRepository.findById(UUID.fromString(principal.userId)).orElse(null)
            val reservation = reservationService.adminCancelReservation(id, refundCredits, principal.userId, admin?.email)
            ResponseEntity.ok(reservation)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message))
        }
    }

    @PatchMapping("/{id}/note")
    fun updateReservationNote(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateReservationNoteRequest
    ): ResponseEntity<Any> {
        return try {
            val reservation = reservationService.updateReservationNote(id, request.note)
            ResponseEntity.ok(reservation)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))
        }
    }
}
