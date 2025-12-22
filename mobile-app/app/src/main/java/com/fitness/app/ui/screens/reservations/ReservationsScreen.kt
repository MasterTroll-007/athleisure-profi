package com.fitness.app.ui.screens.reservations

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
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsScreen(
    viewModel: ReservationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(R.string.available_slots, R.string.my_reservations)

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reservations)) }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, titleRes ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(stringResource(titleRes)) }
                    )
                }
            }

            when (selectedTab) {
                0 -> AvailableSlotsTab(
                    uiState = uiState,
                    onDateSelected = { viewModel.selectDate(it) },
                    onBookSlot = { viewModel.bookSlot(it) },
                    onRetry = { viewModel.loadData() }
                )
                1 -> MyReservationsTab(
                    uiState = uiState,
                    onCancelReservation = { viewModel.cancelReservation(it) },
                    onRetry = { viewModel.loadMyReservations() }
                )
            }
        }
    }

    // Booking confirmation dialog
    if (uiState.showBookingDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBookingDialog() },
            title = { Text(stringResource(R.string.book_training)) },
            text = { Text("Book this slot for 1 credit?") },
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
        // Date selector
        Card(
            onClick = { showDatePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                        text = "Selected Date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
            }
        }

        when {
            uiState.isSlotsLoading -> LoadingContent()
            uiState.slotsError != null -> ErrorContent(
                message = uiState.slotsError,
                onRetry = onRetry
            )
            uiState.availableSlots.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No available slots for this date",
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
    onBook: () -> Unit
) {
    val startTime = LocalDateTime.parse(slot.startTime.removeSuffix("Z"))
    val endTime = LocalDateTime.parse(slot.endTime.removeSuffix("Z"))
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
                    text = "${startTime.format(timeFormatter)} - ${endTime.format(timeFormatter)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "1 credit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onBook) {
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
        uiState.reservationsError != null -> ErrorContent(
            message = uiState.reservationsError,
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
    val startTime = LocalDateTime.parse(reservation.startTime.removeSuffix("Z"))
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
                    label = { Text(reservation.status) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = when (reservation.status) {
                            "CONFIRMED" -> MaterialTheme.colorScheme.primaryContainer
                            "CANCELLED" -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                )
            }
            if (reservation.status == "CONFIRMED") {
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
