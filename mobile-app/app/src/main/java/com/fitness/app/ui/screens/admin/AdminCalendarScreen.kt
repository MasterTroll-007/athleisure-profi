package com.fitness.app.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitness.app.R
import com.fitness.app.data.dto.ClientDTO
import com.fitness.app.data.dto.SlotDTO
import com.fitness.app.ui.components.ErrorContent
import com.fitness.app.ui.components.LoadingContent
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private const val HOUR_HEIGHT_DP = 48
private const val TIME_COLUMN_WIDTH_DP = 40
private const val START_HOUR = 6
private const val END_HOUR = 22

enum class CalendarViewMode(val days: Int, val label: String) {
    DAY(1, "1"),
    THREE_DAYS(3, "3"),
    WEEK(7, "7")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCalendarScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Drag mode - when false, calendar scrolls normally; when true, slots can be dragged
    var isDragEnabled by remember { mutableStateOf(false) }

    // Calendar view mode
    var viewMode by remember { mutableStateOf(CalendarViewMode.THREE_DAYS) }

    // Dialog states
    var showCreateDialog by remember { mutableStateOf(false) }
    var createDialogPrefilledDate by remember { mutableStateOf<LocalDate?>(null) }
    var createDialogPrefilledTime by remember { mutableStateOf<LocalTime?>(null) }
    var selectedSlot by remember { mutableStateOf<SlotDTO?>(null) }
    var showSlotDetail by remember { mutableStateOf(false) }
    var showUserSearch by remember { mutableStateOf(false) }
    var showUserSearchForCreate by remember { mutableStateOf(false) }
    var showDragConfirm by remember { mutableStateOf(false) }
    var pendingDragMove by remember { mutableStateOf<DragMoveData?>(null) }
    var pendingCreateSlotData by remember { mutableStateOf<CreateSlotData?>(null) }

