package com.fitness.app.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitness.app.R
import com.fitness.app.data.dto.ClientDTO
import com.fitness.app.data.dto.ReservationDTO
import com.fitness.app.data.dto.SlotDTO
import com.fitness.app.ui.components.ErrorContent
import com.fitness.app.ui.components.LoadingContent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReservationsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminReservationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var selectedReservation by remember { mutableStateOf<ReservationDTO?>(null) }
    var showClientSearch by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_reservations)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_reservation))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Week navigation
                WeekNavigator(
                    selectedDate = uiState.selectedDate,
                    onDateChange = { viewModel.setSelectedDate(it) }
                )

                // Status filter
                StatusFilterRow(
                    selectedStatus = uiState.filterStatus,
                    onStatusSelected = { viewModel.setFilterStatus(it) }
                )

                // Reservations list
                when {
                    uiState.isLoading -> LoadingContent()
                    uiState.error != null -> ErrorContent(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadReservations() }
                    )
                    viewModel.getFilteredReservations().isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.EventBusy,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.no_reservations_found),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(viewModel.getFilteredReservations()) { reservation ->
                                ReservationCard(
                                    reservation = reservation,
                                    onCancelClick = {
                                        selectedReservation = reservation
                                        showCancelDialog = true
                                    },
                                    onNoteClick = {
                                        selectedReservation = reservation
                                        showNoteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Create reservation dialog/overlay
            AnimatedVisibility(
                visible = showCreateDialog,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it })
            ) {
                CreateReservationScreen(
                    viewModel = viewModel,
                    onDismiss = { showCreateDialog = false },
                    onClientSearchClick = { showClientSearch = true }
                )
            }

            // Client search overlay
            AnimatedVisibility(
                visible = showClientSearch,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it })
            ) {
                ClientSearchOverlay(
                    viewModel = viewModel,
                    onClientSelected = { client ->
                        showClientSearch = false
                    },
                    onDismiss = { showClientSearch = false }
                )
            }
        }

        // Cancel dialog
        if (showCancelDialog && selectedReservation != null) {
            CancelReservationDialog(
                reservation = selectedReservation!!,
                isCancelling = uiState.isCancelling,
                onDismiss = {
                    showCancelDialog = false
                    selectedReservation = null
                },
                onConfirm = { refundCredits ->
                    viewModel.cancelReservation(selectedReservation!!.id, refundCredits)
                    showCancelDialog = false
                    selectedReservation = null
                }
            )
        }

        // Note dialog
        if (showNoteDialog && selectedReservation != null) {
            EditNoteDialog(
                currentNote = selectedReservation!!.note,
                onDismiss = {
                    showNoteDialog = false
                    selectedReservation = null
                },
                onSave = { note ->
                    viewModel.updateNote(selectedReservation!!.id, note)
                    showNoteDialog = false
                    selectedReservation = null
                }
            )
        }
    }
}

