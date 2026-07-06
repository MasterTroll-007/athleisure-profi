package com.fitness.service

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReservationBookingRules {
    const val MIN_CLIENT_BOOKING_LEAD_HOURS = 12L
    val BOOKING_ZONE: ZoneId = ZoneId.of("Europe/Prague")

    fun minimumClientBookingStart(now: LocalDateTime = LocalDateTime.now(BOOKING_ZONE)): LocalDateTime =
        now.plusHours(MIN_CLIENT_BOOKING_LEAD_HOURS)

    fun isClientBookingAllowed(
        date: LocalDate,
        startTime: LocalTime,
        now: LocalDateTime = LocalDateTime.now(BOOKING_ZONE)
    ): Boolean =
        !LocalDateTime.of(date, startTime).isBefore(minimumClientBookingStart(now))

    fun requireClientBookingLeadTime(date: LocalDate, startTime: LocalTime) {
        if (!isClientBookingAllowed(date, startTime)) {
            throw IllegalArgumentException(
                "Reservations must be created at least $MIN_CLIENT_BOOKING_LEAD_HOURS hours in advance"
            )
        }
    }
}