    LaunchedEffect(uiState.selectedWeekStart) {
        viewModel.loadSlots()
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.calendar)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        // Drag mode toggle in app bar
                        IconButton(onClick = { isDragEnabled = !isDragEnabled }) {
                            Icon(
                                imageVector = if (isDragEnabled) Icons.Default.OpenWith else Icons.Default.Lock,
                                contentDescription = if (isDragEnabled) stringResource(R.string.lock_drag) else stringResource(R.string.unlock_drag),
                                tint = if (isDragEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Overflow menu
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.unlock_week)) },
                                    onClick = {
                                        viewModel.unlockWeek()
                                        showMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null) },
                                    enabled = !uiState.isUnlocking
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        createDialogPrefilledDate = null
                        createDialogPrefilledTime = null
                        showCreateDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_slot))
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Clean date navigation
                DateNavigationBar(
                    startDate = uiState.selectedWeekStart,
                    visibleDays = viewMode.days,
                    viewMode = viewMode,
                    onViewModeChange = { viewMode = it },
                    onPrevious = { viewModel.previousDays(viewMode.days) },
                    onNext = { viewModel.nextDays(viewMode.days) }
                )

                when {
                    uiState.isLoading -> LoadingContent()
                    uiState.error != null -> ErrorContent(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadSlots() }
                    )
                    else -> CalendarGridView(
                        startDate = uiState.selectedWeekStart,
                        visibleDays = viewMode.days,
                        slots = uiState.slots,
                        isDragEnabled = isDragEnabled,
                        onSlotClick = { slot ->
                            selectedSlot = slot
                            showSlotDetail = true
                        },
                        onSlotDrag = { slot, newDate, newStartTime ->
                            pendingDragMove = DragMoveData(slot, newDate, newStartTime)
                            showDragConfirm = true
                        },
                        onEmptyClick = { date, time ->
                            createDialogPrefilledDate = date
                            createDialogPrefilledTime = time
                            showCreateDialog = true
                        }
                    )
                }
            }
        }

        // Fullscreen User Search (for existing slot)
        AnimatedVisibility(
            visible = showUserSearch,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            UserSearchScreen(
                onBack = { showUserSearch = false },
                onSelectUser = { client ->
                    selectedSlot?.let { slot ->
                        viewModel.assignUser(slot.id, client.id)
                    }
                    showUserSearch = false
                    showSlotDetail = false
                },
                viewModel = viewModel
            )
        }

        // Fullscreen User Search (for new slot creation)
        AnimatedVisibility(
            visible = showUserSearchForCreate,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            UserSearchScreen(
                onBack = { showUserSearchForCreate = false },
                onSelectUser = { client ->
                    pendingCreateSlotData?.let { data ->
                        viewModel.createSlotWithUser(data.date, data.startTime, data.endTime, client.id)
                    }
                    showUserSearchForCreate = false
                    showCreateDialog = false
                    pendingCreateSlotData = null
                },
                viewModel = viewModel
            )
        }
    }

    // Slot Detail Dialog - only show if not in user search mode
    if (showSlotDetail && selectedSlot != null && !showUserSearch) {
        SlotDetailDialog(
            slot = selectedSlot!!,
            onDismiss = {
                showSlotDetail = false
                selectedSlot = null
            },
            onAssignUser = {
                showUserSearch = true
            },
            onUnassignUser = {
                viewModel.unassignUser(selectedSlot!!.id)
                showSlotDetail = false
                selectedSlot = null
            },
            onDelete = {
                viewModel.deleteSlot(selectedSlot!!.id)
                showSlotDetail = false
                selectedSlot = null
            },
            onLockSlot = {
                viewModel.lockSlot(selectedSlot!!.id)
                showSlotDetail = false
                selectedSlot = null
            },
            onUnlockSlot = {
                viewModel.unlockSlot(selectedSlot!!.id)
                showSlotDetail = false
                selectedSlot = null
            },
            isProcessing = uiState.isProcessing
        )
    }

    // Create slot dialog
    if (showCreateDialog && !showUserSearchForCreate) {
        CreateSlotDialog(
            weekStart = uiState.selectedWeekStart,
            prefilledDate = createDialogPrefilledDate,
            prefilledTime = createDialogPrefilledTime,
            onDismiss = {
                showCreateDialog = false
                createDialogPrefilledDate = null
                createDialogPrefilledTime = null
            },
            onConfirm = { date, startTime, endTime ->
                viewModel.createSlot(date, startTime, endTime)
                showCreateDialog = false
                createDialogPrefilledDate = null
                createDialogPrefilledTime = null
            },
            onConfirmWithUser = { date, startTime, endTime ->
                pendingCreateSlotData = CreateSlotData(date, startTime, endTime)
                showUserSearchForCreate = true
            }
        )
    }

    // Drag confirmation dialog
    if (showDragConfirm && pendingDragMove != null) {
        val move = pendingDragMove!!
        val oldDayName = LocalDate.parse(move.slot.date).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val newDayName = move.newDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

        AlertDialog(
            onDismissRequest = {
                showDragConfirm = false
                pendingDragMove = null
            },
            title = { Text(stringResource(R.string.move_slot)) },
            text = {
                Text(stringResource(
                    R.string.confirm_move_slot,
                    oldDayName,
                    move.slot.startTime,
                    newDayName,
                    move.newStartTime
                ))
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.moveSlot(
                            move.slot.id,
                            move.newDate.toString(),
                            move.newStartTime,
                            calculateEndTime(move.newStartTime, move.slot.durationMinutes)
                        )
                        showDragConfirm = false
                        pendingDragMove = null
                    }
                ) {
                    Text(stringResource(R.string.move))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDragConfirm = false
                    pendingDragMove = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private data class DragMoveData(
    val slot: SlotDTO,
    val newDate: LocalDate,
    val newStartTime: String
)

private data class CreateSlotData(
    val date: LocalDate,
    val startTime: String,
    val endTime: String
)

private fun calculateEndTime(startTime: String, durationMinutes: Int): String {
    val parts = startTime.split(":")
    val startMinutes = parts[0].toInt() * 60 + parts[1].toInt()
    val endMinutes = startMinutes + durationMinutes
    return "%02d:%02d".format(endMinutes / 60, endMinutes % 60)
}

@Composable
private fun DateNavigationBar(
    startDate: LocalDate,
    visibleDays: Int,
    viewMode: CalendarViewMode,
    onViewModeChange: (CalendarViewMode) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val endDate = startDate.plusDays(visibleDays.toLong() - 1)
    val formatter = DateTimeFormatter.ofPattern("d. MMMM", Locale.getDefault())
    val shortFormatter = DateTimeFormatter.ofPattern("d.M.")

    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date navigation - arrows close to date
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = stringResource(R.string.previous_week),
                    modifier = Modifier
                        .clickable(onClick = onPrevious)
                        .padding(8.dp)
                        .size(24.dp)
                )

                Text(
                    text = if (visibleDays == 1) {
                        startDate.format(formatter)
                    } else {
                        "${startDate.format(shortFormatter)} - ${endDate.format(shortFormatter)}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = stringResource(R.string.next_week),
                    modifier = Modifier
                        .clickable(onClick = onNext)
                        .padding(8.dp)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // View mode toggle - compact, no checkmark
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.height(36.dp)
            ) {
                CalendarViewMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = viewMode == mode,
                        onClick = { onViewModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = CalendarViewMode.entries.size
                        ),
                        icon = {} // No checkmark
                    ) {
                        Text(text = mode.label)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarGridView(
    startDate: LocalDate,
    visibleDays: Int,
    slots: List<SlotDTO>,
    isDragEnabled: Boolean,
    onSlotClick: (SlotDTO) -> Unit,
    onSlotDrag: (SlotDTO, LocalDate, String) -> Unit,
    onEmptyClick: (LocalDate, LocalTime) -> Unit
) {
    val density = LocalDensity.current
    val hourHeightPx = with(density) { HOUR_HEIGHT_DP.dp.toPx() }

    val days = (0 until visibleDays).map { startDate.plusDays(it.toLong()) }
    val slotsByDay = slots.groupBy { LocalDate.parse(it.date) }

    val verticalScrollState = rememberScrollState()

    // Calculate column width dynamically based on screen width
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableWidth = maxWidth - TIME_COLUMN_WIDTH_DP.dp
        val columnWidth = availableWidth / visibleDays
        val columnWidthPx = with(density) { columnWidth.toPx() }

        Column(modifier = Modifier.fillMaxSize()) {
            // Day headers - compact single line
            Row(modifier = Modifier.fillMaxWidth()) {
                // Empty corner for time column
                Box(modifier = Modifier.width(TIME_COLUMN_WIDTH_DP.dp))

                days.forEach { date ->
                    val isToday = date == LocalDate.now()
                    val dayName = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                    val dayNum = date.dayOfMonth

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isToday) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$dayName $dayNum",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            HorizontalDivider()

            // Calendar grid
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
            ) {
                // Time column - compact hour labels
                Column(modifier = Modifier.width(TIME_COLUMN_WIDTH_DP.dp)) {
                    (START_HOUR..END_HOUR).forEach { hour ->
                        Box(
                            modifier = Modifier
                                .height(HOUR_HEIGHT_DP.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Text(
                                text = "$hour",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(end = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Day columns - fill available width equally
                days.forEachIndexed { dayIndex, date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(((END_HOUR - START_HOUR + 1) * HOUR_HEIGHT_DP).dp)
                    ) {
                        // Hour grid lines with 15-minute subdivisions - clickable for creating slots
                        Column {
                            (START_HOUR..END_HOUR).forEach { hour ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(HOUR_HEIGHT_DP.dp)
                                        .border(
                                            width = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                ) {
                                    // 15-minute subdivision lines - each clickable
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        repeat(4) { quarter ->
                                            val clickTime = LocalTime.of(hour, quarter * 15)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                                    .clickable { onEmptyClick(date, clickTime) }
                                            ) {
                                                if (quarter > 0) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        thickness = 0.5.dp,
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                            alpha = if (quarter == 2) 0.5f else 0.25f
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Slots for this day
                        val daySlots = slotsByDay[date] ?: emptyList()
                        daySlots.forEach { slot ->
                            SlotBlock(
                                slot = slot,
                                hourHeightPx = hourHeightPx,
                                columnWidthPx = columnWidthPx,
                                isDragEnabled = isDragEnabled,
                                onClick = { onSlotClick(slot) },
                                onDragEnd = { newDayOffset, newTimeOffsetPx ->
                                    // Calculate new day
                                    val dayChange = (newDayOffset / columnWidthPx).roundToInt()
                                    val newDayIndex = (dayIndex + dayChange).coerceIn(0, visibleDays - 1)
                                    val newDate = days[newDayIndex]

                                    // Calculate new time (snap to 15 min)
                                    val startTime = LocalTime.parse(slot.startTime)
                                    val startMinutes = startTime.hour * 60 + startTime.minute - START_HOUR * 60
                                    val startOffsetPx = (startMinutes / 60f) * hourHeightPx
                                    val newOffsetPx = startOffsetPx + newTimeOffsetPx
                                    val newMinutes = ((newOffsetPx / hourHeightPx) * 60).roundToInt()
                                    val snappedMinutes = ((newMinutes / 15) * 15).coerceIn(0, (END_HOUR - START_HOUR) * 60)
                                    val totalMinutes = START_HOUR * 60 + snappedMinutes
                                    val newStartTime = "%02d:%02d".format(totalMinutes / 60, totalMinutes % 60)

                                    // Only trigger if actually moved
                                    if (newDate != date || newStartTime != slot.startTime) {
                                        onSlotDrag(slot, newDate, newStartTime)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotBlock(
    slot: SlotDTO,
    hourHeightPx: Float,
    columnWidthPx: Float,
    isDragEnabled: Boolean,
    onClick: () -> Unit,
    onDragEnd: (Float, Float) -> Unit
) {
    val startTime = LocalTime.parse(slot.startTime)
    val startMinutes = startTime.hour * 60 + startTime.minute - START_HOUR * 60
    val topOffset = (startMinutes / 60f) * hourHeightPx
    val height = (slot.durationMinutes / 60f) * hourHeightPx

    // 15-minute snap height in pixels
    val snapHeightPx = hourHeightPx / 4f

    var rawOffsetX by remember { mutableFloatStateOf(0f) }
    var rawOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Snapped offsets for visual display during drag
    val displayOffsetX = if (isDragging) {
        (rawOffsetX / columnWidthPx).roundToInt() * columnWidthPx
    } else 0f
    val displayOffsetY = if (isDragging) {
        (rawOffsetY / snapHeightPx).roundToInt() * snapHeightPx
    } else 0f

    // Colors: locked=grey, unlocked=green (material teal for better design fit)
    val statusColor = when (slot.status.lowercase()) {
        "locked" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        "unlocked" -> Color(0xFF26A69A).copy(alpha = 0.4f) // Teal 400 - fits material design
        "booked", "reserved" -> MaterialTheme.colorScheme.primaryContainer
        "blocked" -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val density = LocalDensity.current
    val heightDp = with(density) { height.toDp() }

    // Calculate live time during drag
    val displayTimeRange = if (isDragging) {
        val snappedOffsetMinutes = ((displayOffsetY / hourHeightPx) * 60).roundToInt()
        val newStartMinutes = (startTime.hour * 60 + startTime.minute + snappedOffsetMinutes)
            .coerceIn(START_HOUR * 60, END_HOUR * 60)
        val newEndMinutes = (newStartMinutes + slot.durationMinutes)
            .coerceAtMost((END_HOUR + 1) * 60)
        "%02d:%02d - %02d:%02d".format(
            newStartMinutes / 60, newStartMinutes % 60,
            newEndMinutes / 60, newEndMinutes % 60
        )
    } else {
        "${slot.startTime} - ${slot.endTime}"
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = displayOffsetX.roundToInt(),
                    y = topOffset.roundToInt() + displayOffsetY.roundToInt()
                )
            }
            .padding(horizontal = 2.dp)
            .fillMaxWidth()
            .height(heightDp.coerceAtLeast(24.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(if (isDragging) statusColor.copy(alpha = 0.8f) else statusColor)
            .border(1.dp, Color.White, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .then(
                if (isDragEnabled) {
                    Modifier.pointerInput(slot.id, columnWidthPx, snapHeightPx) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                if (isDragging) {
                                    // Calculate snapped values at the moment of release
                                    val finalSnappedX = (rawOffsetX / columnWidthPx).roundToInt() * columnWidthPx
                                    val finalSnappedY = (rawOffsetY / snapHeightPx).roundToInt() * snapHeightPx
                                    onDragEnd(finalSnappedX, finalSnappedY)
                                    rawOffsetX = 0f
                                    rawOffsetY = 0f
                                    isDragging = false
                                }
                            },
                            onDragCancel = {
                                rawOffsetX = 0f
                                rawOffsetY = 0f
                                isDragging = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                rawOffsetX += dragAmount.x
                                rawOffsetY += dragAmount.y
                            }
                        )
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.TopStart
    ) {
        Column(modifier = Modifier.padding(2.dp)) {
            Text(
                text = displayTimeRange,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (slot.assignedUserName != null) {
                Text(
                    text = slot.assignedUserName,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotDetailDialog(
    slot: SlotDTO,
    onDismiss: () -> Unit,
    onAssignUser: () -> Unit,
    onUnassignUser: () -> Unit,
    onDelete: () -> Unit,
    onLockSlot: () -> Unit,
    onUnlockSlot: () -> Unit,
    isProcessing: Boolean
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val date = LocalDate.parse(slot.date)
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${slot.startTime} - ${slot.endTime}")
                AssistChip(
                    onClick = {},
                    label = { Text(slot.status) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when (slot.status) {
                            "LOCKED" -> MaterialTheme.colorScheme.surfaceVariant
                            "UNLOCKED" -> Color(0xFF4CAF50).copy(alpha = 0.3f)
                            "BOOKED" -> MaterialTheme.colorScheme.primaryContainer
                            "BLOCKED" -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(date.format(dayFormatter))
                }

                HorizontalDivider()

                // Assigned user section
                Text(
                    text = stringResource(R.string.clients),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (slot.assignedUserName != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
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
                                    text = slot.assignedUserName,
                                    fontWeight = FontWeight.Medium
                                )
                                slot.assignedUserEmail?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = onUnassignUser,
                                enabled = !isProcessing
                            ) {
                                Icon(
                                    Icons.Default.PersonRemove,
                                    contentDescription = "Remove user",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onAssignUser,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.assign_user))
                    }
                }

                // Note section
                slot.note?.let { note ->
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.reason),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = note)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lock/Unlock icon button (left)
                if (slot.status != "BOOKED") {
                    if (slot.status.uppercase() == "LOCKED") {
                        IconButton(
                            onClick = onUnlockSlot,
                            enabled = !isProcessing
                        ) {
                            Icon(
                                Icons.Default.LockOpen,
                                contentDescription = stringResource(R.string.unlock_slot),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onLockSlot,
                            enabled = !isProcessing
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = stringResource(R.string.lock_slot),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Delete icon button (center)
                if (slot.status != "BOOKED") {
                    IconButton(
                        onClick = { showDeleteConfirm = true }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // OK button (right)
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.ok))
                }
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserSearchScreen(
    onBack: () -> Unit,
    onSelectUser: (ClientDTO) -> Unit,
    viewModel: AdminCalendarViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadClients()
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            viewModel.searchClients(searchQuery)
        } else if (searchQuery.isEmpty()) {
            viewModel.loadClients()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(stringResource(R.string.search_clients)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.search_clients)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                        }
                    }
                },
                singleLine = true
            )

            when {
                uiState.isLoadingClients -> LoadingContent()
                uiState.clients.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_clients_found),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.clients) { client ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectUser(client) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = client.fullName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = client.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("${client.creditBalance}") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
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

@Composable
private fun CreateSlotDialog(
    weekStart: LocalDate,
    prefilledDate: LocalDate? = null,
    prefilledTime: LocalTime? = null,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, String, String) -> Unit,
    onConfirmWithUser: (LocalDate, String, String) -> Unit
) {
    val date = prefilledDate ?: weekStart
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.getDefault())

    val startTime = prefilledTime ?: LocalTime.of(9, 0)
    val endTime = startTime.plusHours(1)

    var startTimeStr by remember { mutableStateOf("%02d:%02d".format(startTime.hour, startTime.minute)) }
    var endTimeStr by remember { mutableStateOf("%02d:%02d".format(endTime.hour, endTime.minute)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = date.format(dateFormatter),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // Time inputs in a card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )

                        OutlinedTextField(
                            value = startTimeStr,
                            onValueChange = { if (it.length <= 5) startTimeStr = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            ),
                            placeholder = { Text("09:00", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                        )

                        Text(
                            "–",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = endTimeStr,
                            onValueChange = { if (it.length <= 5) endTimeStr = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            ),
                            placeholder = { Text("10:00", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                        )
                    }
                }

                // Assign user button
                OutlinedButton(
                    onClick = {
                        onConfirmWithUser(date, startTimeStr, endTimeStr)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.assign_user),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(date, startTimeStr, endTimeStr) },
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
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
