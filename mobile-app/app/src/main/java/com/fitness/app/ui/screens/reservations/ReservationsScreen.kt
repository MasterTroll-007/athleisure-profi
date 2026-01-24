package com.fitness.app.ui.screens.reservations

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitness.app.R
import com.fitness.app.data.dto.AvailableSlotDTO
import com.fitness.app.data.dto.ClientDTO
import com.fitness.app.data.dto.ReservationDTO
import com.fitness.app.data.dto.SlotDTO
import com.fitness.app.ui.components.ErrorContent
import com.fitness.app.ui.components.LoadingContent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

private const val START_HOUR = 6
private const val END_HOUR = 21
private const val HOUR_HEIGHT_DP = 72
private const val TIME_COLUMN_WIDTH_DP = 32
private const val DAY_COLUMN_WIDTH_DP = 100

enum class CalendarViewMode(val days: Int, val label: String) {
    DAY(1, "1"),
    THREE_DAYS(3, "3"),
    WEEK(5, "5"),
    MONTH(0, "M")  // 0 days indicates month view
}

// Data class for simplified slot info in monthly view
data class MonthSlotInfo(
    val time: String,        // "14:00"
    val label: String?,      // Optional: "J. Novák" for admin
    val isReserved: Boolean,
    val isLocked: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsScreen(
    viewModel: ReservationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var viewMode by remember { mutableStateOf(CalendarViewMode.THREE_DAYS) }
    var selectedDateForDayView by remember { mutableStateOf<LocalDate?>(null) }
    var displayedMonth by remember { mutableStateOf(java.time.YearMonth.now()) }

    // Infinite scroll state
    val lazyListState = rememberLazyListState()
    var visibleMonthYear by remember { mutableStateOf("") }

    // Track visible date range and update header
    LaunchedEffect(lazyListState.layoutInfo.visibleItemsInfo, uiState.loadedDays) {
        if (uiState.loadedDays.isNotEmpty() && lazyListState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
            val firstVisibleIndex = lazyListState.firstVisibleItemIndex
            if (firstVisibleIndex in uiState.loadedDays.indices) {
                val firstVisibleDate = uiState.loadedDays[firstVisibleIndex]
                val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())
                visibleMonthYear = firstVisibleDate.format(monthFormatter).replaceFirstChar { it.uppercase() }
            }
        }
    }

    // Edge detection for loading more data
    LaunchedEffect(lazyListState.layoutInfo) {
        val firstVisible = lazyListState.firstVisibleItemIndex
        val lastVisible = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index

        if (firstVisible <= 2 && !uiState.isLoadingPast) {
            viewModel.loadMorePast()
        }
        if (lastVisible != null && lastVisible >= uiState.loadedDays.size - 3 && !uiState.isLoadingFuture) {
            viewModel.loadMoreFuture()
        }
    }

    // Track if initial scroll has been done
    var hasInitialScrolled by remember { mutableStateOf(false) }

    // Scroll to today on initial load (only once)
    LaunchedEffect(uiState.loadedDays) {
        if (uiState.loadedDays.isNotEmpty() && !hasInitialScrolled) {
            val todayIndex = uiState.loadedDays.indexOfFirst { it == LocalDate.now() }
            if (todayIndex >= 0) {
                lazyListState.scrollToItem(todayIndex)
                hasInitialScrolled = true
            }
        }
    }

    // Scroll to selected date when switching from month view
    LaunchedEffect(selectedDateForDayView) {
        selectedDateForDayView?.let { targetDate ->
            // Wait for the calendar to be ready and scroll
            snapshotFlow { lazyListState.layoutInfo.totalItemsCount }
                .first { it > 0 }

            val dateIndex = uiState.loadedDays.indexOf(targetDate)
            if (dateIndex >= 0) {
                // Additional delay for AnimatedContent to complete
                kotlinx.coroutines.delay(450)
                lazyListState.scrollToItem(dateIndex)
            }
            selectedDateForDayView = null
        }
    }

    // Admin-specific state
    var isDragEnabled by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createDialogPrefilledDate by remember { mutableStateOf<LocalDate?>(null) }
    var createDialogPrefilledTime by remember { mutableStateOf<LocalTime?>(null) }
    var showSlotDetail by remember { mutableStateOf(false) }
    var showUserSearch by remember { mutableStateOf(false) }
    var showUserSearchForCreate by remember { mutableStateOf(false) }
    var showDragConfirm by remember { mutableStateOf(false) }
    var pendingDragMove by remember { mutableStateOf<DragMoveData?>(null) }
    var pendingCreateSlotData by remember { mutableStateOf<CreateSlotData?>(null) }

    // Handle snackbar messages
    LaunchedEffect(uiState.snackbarMessageResId, uiState.snackbarMessage) {
        uiState.snackbarMessageResId?.let {
            snackbarHostState.showSnackbar(".", duration = SnackbarDuration.Short)
            viewModel.clearSnackbar()
        }
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadInitialRange()
    }

    // Reload data when isAdmin changes
    LaunchedEffect(uiState.isAdmin) {
        viewModel.loadInitialRange()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                if (uiState.isAdmin) {
                    FloatingActionButton(
                        onClick = {
                            createDialogPrefilledDate = null
                            createDialogPrefilledTime = null
                            showCreateDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_slot))
                    }
                }
            },
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
                    } ?: uiState.snackbarMessage?.let { message ->
                        Snackbar(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(message)
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
                    monthYear = visibleMonthYear,
                    viewMode = viewMode,
                    onViewModeChange = { newMode -> viewMode = newMode },
                    isAdmin = uiState.isAdmin,
                    isDragEnabled = isDragEnabled,
                    onDragToggle = { isDragEnabled = !isDragEnabled },
                    onUnlockWeek = { viewModel.unlockWeek() },
                    isUnlocking = uiState.isUnlocking
                )

                when {
                    uiState.isLoading -> LoadingContent()
                    uiState.error != null -> ErrorContent(
                        message = uiState.error ?: "",
                        onRetry = { viewModel.loadInitialRange() }
                    )
                    else -> {
                        AnimatedContent(
                            targetState = viewMode,
                            transitionSpec = {
                                if (targetState == CalendarViewMode.MONTH) {
                                    // Zooming out to month view
                                    (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 1.2f, animationSpec = tween(300)))
                                        .togetherWith(fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300)))
                                } else if (initialState == CalendarViewMode.MONTH) {
                                    // Zooming in from month view
                                    (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f, animationSpec = tween(300)))
                                        .togetherWith(fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 1.2f, animationSpec = tween(300)))
                                } else {
                                    fadeIn(animationSpec = tween(200)).togetherWith(fadeOut(animationSpec = tween(200)))
                                }
                            },
                            label = "calendar_transition"
                        ) { targetViewMode ->
                            when {
                                targetViewMode == CalendarViewMode.MONTH -> {
                                    // Convert slot data to simplified format for monthly view
                                    val monthSlots: Map<LocalDate, List<MonthSlotInfo>> = if (uiState.isAdmin) {
                                        uiState.adminSlotsByDate.mapValues { (_, slots) ->
                                            slots.map { slot ->
                                                MonthSlotInfo(
                                                    time = slot.startTime.take(5),
                                                    label = slot.assignedUserName?.let { name ->
                                                        val parts = name.split(" ")
                                                        if (parts.size >= 2) "${parts[0].first()}. ${parts.last()}"
                                                        else name
                                                    },
                                                    isReserved = slot.assignedUserId != null,
                                                    isLocked = slot.status == "LOCKED"
                                                )
                                            }.sortedBy { it.time }
                                        }
                                    } else {
                                        uiState.userSlotsByDate.mapValues { (_, slots) ->
                                            slots.map { slot ->
                                                MonthSlotInfo(
                                                    time = slot.startTime.take(5),
                                                    label = null,
                                                    isReserved = !slot.isAvailable,
                                                    isLocked = false
                                                )
                                            }.sortedBy { it.time }
                                        }
                                    }

                                    MonthlyCalendarView(
                                        yearMonth = displayedMonth,
                                        slotsByDate = monthSlots,
                                        onDayClick = { date ->
                                            selectedDateForDayView = date
                                            // Scroll to the selected date in the lazy list
                                            val dateIndex = uiState.loadedDays.indexOf(date)
                                            if (dateIndex >= 0) {
                                                viewMode = CalendarViewMode.DAY
                                            }
                                        },
                                        onMonthChange = { newMonth ->
                                            displayedMonth = newMonth
                                        }
                                    )
                                }
                                uiState.isAdmin -> {
                                    // Admin calendar view with infinite scroll
                                    InfiniteScrollAdminCalendar(
                                        loadedDays = uiState.loadedDays,
                                        slotsByDate = uiState.adminSlotsByDate,
                                        lazyListState = lazyListState,
                                        viewMode = targetViewMode,
                                        isDragEnabled = isDragEnabled,
                                        isLoadingPast = uiState.isLoadingPast,
                                        isLoadingFuture = uiState.isLoadingFuture,
                                        onSlotClick = { slot ->
                                            viewModel.selectAdminSlot(slot)
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
                                else -> {
                                    // User calendar view with infinite scroll
                                    InfiniteScrollUserCalendar(
                                        loadedDays = uiState.loadedDays,
                                        slotsByDate = uiState.userSlotsByDate,
                                        reservationsByDate = uiState.reservationsByDate,
                                        lazyListState = lazyListState,
                                        viewMode = targetViewMode,
                                        isLoadingPast = uiState.isLoadingPast,
                                        isLoadingFuture = uiState.isLoadingFuture,
                                        onSlotClick = { slot -> viewModel.bookSlot(slot) },
                                        onReservationClick = { reservation -> viewModel.cancelReservation(reservation) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ===== ADMIN DIALOGS =====

        // Fullscreen User Search (for existing slot)
        AnimatedVisibility(
            visible = showUserSearch,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            UserSearchScreen(
                clients = uiState.clients,
                isLoading = uiState.isLoadingClients,
                onBack = { showUserSearch = false },
                onSelectUser = { client ->
                    uiState.selectedAdminSlot?.let { slot ->
                        viewModel.assignUser(slot.id, client.id)
                    }
                    showUserSearch = false
                    showSlotDetail = false
                    viewModel.clearSelectedAdminSlot()
                },
                onSearch = { query ->
                    if (query.length >= 2) {
                        viewModel.searchClients(query)
                    } else if (query.isEmpty()) {
                        viewModel.loadClients()
                    }
                },
                onLoadClients = { viewModel.loadClients() }
            )
        }

        // Fullscreen User Search (for new slot creation)
        AnimatedVisibility(
            visible = showUserSearchForCreate,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            UserSearchScreen(
                clients = uiState.clients,
                isLoading = uiState.isLoadingClients,
                onBack = { showUserSearchForCreate = false },
                onSelectUser = { client ->
                    pendingCreateSlotData?.let { data ->
                        viewModel.createSlotWithUser(data.date, data.startTime, data.endTime, client.id)
                    }
                    showUserSearchForCreate = false
                    showCreateDialog = false
                    pendingCreateSlotData = null
                },
                onSearch = { query ->
                    if (query.length >= 2) {
                        viewModel.searchClients(query)
                    } else if (query.isEmpty()) {
                        viewModel.loadClients()
                    }
                },
                onLoadClients = { viewModel.loadClients() }
            )
        }

        // Slot Detail Dialog (admin)
        if (showSlotDetail && uiState.selectedAdminSlot != null && !showUserSearch) {
            SlotDetailDialog(
                slot = uiState.selectedAdminSlot!!,
                onDismiss = {
                    showSlotDetail = false
                    viewModel.clearSelectedAdminSlot()
                },
                onAssignUser = {
                    showUserSearch = true
                },
                onUnassignUser = {
                    viewModel.unassignUser(uiState.selectedAdminSlot!!.id)
                    showSlotDetail = false
                    viewModel.clearSelectedAdminSlot()
                },
                onDelete = {
                    viewModel.deleteSlot(uiState.selectedAdminSlot!!.id)
                    showSlotDetail = false
                    viewModel.clearSelectedAdminSlot()
                },
                onLockSlot = {
                    viewModel.lockSlot(uiState.selectedAdminSlot!!.id)
                    showSlotDetail = false
                    viewModel.clearSelectedAdminSlot()
                },
                onUnlockSlot = {
                    viewModel.unlockSlot(uiState.selectedAdminSlot!!.id)
                    showSlotDetail = false
                    viewModel.clearSelectedAdminSlot()
                },
                isProcessing = uiState.isProcessing
            )
        }

        // Create slot dialog (admin)
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

        // Drag confirmation dialog (admin)
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

        // ===== USER DIALOGS =====

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

// ===== DATA CLASSES =====

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

// ===== SHARED COMPONENTS =====

@Composable
private fun DateNavigationBar(
    monthYear: String,
    viewMode: CalendarViewMode,
    onViewModeChange: (CalendarViewMode) -> Unit,
    // Admin-specific props
    isAdmin: Boolean = false,
    isDragEnabled: Boolean = false,
    onDragToggle: () -> Unit = {},
    onUnlockWeek: () -> Unit = {},
    isUnlocking: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Month/Year display
            Text(
                text = monthYear,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Menu with view mode and admin actions
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        modifier = Modifier.size(24.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // View mode options
                    Text(
                        text = stringResource(R.string.view),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    CalendarViewMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (mode) {
                                        CalendarViewMode.DAY -> stringResource(R.string.view_day)
                                        CalendarViewMode.THREE_DAYS -> stringResource(R.string.view_three_days)
                                        CalendarViewMode.WEEK -> stringResource(R.string.view_week)
                                        CalendarViewMode.MONTH -> stringResource(R.string.view_month)
                                    }
                                )
                            },
                            onClick = {
                                onViewModeChange(mode)
                                showMenu = false
                            },
                            leadingIcon = {
                                if (viewMode == mode) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }

                    // Admin options
                    if (isAdmin) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Drag mode toggle
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isDragEnabled) stringResource(R.string.lock_drag)
                                    else stringResource(R.string.unlock_drag)
                                )
                            },
                            onClick = {
                                onDragToggle()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isDragEnabled) Icons.Default.Lock else Icons.Default.OpenWith,
                                    contentDescription = null
                                )
                            }
                        )

                        // Unlock week
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.unlock_week)) },
                            onClick = {
                                onUnlockWeek()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null) },
                            enabled = !isUnlocking
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeaders(
    days: List<LocalDate>,
    columnWidth: androidx.compose.ui.unit.Dp
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.width(TIME_COLUMN_WIDTH_DP.dp))

        days.forEach { date ->
            val isToday = date == LocalDate.now()
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

            Box(
                modifier = Modifier
                    .width(columnWidth)
                    .background(
                        if (isToday) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                // Compact format: "8.1. Pá"
                Text(
                    text = "${date.dayOfMonth}.${date.monthValue}. $dayName",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ===== INFINITE SCROLL USER CALENDAR =====

@Composable
private fun InfiniteScrollUserCalendar(
    loadedDays: List<LocalDate>,
    slotsByDate: Map<LocalDate, List<AvailableSlotDTO>>,
    reservationsByDate: Map<LocalDate, List<ReservationDTO>>,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    viewMode: CalendarViewMode,
    isLoadingPast: Boolean,
    isLoadingFuture: Boolean,
    onSlotClick: (AvailableSlotDTO) -> Unit,
    onReservationClick: (ReservationDTO) -> Unit
) {
    val verticalScrollState = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Calculate dynamic column width based on view mode
        val availableWidth = maxWidth - TIME_COLUMN_WIDTH_DP.dp
        val columnWidth = if (viewMode.days > 0) {
            availableWidth / viewMode.days
        } else {
            DAY_COLUMN_WIDTH_DP.dp // Fallback for month view
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Day headers row
            Row(modifier = Modifier.fillMaxWidth()) {
                // Empty space for time column
                Box(modifier = Modifier.width(TIME_COLUMN_WIDTH_DP.dp))

                // Scrollable day headers with snap behavior
                LazyRow(
                    state = lazyListState,
                    modifier = Modifier.fillMaxWidth(),
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)
                ) {
                    items(loadedDays.size, key = { loadedDays[it].toString() }) { index ->
                        val date = loadedDays[index]
                        val isToday = date == LocalDate.now()
                        val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

                        Box(
                            modifier = Modifier
                                .width(columnWidth)
                                .background(
                                    if (isToday) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${date.dayOfMonth}.${date.monthValue}. $dayName",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
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
                // Fixed time column
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

                // Scrollable day columns with snap behavior
                LazyRow(
                    state = lazyListState,
                    modifier = Modifier.fillMaxWidth(),
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)
                ) {
                    items(loadedDays.size, key = { loadedDays[it].toString() }) { index ->
                        val date = loadedDays[index]
                        val daySlots = slotsByDate[date] ?: emptyList()
                        val dayReservations = reservationsByDate[date] ?: emptyList()

                        UserDayColumn(
                            date = date,
                            columnWidth = columnWidth,
                            slots = daySlots,
                            reservations = dayReservations,
                            onSlotClick = onSlotClick,
                            onReservationClick = onReservationClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserDayColumn(
    date: LocalDate,
    columnWidth: androidx.compose.ui.unit.Dp,
    slots: List<AvailableSlotDTO>,
    reservations: List<ReservationDTO>,
    onSlotClick: (AvailableSlotDTO) -> Unit,
    onReservationClick: (ReservationDTO) -> Unit
) {
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

        // Available slots (teal - bookable)
        slots.filter { it.isAvailable }.forEach { slot ->
            val isPast = isSlotInPast(date, slot.startTime)
            if (!isPast) {
                UserSlotBlock(
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    color = Color(0xFF26A69A).copy(alpha = 0.4f),
                    text = slot.startTime.substring(0, 5),
                    onClick = { onSlotClick(slot) }
                )
            }
        }

        // My reservations (blue - cancellable)
        reservations.forEach { reservation ->
            val isPast = isSlotInPast(date, reservation.startTime)
            UserSlotBlock(
                startTime = reservation.startTime,
                endTime = reservation.endTime,
                color = if (isPast) Color.Gray else MaterialTheme.colorScheme.primary,
                text = reservation.startTime.substring(0, 5),
                subText = stringResource(R.string.reserved),
                onClick = if (!isPast) {{ onReservationClick(reservation) }} else null
            )
        }

        // Unavailable slots (grey - not bookable)
        slots.filter { !it.isAvailable }.forEach { slot ->
            val isMyReservation = reservations.any {
                it.startTime == slot.startTime && it.date == slot.date
            }
            if (!isMyReservation) {
                UserSlotBlock(
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    color = Color.Gray.copy(alpha = 0.5f),
                    text = slot.startTime.substring(0, 5),
                    onClick = null
                )
            }
        }
    }
}

@Composable
private fun UserSlotBlock(
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
            .border(1.dp, Color.White, RoundedCornerShape(4.dp))
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

// ===== INFINITE SCROLL ADMIN CALENDAR =====

@Composable
private fun InfiniteScrollAdminCalendar(
    loadedDays: List<LocalDate>,
    slotsByDate: Map<LocalDate, List<SlotDTO>>,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    viewMode: CalendarViewMode,
    isDragEnabled: Boolean,
    isLoadingPast: Boolean,
    isLoadingFuture: Boolean,
    onSlotClick: (SlotDTO) -> Unit,
    onSlotDrag: (SlotDTO, LocalDate, String) -> Unit,
    onEmptyClick: (LocalDate, LocalTime) -> Unit
) {
    val density = LocalDensity.current
    val hourHeightPx = with(density) { HOUR_HEIGHT_DP.dp.toPx() }
    val verticalScrollState = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Calculate dynamic column width based on view mode
        val availableWidth = maxWidth - TIME_COLUMN_WIDTH_DP.dp
        val columnWidth = if (viewMode.days > 0) {
            availableWidth / viewMode.days
        } else {
            DAY_COLUMN_WIDTH_DP.dp // Fallback for month view
        }
        val columnWidthPx = with(density) { columnWidth.toPx() }

        Column(modifier = Modifier.fillMaxSize()) {
            // Day headers row
            Row(modifier = Modifier.fillMaxWidth()) {
                // Empty space for time column
                Box(modifier = Modifier.width(TIME_COLUMN_WIDTH_DP.dp))

                // Scrollable day headers with snap behavior
                LazyRow(
                    state = lazyListState,
                    modifier = Modifier.fillMaxWidth(),
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)
                ) {
                    items(loadedDays.size, key = { loadedDays[it].toString() }) { index ->
                        val date = loadedDays[index]
                        val isToday = date == LocalDate.now()
                        val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

                        Box(
                            modifier = Modifier
                                .width(columnWidth)
                                .background(
                                    if (isToday) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${date.dayOfMonth}.${date.monthValue}. $dayName",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
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
                // Fixed time column
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

                // Scrollable day columns with snap behavior
                LazyRow(
                    state = lazyListState,
                    modifier = Modifier.fillMaxWidth(),
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)
                ) {
                    items(loadedDays.size, key = { loadedDays[it].toString() }) { index ->
                        val date = loadedDays[index]
                        val daySlots = slotsByDate[date] ?: emptyList()

                        AdminDayColumn(
                            date = date,
                            dayIndex = index,
                            loadedDays = loadedDays,
                            columnWidth = columnWidth,
                            columnWidthPx = columnWidthPx,
                            hourHeightPx = hourHeightPx,
                            slots = daySlots,
                            isDragEnabled = isDragEnabled,
                            onSlotClick = onSlotClick,
                            onSlotDrag = onSlotDrag,
                            onEmptyClick = onEmptyClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminDayColumn(
    date: LocalDate,
    dayIndex: Int,
    loadedDays: List<LocalDate>,
    columnWidth: androidx.compose.ui.unit.Dp,
    columnWidthPx: Float,
    hourHeightPx: Float,
    slots: List<SlotDTO>,
    isDragEnabled: Boolean,
    onSlotClick: (SlotDTO) -> Unit,
    onSlotDrag: (SlotDTO, LocalDate, String) -> Unit,
    onEmptyClick: (LocalDate, LocalTime) -> Unit
) {
    Box(
        modifier = Modifier
            .width(columnWidth)
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
        slots.forEach { slot ->
            AdminSlotBlock(
                slot = slot,
                hourHeightPx = hourHeightPx,
                columnWidthPx = columnWidthPx,
                isDragEnabled = isDragEnabled,
                onClick = { onSlotClick(slot) },
                onDragEnd = { newDayOffset, newTimeOffsetPx ->
                    // Calculate new day
                    val dayChange = (newDayOffset / columnWidthPx).roundToInt()
                    val newDayIndex = (dayIndex + dayChange).coerceIn(0, loadedDays.size - 1)
                    val newDate = loadedDays[newDayIndex]

                    // Calculate new time (snap to 15 min)
                    val slotStartTime = LocalTime.parse(slot.startTime)
                    val startMinutes = slotStartTime.hour * 60 + slotStartTime.minute - START_HOUR * 60
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

@Composable
private fun AdminSlotBlock(
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

    // Colors: locked=grey, unlocked=green, cancelled=red
    val statusColor = when (slot.status.lowercase()) {
        "locked" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        "unlocked" -> Color(0xFF26A69A).copy(alpha = 0.4f) // Teal 400
        "booked", "reserved" -> MaterialTheme.colorScheme.primaryContainer
        "cancelled" -> Color(0xFFEF5350).copy(alpha = 0.4f) // Red 400
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
                val nameLines = slot.assignedUserName.split("\n")
                nameLines.forEach { line ->
                    Text(
                        text = line,
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
}

// ===== ADMIN DIALOGS =====

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
                        containerColor = when (slot.status.uppercase()) {
                            "LOCKED" -> MaterialTheme.colorScheme.surfaceVariant
                            "UNLOCKED" -> Color(0xFF4CAF50).copy(alpha = 0.3f)
                            "BOOKED", "RESERVED" -> MaterialTheme.colorScheme.primaryContainer
                            "CANCELLED" -> Color(0xFFEF5350).copy(alpha = 0.3f)
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
                    val isCancelled = slot.status.uppercase() == "CANCELLED"
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCancelled)
                                Color(0xFFEF5350).copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.secondaryContainer
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
                                val nameLines = slot.assignedUserName.split("\n")
                                nameLines.forEach { line ->
                                    Text(
                                        text = line,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                slot.assignedUserEmail?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isCancelled && slot.cancelledAt != null) {
                                    Text(
                                        text = "Zruseno: ${slot.cancelledAt.replace("T", " ").take(16)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFEF5350)
                                    )
                                }
                            }
                            if (!isCancelled) {
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
                // Lock/Unlock icon button
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

                // Delete icon button
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

                // OK button
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
    clients: List<ClientDTO>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSelectUser: (ClientDTO) -> Unit,
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

/**
 * Validates time string format (HH:MM) and returns minutes since midnight, or null if invalid.
 * Valid range: 00:00-23:59
 */
private fun parseTimeToMinutes(time: String): Int? {
    if (!time.matches(Regex("^\\d{2}:\\d{2}$"))) return null
    val parts = time.split(":")
    val hours = parts[0].toIntOrNull() ?: return null
    val minutes = parts[1].toIntOrNull() ?: return null
    if (hours !in 0..23 || minutes !in 0..59) return null
    return hours * 60 + minutes
}

/**
 * Validates that time format is correct and end time is after start time.
 * Returns error string resource ID or null if valid.
 */
private fun validateTimeRange(startTime: String, endTime: String): Int? {
    val startMinutes = parseTimeToMinutes(startTime)
    val endMinutes = parseTimeToMinutes(endTime)

    if (startMinutes == null) return R.string.error_invalid_start_time
    if (endMinutes == null) return R.string.error_invalid_end_time
    if (endMinutes <= startMinutes) return R.string.error_end_before_start

    return null
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
    var validationError by remember { mutableStateOf<Int?>(null) }

    // Validate on every change
    LaunchedEffect(startTimeStr, endTimeStr) {
        validationError = validateTimeRange(startTimeStr, endTimeStr)
    }

    val isValid = validationError == null

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
                            placeholder = { Text("09:00", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                            isError = validationError == R.string.error_invalid_start_time
                        )

                        Text(
                            "-",
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
                            placeholder = { Text("10:00", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                            isError = validationError == R.string.error_invalid_end_time || validationError == R.string.error_end_before_start
                        )
                    }
                }

                // Error message
                validationError?.let { errorResId ->
                    Text(
                        text = stringResource(errorResId),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Assign user button
                OutlinedButton(
                    onClick = {
                        if (isValid) {
                            onConfirmWithUser(date, startTimeStr, endTimeStr)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    enabled = isValid
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
                onClick = {
                    if (isValid) {
                        onConfirm(date, startTimeStr, endTimeStr)
                    }
                },
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                enabled = isValid
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

// ===== MONTHLY CALENDAR VIEW =====

@Composable
private fun MonthlyCalendarView(
    yearMonth: java.time.YearMonth,
    slotsByDate: Map<LocalDate, List<MonthSlotInfo>>,
    onDayClick: (LocalDate) -> Unit,
    onMonthChange: (java.time.YearMonth) -> Unit
) {
    val daysOfWeek = listOf("Po", "Út", "St", "Čt", "Pá", "So", "Ne")
    val today = LocalDate.now()

    // Calculate first day of month and padding
    val firstDayOfMonth = yearMonth.atDay(1)

    // Monday = 1, Sunday = 7, we want Monday as first day
    val startPadding = (firstDayOfMonth.dayOfWeek.value - 1)
    val totalDays = yearMonth.lengthOfMonth()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp)
    ) {
        // Month navigation header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onMonthChange(yearMonth.minusMonths(1)) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
            }

            Text(
                text = yearMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = { onMonthChange(yearMonth.plusMonths(1)) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
            }
        }

        // Days of week header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Calendar grid
        val totalCells = startPadding + totalDays
        val rows = (totalCells + 6) / 7  // Ceiling division

        Column(modifier = Modifier.weight(1f)) {
            repeat(rows) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(7) { col ->
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - startPadding + 1

                        if (dayNumber in 1..totalDays) {
                            val date = yearMonth.atDay(dayNumber)
                            val slots = slotsByDate[date] ?: emptyList()
                            val isToday = date == today
                            val isPast = date.isBefore(today)

                            MonthDayCell(
                                day = dayNumber,
                                isToday = isToday,
                                isPast = isPast,
                                slots = slots,
                                onClick = { onDayClick(date) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // Empty cell
                            Box(modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    day: Int,
    isToday: Boolean,
    isPast: Boolean,
    slots: List<MonthSlotInfo>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxVisibleSlots = 2  // Show max 2 slots, then indicator for more

    val textColor = when {
        isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val todayIndicatorColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .border(
                width = if (isToday) 2.dp else 0.dp,
                color = if (isToday) todayIndicatorColor else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Day number
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) todayIndicatorColor else textColor,
                modifier = Modifier.padding(bottom = 1.dp)
            )

            // Slot indicators (mini blocks)
            if (slots.isNotEmpty() && !isPast) {
                val visibleSlots = slots.take(maxVisibleSlots)
                val remainingCount = slots.size - maxVisibleSlots

                visibleSlots.forEach { slot ->
                    SlotMiniBlock(slot = slot)
                }

                // "+N" indicator if more slots
                if (remainingCount > 0) {
                    Text(
                        text = "+$remainingCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SlotMiniBlock(slot: MonthSlotInfo) {
    val backgroundColor = when {
        slot.isLocked -> MaterialTheme.colorScheme.surfaceVariant
        slot.isReserved -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val textColor = when {
        slot.isLocked -> MaterialTheme.colorScheme.onSurfaceVariant
        slot.isReserved -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(backgroundColor)
            .padding(horizontal = 2.dp, vertical = 1.dp)
    ) {
        Text(
            text = if (slot.label != null) "${slot.time} ${slot.label}" else slot.time,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 7.sp,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
