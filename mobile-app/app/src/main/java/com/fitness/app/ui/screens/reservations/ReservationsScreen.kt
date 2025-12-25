package com.fitness.app.ui.screens.reservations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitness.app.R
import com.fitness.app.data.dto.AvailableSlotDTO
import com.fitness.app.data.dto.ReservationDTO
import com.fitness.app.ui.components.ErrorContent
import com.fitness.app.ui.components.LoadingContent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val START_HOUR = 6
private const val END_HOUR = 21
private const val HOUR_HEIGHT_DP = 48
private const val TIME_COLUMN_WIDTH_DP = 32

enum class CalendarViewMode(val days: Int, val label: String) {
    DAY(1, "1"),
    THREE_DAYS(3, "3"),
    WEEK(7, "7")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsScreen(
    viewModel: ReservationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var viewMode by remember { mutableStateOf(CalendarViewMode.THREE_DAYS) }

    LaunchedEffect(uiState.snackbarMessageResId) {
        uiState.snackbarMessageResId?.let {
            snackbarHostState.showSnackbar(".", duration = SnackbarDuration.Short)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) {
                    uiState.snackbarMessageResId?.let { messageResId ->
                        Snackbar(
                            containerColor = if (uiState.isSnackbarError)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (uiState.isSnackbarError)
                                MaterialTheme.colorScheme.onErrorContainer
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(stringResource(messageResId))
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Date navigation bar
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
                        message = uiState.error ?: "",
                        onRetry = { viewModel.loadData() }
                    )
                    else -> {
                        CalendarGridView(
                            startDate = uiState.selectedWeekStart,
                            visibleDays = viewMode.days,
                            availableSlots = uiState.availableSlots,
                            myReservations = uiState.myReservations,
                            onSlotClick = { slot -> viewModel.bookSlot(slot) },
                            onReservationClick = { reservation -> viewModel.cancelReservation(reservation) }
                        )
                    }
                }
            }
        }

        // Booking confirmation dialog
        if (uiState.showBookingDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissBookingDialog() },
                title = { Text(stringResource(R.string.book_training)) },
                text = { Text(stringResource(R.string.book_slot_confirm)) },
                confirmButton = {
                    Button(onClick = { viewModel.confirmBooking() }) {
                        Text(stringResource(R.string.book))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissBookingDialog() }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        // Cancel confirmation dialog
        if (uiState.showCancelDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissCancelDialog() },
                title = { Text(stringResource(R.string.cancel_reservation)) },
                text = { Text(stringResource(R.string.confirm_cancel)) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmCancel() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissCancelDialog() }) {
                        Text(stringResource(R.string.no))
                    }
                }
            )
        }
    }
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
            // Date navigation
            Row(verticalAlignment = Alignment.CenterVertically) {
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

            // View mode toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.height(36.dp)) {
                CalendarViewMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = viewMode == mode,
                        onClick = { onViewModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = CalendarViewMode.entries.size
                        ),
                        icon = {}
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
    availableSlots: List<AvailableSlotDTO>,
    myReservations: List<ReservationDTO>,
    onSlotClick: (AvailableSlotDTO) -> Unit,
    onReservationClick: (ReservationDTO) -> Unit
) {
    val days = (0 until visibleDays).map { startDate.plusDays(it.toLong()) }
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableWidth = maxWidth - TIME_COLUMN_WIDTH_DP.dp
        val columnWidth = availableWidth / visibleDays

        Column(modifier = Modifier.fillMaxSize()) {
            // Day headers
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.width(TIME_COLUMN_WIDTH_DP.dp))

                days.forEach { date ->
                    val isToday = date == LocalDate.now()
                    val dayName = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                    val dayNum = date.dayOfMonth

                    Box(
                        modifier = Modifier
                            .width(columnWidth)
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

            // Calendar grid
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Time column
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

                    // Day columns
                    days.forEachIndexed { dayIndex, date ->
                        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val daySlots = availableSlots.filter { it.date == dateStr }
                        val dayReservations = myReservations.filter { it.date == dateStr }

                        Box(
                            modifier = Modifier
                                .width(columnWidth)
                                .height(((END_HOUR - START_HOUR + 1) * HOUR_HEIGHT_DP).dp)
                        ) {
                            // Hour lines
                            Column {
                                (START_HOUR..END_HOUR).forEach { _ ->
                                    Box(
                                        modifier = Modifier
                                            .height(HOUR_HEIGHT_DP.dp)
                                            .fillMaxWidth()
                                            .border(
                                                width = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant
                                            )
                                    )
                                }
                            }

                            // Available slots (green - bookable)
                            daySlots.filter { it.isAvailable }.forEach { slot ->
                                val isPast = isSlotInPast(date, slot.startTime)
                                if (!isPast) {
                                    SlotBlock(
                                        startTime = slot.startTime,
                                        endTime = slot.endTime,
                                        color = Color(0xFF4CAF50), // Green for available
                                        text = "${slot.startTime.substring(0, 5)}",
                                        onClick = { onSlotClick(slot) }
                                    )
                                }
                            }

                            // My reservations (blue - cancellable)
                            dayReservations.forEach { reservation ->
                                val isPast = isSlotInPast(date, reservation.startTime)
                                SlotBlock(
                                    startTime = reservation.startTime,
                                    endTime = reservation.endTime,
                                    color = if (isPast) Color.Gray else MaterialTheme.colorScheme.primary,
                                    text = "${reservation.startTime.substring(0, 5)}",
                                    subText = stringResource(R.string.reserved),
                                    onClick = if (!isPast) {{ onReservationClick(reservation) }} else null
                                )
                            }

                            // Unavailable slots (grey - not bookable)
                            daySlots.filter { !it.isAvailable }.forEach { slot ->
                                // Check if this isn't already shown as my reservation
                                val isMyReservation = dayReservations.any {
                                    it.startTime == slot.startTime && it.date == slot.date
                                }
                                if (!isMyReservation) {
                                    SlotBlock(
                                        startTime = slot.startTime,
                                        endTime = slot.endTime,
                                        color = Color.Gray.copy(alpha = 0.5f),
                                        text = "${slot.startTime.substring(0, 5)}",
                                        onClick = null
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
private fun SlotBlock(
    startTime: String,
    endTime: String,
    color: Color,
    text: String,
    subText: String? = null,
    onClick: (() -> Unit)?
) {
    val startParts = startTime.split(":")
    val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
    val endParts = endTime.split(":")
    val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()

    val topOffset = ((startMinutes - START_HOUR * 60) * HOUR_HEIGHT_DP / 60f).dp
    val height = ((endMinutes - startMinutes) * HOUR_HEIGHT_DP / 60f).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp)
            .offset(y = topOffset)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(modifier = Modifier.padding(2.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subText != null) {
                Text(
                    text = subText,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun isSlotInPast(date: LocalDate, startTime: String): Boolean {
    val slotTime = LocalTime.parse(startTime)
    val slotDateTime = LocalDateTime.of(date, slotTime)
    return slotDateTime.isBefore(LocalDateTime.now().minusMinutes(15))
}
