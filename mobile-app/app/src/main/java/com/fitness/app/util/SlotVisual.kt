package com.fitness.app.util

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import com.fitness.app.data.dto.AvailableSlotDTO
import com.fitness.app.data.dto.SlotDTO

/**
 * Visual descriptor for a calendar slot combining location color with status.
 *
 * The fill color carries the training location; the status is communicated via
 * opacity, an optional overlay icon, and an optional diagonal stripe pattern.
 */
data class SlotVisual(
    val baseColor: Color,
    val opacity: Float,
    val showStripes: Boolean,
    val icon: String?
)

private val NEUTRAL_GRAY = Color(0xFF9CA3AF)

private fun parseHex(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        Color(AndroidColor.parseColor(hex))
    } catch (_: IllegalArgumentException) {
        null
    }
}

fun resolveAdminSlotVisual(slot: SlotDTO): SlotVisual {
    val base = parseHex(slot.locationColor) ?: NEUTRAL_GRAY
    // opacity applies only to the fill color below — text layer never fades.
    return when (slot.status.lowercase()) {
        "reserved", "booked" -> SlotVisual(base, 1f, showStripes = false, icon = null)
        "cancelled" -> SlotVisual(base, 0.55f, showStripes = true, icon = "❌")
        "locked" -> SlotVisual(base, 0.25f, showStripes = false, icon = "🔒")
        "blocked" -> SlotVisual(base, 0.3f, showStripes = false, icon = "⛔")
        else -> SlotVisual(base, 0.2f, showStripes = false, icon = null) // unlocked
    }
}

fun resolveAvailableSlotVisual(slot: AvailableSlotDTO): SlotVisual {
    val base = parseHex(slot.locationColor) ?: NEUTRAL_GRAY
    if (!slot.isAvailable) {
        return SlotVisual(base, 0.3f, showStripes = true, icon = null)
    }
    return SlotVisual(base, 0.2f, showStripes = false, icon = null)
}
