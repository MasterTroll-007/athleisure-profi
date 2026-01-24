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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

enum class ReservationFilter(val label: String) {
    ALL("All"),
    CONFIRMED("Confirmed"),
    CANCELLED("Cancelled")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReservationsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AdminReservationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var selectedReservation by remember { mutableStateOf<ReservationDTO?>(null) }
    var showClientSearch by remember { mutableStateOf(false) }
    var showSlotSelection by remember { mutableStateOf(false) }
    var showReservationDetail by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.admin_reservations)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            viewModel.loadClients()
                            viewModel.loadAvailableSlots()
                            showCreateDialog = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_reservation))
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
                    selectedDate = uiState.selectedDate,
                    onDateChange = { viewModel.setSelectedDate(it) }
                )

                // Search and filter bar
                SearchAndFilterBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    selectedFilter = uiState.filterStatus,
                    onFilterChange = { viewModel.setFilterStatus(it) }
                )

                // Reservations list
                when {
                    uiState.isLoading -> LoadingContent()
                    uiState.error != null -> ErrorContent(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadReservations() }
                    )
                    uiState.reservations.isEmpty() -> {
                        EmptyReservationsContent()
                    }
                    else -> {
                        ReservationsList(
                            reservations = uiState.reservations,
                            onReservationClick = { reservation ->
                                selectedReservation = reservation
                                showReservationDetail = true
                            }
                        )
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
                onClientSearchClick = { showClientSearch = true },
                onSlotSelectionClick = { showSlotSelection = true }
            )
        }

        // Client search overlay
        AnimatedVisibility(
            visible = showClientSearch,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            ClientSearchScreen(
                clients = uiState.clients,
                isLoading = uiState.isLoadingClients,
                onBack = { showClientSearch = false },
                onSelectClient = { client ->
                    viewModel.selectClient(client)
                    showClientSearch = false
                },
                onSearch = { query -> viewModel.searchClients(query) },
                onLoadClients = { viewModel.loadClients() }
            )
        }

        // Slot selection overlay
        AnimatedVisibility(
            visible = showSlotSelection,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            SlotSelectionScreen(
                slots = uiState.availableSlots,
                onBack = { showSlotSelection = false },
                onSelectSlot = { slot ->
                    viewModel.selectSlot(slot)
                    showSlotSelection = false
                }
            )
        }

        // Reservation detail dialog
        if (showReservationDetail && selectedReservation != null) {
            ReservationDetailDialog(
                reservation = selectedReservation!!,
                onDismiss = {
                    showReservationDetail = false
                    selectedReservation = null
                },
                onCancel = {
                    showReservationDetail = false
                    showCancelDialog = true
                },
                onEditNote = {
                    showReservationDetail = false
                    showNoteDialog = true
                }
            )
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
                    viewModel.updateReservationNote(selectedReservation!!.id, note ?: "")
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
private fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: String?,
    onFilterChange: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_clients)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                    }
                }
            },
            singleLine = true
        )

        // Filter chips
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ReservationFilter.entries.forEachIndexed { index, filter ->
                SegmentedButton(
                    selected = when (filter) {
                        ReservationFilter.ALL -> selectedFilter == null
                        ReservationFilter.CONFIRMED -> selectedFilter == "CONFIRMED"
                        ReservationFilter.CANCELLED -> selectedFilter == "CANCELLED"
                    },
                    onClick = {
                        onFilterChange(
                            when (filter) {
                                ReservationFilter.ALL -> null
                                ReservationFilter.CONFIRMED -> "CONFIRMED"
                                ReservationFilter.CANCELLED -> "CANCELLED"
                            }
                        )
                    },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ReservationFilter.entries.size
                    )
                ) {
                    Text(
                        text = when (filter) {
                            ReservationFilter.ALL -> stringResource(R.string.all)
                            ReservationFilter.CONFIRMED -> stringResource(R.string.reserved)
                            ReservationFilter.CANCELLED -> stringResource(R.string.cancelled)
                        },
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ReservationsList(
    reservations: List<ReservationDTO>,
    onReservationClick: (ReservationDTO) -> Unit
) {
    val groupedReservations = reservations.groupBy { it.date }
        .toSortedMap(compareByDescending { it })

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedReservations.forEach { (date, dayReservations) ->
            item {
                DateHeader(date = date)
            }

            items(dayReservations, key = { it.id }) { reservation ->
                ReservationCard(
                    reservation = reservation,
                    onClick = { onReservationClick(reservation) }
                )
            }
        }
    }
}

@Composable
private fun DateHeader(date: String) {
    val localDate = LocalDate.parse(date)
    val today = LocalDate.now()
    val displayText = when (localDate) {
        today -> stringResource(R.string.today)
        today.plusDays(1) -> stringResource(R.string.tomorrow)
        else -> localDate.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.getDefault()))
    }

    Text(
        text = displayText,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ReservationCard(
    reservation: ReservationDTO,
    onClick: () -> Unit
) {
    val isCancelled = reservation.status.equals("CANCELLED", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCancelled)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = reservation.clientName?.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reservation.clientName ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${reservation.startTime.take(5)} - ${reservation.endTime.take(5)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    reservation.note?.let { note ->
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Status badge
            ReservationStatusBadge(status = reservation.status)
        }
    }
}

@Composable
private fun ReservationStatusBadge(status: String) {
    val (backgroundColor, textColor, text) = when (status.uppercase()) {
        "CONFIRMED", "BOOKED" -> Triple(
            Color(0xFF22C55E).copy(alpha = 0.1f),
            Color(0xFF22C55E),
            stringResource(R.string.reserved)
        )
        "CANCELLED" -> Triple(
            Color(0xFFEF4444).copy(alpha = 0.1f),
            Color(0xFFEF4444),
            stringResource(R.string.cancelled)
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            status
        )
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyReservationsContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.EventBusy,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_reservations),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateReservationScreen(
    viewModel: AdminReservationsViewModel,
    onDismiss: () -> Unit,
    onClientSearchClick: () -> Unit,
    onSlotSelectionClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var deductCredits by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }

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
                        if (uiState.selectedClient != null) {
                            Column {
                                Text(
                                    text = uiState.selectedClient!!.fullName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = uiState.selectedClient!!.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${uiState.selectedClient!!.creditBalance} credits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
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

                // Slot selection
                Text(
                    text = stringResource(R.string.select_slot),
                    style = MaterialTheme.typography.labelLarge
                )

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSlotSelectionClick() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.selectedSlot != null) {
                            Column {
                                Text(
                                    text = formatSlotDate(uiState.selectedSlot!!.date),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "${uiState.selectedSlot!!.startTime} - ${uiState.selectedSlot!!.endTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.no_slots_available),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
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
                        val client = uiState.selectedClient
                        val slot = uiState.selectedSlot
                        if (client != null && slot != null) {
                            viewModel.createReservation(
                                clientId = client.id,
                                slot = slot,
                                deductCredits = deductCredits,
                                note = note.ifBlank { null }
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.selectedClient != null && uiState.selectedSlot != null && !uiState.isCreating
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientSearchScreen(
    clients: List<ClientDTO>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSelectClient: (ClientDTO) -> Unit,
    onSearch: (String) -> Unit,
    onLoadClients: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        onLoadClients()
    }

    LaunchedEffect(searchQuery) {
        onSearch(searchQuery)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(stringResource(R.string.select_client)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )

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
                isLoading -> LoadingContent()
                clients.isEmpty() -> {
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
                        items(clients) { client ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectClient(client) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotSelectionScreen(
    slots: List<SlotDTO>,
    onBack: () -> Unit,
    onSelectSlot: (SlotDTO) -> Unit
) {
    val groupedSlots = slots.groupBy { it.date }
        .toSortedMap()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(stringResource(R.string.select_slot)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )

            if (slots.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.no_slots_available),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedSlots.forEach { (date, daySlots) ->
                        item {
                            Text(
                                text = formatSlotDate(date),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(daySlots.sortedBy { it.startTime }) { slot ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectSlot(slot) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "${slot.startTime} - ${slot.endTime}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = "${slot.durationMinutes} min",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
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
private fun ReservationDetailDialog(
    reservation: ReservationDTO,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onEditNote: () -> Unit
) {
    val isCancelled = reservation.status.equals("CANCELLED", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.reservation_details))
                ReservationStatusBadge(status = reservation.status)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Client info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = reservation.clientName ?: "Unknown",
                            fontWeight = FontWeight.Medium
                        )
                        reservation.clientEmail?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Date and time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = formatSlotDate(reservation.date))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "${reservation.startTime.take(5)} - ${reservation.endTime.take(5)}")
                }

                // Credits used
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "${reservation.creditsUsed} credits")
                }

                // Note
                reservation.note?.let { note ->
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Notes,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = note)
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onEditNote) {
                    Text(stringResource(R.string.edit_note))
                }
                if (!isCancelled) {
                    TextButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.cancel_reservation))
                    }
                }
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.ok))
                }
            }
        }
    )
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.confirm_cancel))

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
                    Text(stringResource(R.string.yes))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.no))
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

private fun formatSlotDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        val today = LocalDate.now()
        when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            else -> date.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.getDefault()))
        }
    } catch (e: Exception) {
        dateString
    }
}
