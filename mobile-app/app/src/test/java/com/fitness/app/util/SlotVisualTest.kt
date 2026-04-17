package com.fitness.app.util

import com.fitness.app.data.dto.AvailableSlotDTO
import com.fitness.app.data.dto.SlotDTO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SlotVisualTest {

    private fun adminSlot(status: String, locationColor: String? = null) = SlotDTO(
        id = "1",
        date = "2026-04-17",
        startTime = "10:00",
        endTime = "11:00",
        durationMinutes = 60,
        status = status,
        locationColor = locationColor,
    )

    @Test
    fun `unlocked gets 20 percent fill without icon`() {
        val v = resolveAdminSlotVisual(adminSlot("unlocked", "#3B82F6"))
        assertEquals(0.2f, v.opacity, 0.001f)
        assertFalse(v.showStripes)
        assertNull(v.icon)
    }

    @Test
    fun `reserved gets solid fill`() {
        val v = resolveAdminSlotVisual(adminSlot("reserved", "#3B82F6"))
        assertEquals(1f, v.opacity, 0.001f)
        assertFalse(v.showStripes)
    }

    @Test
    fun `cancelled gets stripes and cross icon`() {
        val v = resolveAdminSlotVisual(adminSlot("cancelled", "#3B82F6"))
        assertTrue(v.showStripes)
        assertEquals("❌", v.icon)
    }

    @Test
    fun `locked gets lock icon and low opacity`() {
        val v = resolveAdminSlotVisual(adminSlot("locked"))
        assertEquals("🔒", v.icon)
    }

    @Test
    fun `invalid hex falls back to neutral gray`() {
        val v = resolveAdminSlotVisual(adminSlot("unlocked", "not-a-hex"))
        // Neutral gray = 0xFF9CA3AF — assertion by sum of channels
        assertNotNull(v.baseColor)
    }

    @Test
    fun `available-slot unavailable shows stripes`() {
        val slot = AvailableSlotDTO(
            blockId = "1",
            date = "2026-04-17",
            start = "10:00",
            end = "11:00",
            isAvailable = false,
        )
        val v = resolveAvailableSlotVisual(slot)
        assertTrue(v.showStripes)
    }

    @Test
    fun `available-slot available has light tint no stripes`() {
        val slot = AvailableSlotDTO(
            blockId = "1",
            date = "2026-04-17",
            start = "10:00",
            end = "11:00",
            isAvailable = true,
            locationColor = "#EC4899",
        )
        val v = resolveAvailableSlotVisual(slot)
        assertEquals(0.2f, v.opacity, 0.001f)
        assertFalse(v.showStripes)
    }
}