@Composable
private fun WeekNavigator(
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    val weekStart = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
    val formatter = DateTimeFormatter.ofPattern("d MMM")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onDateChange(selectedDate.minusWeeks(1)) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.previous_week))
        }

        Text(
            text = "${weekStart.format(formatter)} - ${weekStart.plusDays(6).format(formatter)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        IconButton(onClick = { onDateChange(selectedDate.plusWeeks(1)) }) {
            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.next_week))
        }
    }

    // Day selector
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(7) { dayOffset ->
            val date = weekStart.plusDays(dayOffset.toLong())
            val isSelected = date == selectedDate

            FilterChip(
                selected = isSelected,
                onClick = { onDateChange(date) },
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun StatusFilterRow(
    selectedStatus: String?,
    onStatusSelected: (String?) -> Unit
) {
    val statuses = listOf(
        null to R.string.all,
        "confirmed" to R.string.confirmed,
        "cancelled" to R.string.cancelled
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(statuses) { (status, labelRes) ->
            FilterChip(
                selected = selectedStatus == status,
                onClick = { onStatusSelected(status) },
                label = { Text(stringResource(labelRes)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReservationCard(
    reservation: ReservationDTO,
    onCancelClick: () -> Unit,
    onNoteClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")
    val date = LocalDate.parse(reservation.date)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Client name
                    Text(
                        text = reservation.clientName ?: reservation.clientEmail ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Date and time
                    Text(
                        text = "${date.format(dateFormatter)} | ${reservation.startTime} - ${reservation.endTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Credits used
                    Text(
                        text = "${reservation.creditsUsed} credits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Status chip
                StatusChip(status = reservation.status)
            }

            // Note
            if (!reservation.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = reservation.note,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Actions
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onNoteClick) {
                    Icon(
                        Icons.Default.Note,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.note))
                }

                if (reservation.status == "confirmed") {
                    TextButton(
                        onClick = onCancelClick,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (backgroundColor, contentColor) = when (status) {
        "confirmed" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "cancelled" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateReservationScreen(
    viewModel: AdminReservationsViewModel,
    onDismiss: () -> Unit,
    onClientSearchClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedClient by remember { mutableStateOf<ClientDTO?>(null) }
    var selectedSlot by remember { mutableStateOf<SlotDTO?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var deductCredits by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDate) {
        viewModel.loadSlotsForDate(selectedDate)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.create_reservation)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Client selection
                Text(
                    text = stringResource(R.string.select_client),
                    style = MaterialTheme.typography.labelLarge
                )

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClientSearchClick() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedClient != null) {
                            Column {
                                Text(
                                    text = selectedClient!!.fullName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = selectedClient!!.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.tap_to_select_client),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }

                // Update selected client when clients list changes
                LaunchedEffect(uiState.clients) {
                    if (uiState.clients.size == 1 && selectedClient == null) {
                        // Auto-select if only one result
                    }
                }

                // Date selection
                Text(
                    text = stringResource(R.string.select_date),
                    style = MaterialTheme.typography.labelLarge
                )

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    }
                }

                // Slot selection
                Text(
                    text = stringResource(R.string.select_slot),
                    style = MaterialTheme.typography.labelLarge
                )

                if (uiState.slots.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_slots_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.slots) { slot ->
                            FilterChip(
                                selected = selectedSlot == slot,
                                onClick = { selectedSlot = slot },
                                label = { Text("${slot.startTime} - ${slot.endTime}") }
                            )
                        }
                    }
                }

                // Deduct credits toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.deduct_credits),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = deductCredits,
                        onCheckedChange = { deductCredits = it }
                    )
                }

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.note)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.weight(1f))

                // Create button
                Button(
                    onClick = {
                        if (selectedClient != null && selectedSlot != null) {
                            viewModel.createReservation(
                                userId = selectedClient!!.id,
                                date = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                startTime = selectedSlot!!.startTime,
                                endTime = selectedSlot!!.endTime,
                                blockId = selectedSlot!!.id,
                                deductCredits = deductCredits,
                                note = note.ifBlank { null }
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedClient != null && selectedSlot != null && !uiState.isCreating
                ) {
                    if (uiState.isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.create_reservation))
                    }
                }
            }
        }

        // Date picker dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate.toEpochDay() * 24 * 60 * 60 * 1000
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                        }
                        showDatePicker = false
                    }) {
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

    // Observe client selection from search
    LaunchedEffect(uiState.clients) {
        // When a single client is selected via search, update selectedClient
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientSearchOverlay(
    viewModel: AdminReservationsViewModel,
    onClientSelected: (ClientDTO) -> Unit,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.search_clients)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.clearClients()
                            onDismiss()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.length >= 2) {
                            viewModel.searchClients(it)
                        }
                    },
                    placeholder = { Text(stringResource(R.string.search_clients)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                viewModel.clearClients()
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                when {
                    uiState.isLoadingClients -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.clients.isEmpty() && searchQuery.length >= 2 -> {
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
                                    onClick = {
                                        onClientSelected(client)
                                    },
                                    modifier = Modifier.fillMaxWidth()
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
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = client.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${client.creditBalance} credits",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
}

@Composable
private fun CancelReservationDialog(
    reservation: ReservationDTO,
    isCancelling: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (refundCredits: Boolean) -> Unit
) {
    var refundCredits by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cancel_reservation)) },
        text = {
            Column {
                Text(stringResource(R.string.confirm_cancel_reservation))

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.refund_credits, reservation.creditsUsed),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = refundCredits,
                        onCheckedChange = { refundCredits = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(refundCredits) },
                enabled = !isCancelling,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isCancelling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                } else {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.back))
            }
        }
    )
}

@Composable
private fun EditNoteDialog(
    currentNote: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var note by remember { mutableStateOf(currentNote ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_note)) },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.note)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
        },
        confirmButton = {
            Button(onClick = { onSave(note.ifBlank { null }) }) {
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
