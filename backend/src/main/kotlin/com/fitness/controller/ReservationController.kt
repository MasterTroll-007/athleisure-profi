package com.fitness.controller

import com.fitness.dto.*
import com.fitness.security.UserPrincipal
import com.fitness.service.AvailabilityService
import com.fitness.service.ReservationService
import com.fitness.service.WorkoutLogService
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/reservations")
class ReservationController(
    private val reservationService: ReservationService,
    private val availabilityService: AvailabilityService,
    private val workoutLogService: WorkoutLogService
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
        if (endDate.isBefore(startDate)) {
            throw IllegalArgumentException("end must be after start")
        }
        // Cap the range to protect against clients requesting huge scans.
        if (endDate.isAfter(startDate.plusDays(93))) {
            throw IllegalArgumentException("Date range too wide (max ~3 months)")
        }
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

    @GetMapping("/{id}/workout")
    fun getWorkoutLog(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String
    ): ResponseEntity<Any> {
        // Verify ownership
        val reservation = reservationService.getReservationById(id)
        if (reservation.userId != principal.userId && principal.role != "admin") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val log = workoutLogService.getWorkoutLog(id)
        return ResponseEntity.ok(log)
    }

    @GetMapping("/workouts/my")
    fun getMyWorkouts(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<List<com.fitness.dto.WorkoutLogDTO>> {
        val logs = workoutLogService.getMyWorkoutLogs(principal.userId)
        return ResponseEntity.ok(logs)
    }

    @GetMapping("/ical")
    fun exportIcal(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ByteArray> {
        val reservations = reservationService.getUpcomingReservations(principal.userId)
        val icalFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
        val zone = ZoneId.of("Europe/Prague")

        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//Athleisure-Domi//Reservations//CS")
        sb.appendLine("CALSCALE:GREGORIAN")

        for (res in reservations) {
            if (res.status == "cancelled") continue
            val date = LocalDate.parse(res.date)
            val start = LocalDateTime.of(date, java.time.LocalTime.parse(res.startTime))
            val end = LocalDateTime.of(date, java.time.LocalTime.parse(res.endTime))
            val startUtc = start.atZone(zone).withZoneSameInstant(ZoneId.of("UTC"))
            val endUtc = end.atZone(zone).withZoneSameInstant(ZoneId.of("UTC"))

            sb.appendLine("BEGIN:VEVENT")
            sb.appendLine("DTSTART:${startUtc.format(icalFormatter)}")
            sb.appendLine("DTEND:${endUtc.format(icalFormatter)}")
            sb.appendLine("SUMMARY:Trénink${res.pricingItemName?.let { " - $it" } ?: ""}")
            sb.appendLine("UID:${res.id}@athleisure-domi")
            sb.appendLine("END:VEVENT")
        }
        sb.appendLine("END:VCALENDAR")

        val bytes = sb.toString().toByteArray(Charsets.UTF_8)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reservations.ics\"")
            .contentType(MediaType.parseMediaType("text/calendar"))
            .body(bytes)
    }
}
