package com.fitness.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fitness.app.R
import com.fitness.app.data.dto.TemplateSlotDTO
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateCalendarEditorScreen(
    templateName: String,
    initialSlots: List<TemplateSlotDTO>,
    onSave: (String, List<TemplateSlotDTO>) -> Unit,
    onCancel: () -> Unit,
    isProcessing: Boolean = false
) {
    var name by remember { mutableStateOf(templateName) }
    var slots by remember { mutableStateOf(initialSlots) }
    var showAddSlotDialog by remember { mutableStateOf(false) }
    var showEditSlotDialog by remember { mutableStateOf<TemplateSlotDTO?>(null) }
    var selectedDayForNewSlot by remember { mutableIntStateOf(1) }
    var selectedTimeForNewSlot by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var showDragConfirmDialog by remember { mutableStateOf<DragConfirmData?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text(stringResource(R.string.template_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSave(name, slots) },
                        enabled = name.isNotBlank() && !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${slots.size} ${stringResource(R.string.slots)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilledTonalButton(
                    onClick = {
                        selectedDayForNewSlot = 1
                        selectedTimeForNewSlot = LocalTime.of(9, 0)
                        showAddSlotDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.add_slot))
                }
            }

            HorizontalDivider()

            // Calendar grid
            TemplateWeekCalendar(
                slots = slots,
                onSlotClick = { slot -> showEditSlotDialog = slot },
                onEmptySlotClick = { day, time ->
                    selectedDayForNewSlot = day
                    selectedTimeForNewSlot = time
                    showAddSlotDialog = true
                },
                onSlotDragEnd = { oldSlot, newDay, newStartTime ->
                    showDragConfirmDialog = DragConfirmData(oldSlot, newDay, newStartTime)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Add slot dialog
    if (showAddSlotDialog) {
        AddTemplateSlotDialog(
            initialDay = selectedDayForNewSlot,
            initialStartTime = selectedTimeForNewSlot,
            onDismiss = { showAddSlotDialog = false },
            onAdd = { slot ->
                slots = slots + slot
                showAddSlotDialog = false
            }
        )
    }

    // Edit slot dialog
    showEditSlotDialog?.let { slot ->
        EditTemplateSlotDialog(
            slot = slot,
            onDismiss = { showEditSlotDialog = null },
            onUpdate = { updatedSlot ->
                slots = slots.map { if (it.id == slot.id || (it.id == null && it == slot)) updatedSlot else it }
                showEditSlotDialog = null
            },
            onDelete = {
                slots = slots.filter { it.id != slot.id && it != slot }
                showEditSlotDialog = null
            }
        )
    }

    // Drag confirm dialog
    showDragConfirmDialog?.let { data ->
        AlertDialog(
            onDismissRequest = { showDragConfirmDialog = null },
            title = { Text(stringResource(R.string.move_slot)) },
            text = {
                val oldDayName = DayOfWeek.of(data.oldSlot.dayOfWeek).getDisplayName(TextStyle.FULL, Locale.getDefault())
                val newDayName = DayOfWeek.of(data.newDay).getDisplayName(TextStyle.FULL, Locale.getDefault())
                Text(
                    stringResource(
                        R.string.confirm_move_slot,
                        oldDayName,
                        data.oldSlot.startTime,
                        newDayName,
                        data.newStartTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    val duration = try {
                        val startMinutes = data.oldSlot.startTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
                        val endMinutes = data.oldSlot.endTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
                        endMinutes - startMinutes
                    } catch (e: Exception) { 60 }

                    val newEndTime = data.newStartTime.plusMinutes(duration.toLong())
                    val updatedSlot = data.oldSlot.copy(
                        dayOfWeek = data.newDay,
                        startTime = data.newStartTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                        endTime = newEndTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    )
                    slots = slots.map { if (it.id == data.oldSlot.id || (it.id == null && it == data.oldSlot)) updatedSlot else it }
                    showDragConfirmDialog = null
                }) {
                    Text(stringResource(R.string.move))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDragConfirmDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private data class DragConfirmData(
    val oldSlot: TemplateSlotDTO,
    val newDay: Int,
    val newStartTime: LocalTime
)

@Composable
private fun TemplateWeekCalendar(
    slots: List<TemplateSlotDTO>,
    onSlotClick: (TemplateSlotDTO) -> Unit,
    onEmptySlotClick: (Int, LocalTime) -> Unit,
    onSlotDragEnd: (TemplateSlotDTO, Int, LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    startHour: Int = 6,
    endHour: Int = 22
) {
    val hourHeight = 60.dp
    val dayColumnWidth = 100.dp
    val timeColumnWidth = 48.dp
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeight.toPx() }
    val dayColumnWidthPx = with(density) { dayColumnWidth.toPx() }

    var draggedSlot by remember { mutableStateOf<TemplateSlotDTO?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    Column(modifier = modifier) {
        // Day headers (fixed)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
        ) {
            Spacer(modifier = Modifier.width(timeColumnWidth))
            (1..7).forEach { day ->
                val dayName = DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                Box(
                    modifier = Modifier
                        .width(dayColumnWidth)
                        .padding(vertical = 8.dp),
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

        // Calendar grid (scrollable)
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(horizontalScrollState)
        ) {
            // Time column
            Column(
                modifier = Modifier
                    .width(timeColumnWidth)
                    .verticalScroll(verticalScrollState)
            ) {
                (startHour until endHour).forEach { hour ->
                    Box(
                        modifier = Modifier
                            .height(hourHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Text(
                            text = String.format("%02d:00", hour),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                        )
                    }
                }
            }

            // Day columns
            (1..7).forEach { day ->
                Box(
                    modifier = Modifier
                        .width(dayColumnWidth)
                        .verticalScroll(verticalScrollState)
                ) {
                    Column {
                        (startHour until endHour).forEach { hour ->
                            Box(
                                modifier = Modifier
                                    .height(hourHeight)
                                    .fillMaxWidth()
                                    .border(
                                        width = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    .clickable {
                                        onEmptySlotClick(day, LocalTime.of(hour, 0))
                                    }
                            )
                        }
                    }

                    // Render slots for this day
                    slots
                        .filter { it.dayOfWeek == day }
                        .forEach { slot ->
                            val startTime = try {
                                LocalTime.parse(slot.startTime)
                            } catch (e: Exception) {
                                slot.startTime.split(":").let {
                                    LocalTime.of(it[0].toInt(), it[1].toInt())
                                }
                            }
                            val endTime = try {
                                LocalTime.parse(slot.endTime)
                            } catch (e: Exception) {
                                slot.endTime.split(":").let {
                                    LocalTime.of(it[0].toInt(), it[1].toInt())
                                }
                            }

                            if (startTime.hour >= startHour && startTime.hour < endHour) {
                                val topOffset = ((startTime.hour - startHour) * 60 + startTime.minute) * hourHeightPx / 60f
                                val durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes()
                                val slotHeightPx = durationMinutes * hourHeightPx / 60f

                                val isDragging = draggedSlot == slot

                                Box(
                                    modifier = Modifier
                                        .offset(y = with(density) { topOffset.toDp() })
                                        .padding(horizontal = 2.dp)
                                        .height(with(density) { slotHeightPx.toDp() })
                                        .fillMaxWidth()
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
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .pointerInput(slot) {
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
                                                        val dayChange = (dragOffset.x / dayColumnWidthPx).roundToInt()
                                                        val minuteChange = (dragOffset.y / (hourHeightPx / 60f)).roundToInt()
                                                        val snappedMinuteChange = (minuteChange / 15) * 15

                                                        var newDay = dragged.dayOfWeek + dayChange
                                                        newDay = newDay.coerceIn(1, 7)

                                                        val oldStartMinutes = startTime.hour * 60 + startTime.minute
                                                        var newStartMinutes = oldStartMinutes + snappedMinuteChange
                                                        newStartMinutes = newStartMinutes.coerceIn(startHour * 60, (endHour - 1) * 60)
                                                        val newStartTime = LocalTime.of(newStartMinutes / 60, newStartMinutes % 60)

                                                        if (newDay != dragged.dayOfWeek || newStartTime != startTime) {
                                                            onSlotDragEnd(dragged, newDay, newStartTime)
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
                                        .clickable { onSlotClick(slot) }
                                        .padding(4.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "${slot.startTime}-${slot.endTime}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTemplateSlotDialog(
    initialDay: Int,
    initialStartTime: LocalTime,
    onDismiss: () -> Unit,
    onAdd: (TemplateSlotDTO) -> Unit
) {
    var selectedDay by remember { mutableIntStateOf(initialDay) }
    var startTime by remember { mutableStateOf(initialStartTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))) }
    var endTime by remember { mutableStateOf(initialStartTime.plusHours(1).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_slot)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.day), style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (1..7).forEach { day ->
                        val dayName = DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = { Text(dayName, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text(stringResource(R.string.start_time)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("09:00") }
                )

                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text(stringResource(R.string.end_time)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("10:00") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val duration = try {
                        val start = startTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
                        val end = endTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
                        end - start
                    } catch (e: Exception) { 60 }

                    onAdd(TemplateSlotDTO(
                        id = UUID.randomUUID().toString(),
                        dayOfWeek = selectedDay,
                        startTime = startTime,
                        endTime = endTime,
                        durationMinutes = duration
                    ))
                },
                enabled = startTime.isNotBlank() && endTime.isNotBlank()
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTemplateSlotDialog(
    slot: TemplateSlotDTO,
    onDismiss: () -> Unit,
    onUpdate: (TemplateSlotDTO) -> Unit,
    onDelete: () -> Unit
) {
    var selectedDay by remember { mutableIntStateOf(slot.dayOfWeek) }
    var startTime by remember { mutableStateOf(slot.startTime) }
    var endTime by remember { mutableStateOf(slot.endTime) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_slot)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.day), style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (1..7).forEach { day ->
                        val dayName = DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = { Text(dayName, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text(stringResource(R.string.start_time)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text(stringResource(R.string.end_time)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.delete_slot))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val duration = try {
                        val start = startTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
                        val end = endTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
                        end - start
                    } catch (e: Exception) { 60 }

                    onUpdate(slot.copy(
                        dayOfWeek = selectedDay,
                        startTime = startTime,
                        endTime = endTime,
                        durationMinutes = duration
                    ))
                },
                enabled = startTime.isNotBlank() && endTime.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_slot)) },
            text = { Text(stringResource(R.string.confirm_delete_slot)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
