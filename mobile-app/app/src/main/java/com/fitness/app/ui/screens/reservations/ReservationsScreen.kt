package com.fitness.app.ui.screens.reservations

import androidx.compose.foundation.clickable
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
import com.fitness.app.data.dto.AvailableSlotDTO
import com.fitness.app.data.dto.ReservationDTO
import com.fitness.app.ui.components.ErrorContent
import com.fitness.app.ui.components.LoadingContent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsScreen(
    viewModel: ReservationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when there's a message
    LaunchedEffect(uiState.snackbarMessageResId) {
        uiState.snackbarMessageResId?.let {
            snackbarHostState.showSnackbar(
                message = "",
                duration = SnackbarDuration.Short
            )
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AvailableSlotsTab(
                uiState = uiState,
                onDateSelected = { viewModel.selectDate(it) },
                onBookSlot = { viewModel.bookSlot(it) },
                onRetry = { viewModel.loadData() }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
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
                    Text(stringResource(R.string.cancel))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvailableSlotsTab(
    uiState: ReservationsUiState,
    onDateSelected: (LocalDate) -> Unit,
    onBookSlot: (AvailableSlotDTO) -> Unit,
    onRetry: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.selectedDate.toEpochDay() * 24 * 60 * 60 * 1000
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Date selector with navigation arrows inside card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous day button
                IconButton(onClick = { onDateSelected(uiState.selectedDate.minusDays(1)) }) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = stringResource(R.string.previous_week),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Vertical divider
                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Date section (clickable for date picker)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDatePicker = true }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.selected_date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                }

                // Vertical divider
                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Next day button
                IconButton(onClick = { onDateSelected(uiState.selectedDate.plusDays(1)) }) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = stringResource(R.string.next_week),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        when {
            uiState.isSlotsLoading -> LoadingContent()
            uiState.slotsErrorResId != null -> ErrorContent(
                message = stringResource(uiState.slotsErrorResId),
                onRetry = onRetry
            )
            uiState.availableSlots.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_slots_available),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.availableSlots) { slot ->
                        AvailableSlotItem(
                            slot = slot,
                            selectedDate = uiState.selectedDate,
                            onBook = { onBookSlot(slot) }
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                            onDateSelected(selectedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun AvailableSlotItem(
    slot: AvailableSlotDTO,
    selectedDate: LocalDate,
    onBook: () -> Unit
) {
    // Check if slot is in the past (more than 15 minutes ago)
    val slotStartTime = LocalTime.parse(slot.startTime)
    val slotDateTime = LocalDateTime.of(selectedDate, slotStartTime)
    val now = LocalDateTime.now()
    val isPast = slotDateTime.isBefore(now.minusMinutes(15))

    // Slot is bookable only if it's available AND not in the past
    val isBookable = slot.isAvailable && !isPast

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${slot.startTime} - ${slot.endTime}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isBookable)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when {
                        isPast -> stringResource(R.string.past)
                        !slot.isAvailable -> stringResource(R.string.reserved)
                        else -> stringResource(R.string.one_credit)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onBook,
                enabled = isBookable
            ) {
                Text(stringResource(R.string.book))
            }
        }
    }
}

@Composable
private fun MyReservationsTab(
    uiState: ReservationsUiState,
    onCancelReservation: (ReservationDTO) -> Unit,
    onRetry: () -> Unit
) {
    when {
        uiState.isReservationsLoading -> LoadingContent()
        uiState.reservationsErrorResId != null -> ErrorContent(
            message = stringResource(uiState.reservationsErrorResId),
            onRetry = onRetry
        )
        uiState.myReservations.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_upcoming),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.myReservations) { reservation ->
                    ReservationItem(
                        reservation = reservation,
                        onCancel = { onCancelReservation(reservation) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReservationItem(
    reservation: ReservationDTO,
    onCancel: () -> Unit
) {
    val date = LocalDate.parse(reservation.date)
    val time = LocalTime.parse(reservation.startTime)
    val startTime = LocalDateTime.of(date, time)
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = startTime.format(dateFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = startTime.format(timeFormatter),
                    style = MaterialTheme.typography.bodyLarge
                )
                AssistChip(
                    onClick = {},
                    label = { Text(reservation.status.lowercase().replaceFirstChar { it.uppercase() }) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when (reservation.status.uppercase()) {
                            "CONFIRMED" -> MaterialTheme.colorScheme.primaryContainer
                            "CANCELLED" -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                )
            }
            if (reservation.status.equals("CONFIRMED", ignoreCase = true)) {
                IconButton(onClick = onCancel) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
