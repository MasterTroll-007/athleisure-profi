package com.fitness.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.Month
import java.util.Locale

class DateUtilsTest {

    @Test
    fun `parseDateTime strips Z suffix from ISO UTC string`() {
        val dt = DateUtils.parseDateTime("2026-04-17T10:00:00Z")!!
        assertEquals(2026, dt.year)
        assertEquals(Month.APRIL, dt.month)
        assertEquals(17, dt.dayOfMonth)
        assertEquals(10, dt.hour)
    }

    @Test
    fun `parseDateTime returns null for malformed or empty input`() {
        assertNull(DateUtils.parseDateTime(null))
        assertNull(DateUtils.parseDateTime(""))
        assertNull(DateUtils.parseDateTime("not-a-date"))
    }

    @Test
    fun `formatTime returns zero-padded HH mm`() {
        val dt = LocalDateTime.of(2026, 4, 17, 9, 5)
        assertEquals("09:05", DateUtils.formatTime(dt, locale = Locale.ENGLISH))
    }

    @Test
    fun `formatDate with en locale contains month text`() {
        val dt = LocalDateTime.of(2026, 4, 17, 0, 0)
        val out = DateUtils.formatDate(dt, locale = Locale.ENGLISH).lowercase()
        assertEquals(true, out.contains("apr"))
    }

    @Test
    fun `formatters return empty string on null input`() {
        assertEquals("", DateUtils.formatDateTime(null))
        assertEquals("", DateUtils.formatDate(null))
        assertEquals("", DateUtils.formatTime(null))
    }
}
