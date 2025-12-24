package com.fitness.app.ui.components.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

data class CalendarSlot(
    val id: String?,
    val dayOfWeek: Int, // 1-7 (Monday-Sunday)
    val startTime: LocalTime,
    val endTime: LocalTime,
    val title: String = "",
    val subtitle: String = "",
    val color: Color = Color.Unspecified,
    val data: Any? = null
)

data class DragState(
    val slot: CalendarSlot,
    val originalDayOfWeek: Int,
    val originalStartTime: LocalTime,
    val newDayOfWeek: Int,
    val newStartTime: LocalTime
)

@Composable
fun WeekCalendarView(
    slots: List<CalendarSlot>,
    modifier: Modifier = Modifier,
    startHour: Int = 6,
    endHour: Int = 22,
    hourHeight: Dp = 60.dp,
    showDayHeaders: Boolean = true,
    enableDragDrop: Boolean = false,
    dragSnapMinutes: Int = 15,
    onSlotClick: ((CalendarSlot) -> Unit)? = null,
    onSlotDragEnd: ((DragState) -> Unit)? = null,
    onEmptySlotClick: ((Int, LocalTime) -> Unit)? = null, // dayOfWeek, time
    slotContent: (@Composable (CalendarSlot) -> Unit)? = null
) {
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeight.toPx() }
    val minuteHeightPx = hourHeightPx / 60f

    var draggedSlot by remember { mutableStateOf<CalendarSlot?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Day headers
        if (showDayHeaders) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp)
            ) {
                (1..7).forEach { day ->
                    val dayName = DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            HorizontalDivider()
        }

        // Calendar grid
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(endHour - startHour) { hourIndex ->
                val hour = startHour + hourIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(hourHeight)
                ) {
                    // Hour label
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Text(
                            text = String.format("%02d:00", hour),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                        )
                    }

                    // Day columns
                    (1..7).forEach { day ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                .clickable(enabled = onEmptySlotClick != null) {
                                    onEmptySlotClick?.invoke(day, LocalTime.of(hour, 0))
                                }
                        ) {
                            // Render slots for this day and hour
                            slots
                                .filter { slot ->
                                    slot.dayOfWeek == day &&
                                    slot.startTime.hour <= hour &&
                                    slot.endTime.hour > hour
                                }
                                .forEach { slot ->
                                    if (slot.startTime.hour == hour) {
                                        val durationMinutes = java.time.Duration.between(slot.startTime, slot.endTime).toMinutes()
                                        val slotHeight = (durationMinutes * minuteHeightPx / density.density).dp
                                        val topOffset = (slot.startTime.minute * minuteHeightPx / density.density).dp

                                        val isDragging = draggedSlot?.id == slot.id

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 1.dp)
                                                .offset(y = topOffset)
                                                .height(slotHeight)
                                                .then(
                                                    if (isDragging) {
                                                        Modifier.offset {
                                                            IntOffset(
                                                                dragOffset.x.roundToInt(),
                                                                dragOffset.y.roundToInt()
                                                            )
                                                        }
                                                    } else Modifier
                                                )
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (slot.color != Color.Unspecified) slot.color
                                                    else MaterialTheme.colorScheme.primaryContainer
                                                )
                                                .then(
                                                    if (enableDragDrop && onSlotDragEnd != null) {
                                                        Modifier.pointerInput(slot) {
                                                            detectDragGestures(
                                                                onDragStart = {
                                                                    draggedSlot = slot
                                                                    dragOffset = Offset.Zero
                                                                },
                                                                onDrag = { change, dragAmount ->
                                                                    change.consume()
                                                                    dragOffset += dragAmount
                                                                },
                                                                onDragEnd = {
                                                                    draggedSlot?.let { dragged ->
                                                                        // Calculate new position
                                                                        val dayWidth = size.width.toFloat()
                                                                        val dayChange = (dragOffset.x / dayWidth).roundToInt()
                                                                        val minuteChange = (dragOffset.y / minuteHeightPx).roundToInt()
                                                                        val snappedMinuteChange = (minuteChange / dragSnapMinutes) * dragSnapMinutes

                                                                        var newDay = dragged.dayOfWeek + dayChange
                                                                        newDay = newDay.coerceIn(1, 7)

                                                                        var newStartMinutes = dragged.startTime.hour * 60 + dragged.startTime.minute + snappedMinuteChange
                                                                        newStartMinutes = newStartMinutes.coerceIn(startHour * 60, (endHour - 1) * 60)
                                                                        val newStartTime = LocalTime.of(newStartMinutes / 60, newStartMinutes % 60)

                                                                        if (newDay != dragged.dayOfWeek || newStartTime != dragged.startTime) {
                                                                            onSlotDragEnd(
                                                                                DragState(
                                                                                    slot = dragged,
                                                                                    originalDayOfWeek = dragged.dayOfWeek,
                                                                                    originalStartTime = dragged.startTime,
                                                                                    newDayOfWeek = newDay,
                                                                                    newStartTime = newStartTime
                                                                                )
                                                                            )
                                                                        }
                                                                    }
                                                                    draggedSlot = null
                                                                    dragOffset = Offset.Zero
                                                                },
                                                                onDragCancel = {
                                                                    draggedSlot = null
                                                                    dragOffset = Offset.Zero
                                                                }
                                                            )
                                                        }
                                                    } else Modifier
                                                )
                                                .clickable(enabled = onSlotClick != null) {
                                                    onSlotClick?.invoke(slot)
                                                }
                                                .padding(4.dp)
                                        ) {
                                            if (slotContent != null) {
                                                slotContent(slot)
                                            } else {
                                                Column {
                                                    Text(
                                                        text = slot.title.ifEmpty {
                                                            "${slot.startTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}-${slot.endTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                    if (slot.subtitle.isNotEmpty()) {
                                                        Text(
                                                            text = slot.subtitle,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun CompactWeekCalendarView(
    slots: List<CalendarSlot>,
    modifier: Modifier = Modifier,
    onDayClick: ((Int) -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        (1..7).forEach { day ->
            val daySlots = slots.filter { it.dayOfWeek == day }
            val dayName = DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, Locale.getDefault())

            Surface(
                color = if (daySlots.isNotEmpty())
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = onDayClick != null) {
                        onDayClick?.invoke(day)
                    }
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = daySlots.size.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
