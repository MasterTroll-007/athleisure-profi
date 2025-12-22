package com.fitness.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitness.app.R
import com.fitness.app.data.dto.SlotDTO
import com.fitness.app.ui.components.ErrorContent
import com.fitness.app.ui.components.LoadingContent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCalendarScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.selectedWeekStart) {
        viewModel.loadSlots()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calendar)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_slot))
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
            // Week navigation
            WeekNavigator(
                weekStart = uiState.selectedWeekStart,
                onPreviousWeek = { viewModel.previousWeek() },
                onNextWeek = { viewModel.nextWeek() },
                onUnlockWeek = { viewModel.unlockWeek() },
                isUnlocking = uiState.isUnlocking
            )

            when {
                uiState.isLoading -> LoadingContent()
                uiState.error != null -> ErrorContent(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadSlots() }
                )
                else -> {
                    // Group slots by day
                    val slotsByDay = uiState.slots.groupBy { slot ->
                        LocalDateTime.parse(slot.startTime.removeSuffix("Z")).toLocalDate()
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Show each day of the week
                        val days = (0..6).map { uiState.selectedWeekStart.plusDays(it.toLong()) }
                        days.forEach { day ->
                            item(key = day) {
                                DayCard(
                                    date = day,
                                    slots = slotsByDay[day] ?: emptyList(),
                                    onDeleteSlot = { viewModel.deleteSlot(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create slot dialog
    if (showCreateDialog) {
        CreateSlotDialog(
            weekStart = uiState.selectedWeekStart,
            onDismiss = { showCreateDialog = false },
            onConfirm = { date, startTime, endTime ->
                viewModel.createSlot(date, startTime, endTime)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun WeekNavigator(
    weekStart: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onUnlockWeek: () -> Unit,
    isUnlocking: Boolean
) {
    val weekEnd = weekStart.plusDays(6)
    val formatter = DateTimeFormatter.ofPattern("d MMM")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousWeek) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous week")
                }

                Text(
                    text = "${weekStart.format(formatter)} - ${weekEnd.format(formatter)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onNextWeek) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next week")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onUnlockWeek,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUnlocking
            ) {
                if (isUnlocking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.unlock_week))
                }
            }
        }
    }
}

@Composable
private fun DayCard(
    date: LocalDate,
    slots: List<SlotDTO>,
    onDeleteSlot: (String) -> Unit
) {
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")
    val isToday = date == LocalDate.now()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = date.format(dayFormatter),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (slots.isEmpty()) {
                Text(
                    text = "No slots",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                slots.sortedBy { it.startTime }.forEach { slot ->
                    SlotItem(
                        slot = slot,
                        onDelete = { onDeleteSlot(slot.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SlotItem(
    slot: SlotDTO,
    onDelete: () -> Unit
) {
    val startTime = LocalDateTime.parse(slot.startTime.removeSuffix("Z"))
    val endTime = LocalDateTime.parse(slot.endTime.removeSuffix("Z"))
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val statusColor = when (slot.status) {
        "LOCKED" -> MaterialTheme.colorScheme.surfaceVariant
        "UNLOCKED" -> MaterialTheme.colorScheme.primaryContainer
        "BOOKED" -> MaterialTheme.colorScheme.tertiaryContainer
        "BLOCKED" -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = statusColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${startTime.format(timeFormatter)} - ${endTime.format(timeFormatter)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(slot.status, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                    slot.userName?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (slot.status != "BOOKED") {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateSlotDialog(
    weekStart: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, String, String) -> Unit
) {
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var startHour by remember { mutableStateOf("09") }
    var startMinute by remember { mutableStateOf("00") }
    var endHour by remember { mutableStateOf("10") }
    var endMinute by remember { mutableStateOf("00") }

    val days = (0..6).map { weekStart.plusDays(it.toLong()) }
    val dayFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_slot)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Day selector
                Text("Day", style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    days.take(4).forEachIndexed { index, date ->
                        SegmentedButton(
                            selected = selectedDayIndex == index,
                            onClick = { selectedDayIndex = index },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 4)
                        ) {
                            Text(date.dayOfWeek.name.take(3))
                        }
                    }
                }
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    days.drop(4).forEachIndexed { index, date ->
                        SegmentedButton(
                            selected = selectedDayIndex == index + 4,
                            onClick = { selectedDayIndex = index + 4 },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                        ) {
                            Text(date.dayOfWeek.name.take(3))
                        }
                    }
                }

                // Time pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start Time", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = startHour,
                                onValueChange = { if (it.length <= 2) startHour = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Text(":", modifier = Modifier.align(Alignment.CenterVertically))
                            OutlinedTextField(
                                value = startMinute,
                                onValueChange = { if (it.length <= 2) startMinute = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("End Time", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = endHour,
                                onValueChange = { if (it.length <= 2) endHour = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Text(":", modifier = Modifier.align(Alignment.CenterVertically))
                            OutlinedTextField(
                                value = endMinute,
                                onValueChange = { if (it.length <= 2) endMinute = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val date = days[selectedDayIndex]
                    val start = "${startHour.padStart(2, '0')}:${startMinute.padStart(2, '0')}"
                    val end = "${endHour.padStart(2, '0')}:${endMinute.padStart(2, '0')}"
                    onConfirm(date, start, end)
                }
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
}
