package com.fitness.app.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Note
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
import com.fitness.app.data.dto.ClientNoteDTO
import com.fitness.app.data.dto.ReservationDTO
import com.fitness.app.ui.components.ErrorContent
import com.fitness.app.ui.components.LoadingContent
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminClientDetailScreen(
    clientId: String,
    onNavigateBack: () -> Unit,
    viewModel: AdminClientDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAdjustDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(clientId) {
        viewModel.loadClient(clientId)
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail klienta") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> LoadingContent(modifier = Modifier.padding(paddingValues))
            uiState.error != null && uiState.client == null -> ErrorContent(
                message = uiState.error!!,
                onRetry = { viewModel.loadClient(clientId) },
                modifier = Modifier.padding(paddingValues)
            )
            uiState.client != null -> {
                val client = uiState.client!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Client info card
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier.size(80.dp),
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = (client.firstName?.firstOrNull() ?: client.email.first()).uppercase(),
                                            style = MaterialTheme.typography.headlineLarge,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = listOfNotNull(client.firstName, client.lastName)
                                        .joinToString(" ")
                                        .ifEmpty { client.email.substringBefore("@") },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = client.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                client.phone?.let { phone ->
                                    Text(
                                        text = phone,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Credits card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Zůstatek kreditů",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = client.creditBalance.toString(),
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Button(onClick = { showAdjustDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.adjust_credits))
                                }
                            }
                        }
                    }

                    // Notes section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Poznámky",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(onClick = { viewModel.showAddNoteDialog() }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Přidat poznámku",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (uiState.notes.isEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Note,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Žádné poznámky",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(uiState.notes, key = { it.id }) { note ->
                            NoteItem(
                                note = note,
                                onDelete = { viewModel.deleteNote(note.id) }
                            )
                        }
                    }

                    // Reservations section
                    item {
                        Text(
                            text = stringResource(R.string.reservations),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (uiState.reservations.isEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Žádné rezervace",
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(uiState.reservations) { reservation ->
                            ReservationItem(reservation = reservation)
                        }
                    }
                }
            }
        }
    }

    // Adjust credits dialog
    if (showAdjustDialog) {
        AdjustCreditsDialog(
            onDismiss = { showAdjustDialog = false },
            onConfirm = { amount, reason ->
                viewModel.adjustCredits(clientId, amount, reason)
                showAdjustDialog = false
            }
        )
    }

    // Add note dialog
    if (uiState.showAddNoteDialog) {
        AddNoteDialog(
            isSubmitting = uiState.isSubmittingNote,
            onDismiss = { viewModel.dismissAddNoteDialog() },
            onConfirm = { content -> viewModel.createNote(content) }
        )
    }
}

@Composable
private fun NoteItem(
    note: ClientNoteDTO,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        note.adminName?.let { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = formatNoteDate(note.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Smazat",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Smazat poznámku?") },
            text = { Text("Opravdu chcete smazat tuto poznámku?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Smazat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }
}

@Composable
private fun ReservationItem(
    reservation: ReservationDTO
) {
    val startTime = try {
        LocalDateTime.parse(reservation.startTime.removeSuffix("Z"))
    } catch (e: Exception) {
        try {
            ZonedDateTime.parse(reservation.startTime).toLocalDateTime()
        } catch (e: Exception) {
            null
        }
    }

    val dateFormatter = DateTimeFormatter.ofPattern("EEE, d. M. yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = startTime?.format(dateFormatter) ?: reservation.startTime,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                startTime?.let {
                    Text(
                        text = it.format(timeFormatter),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            AssistChip(
                onClick = {},
                label = { Text(reservation.status) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = when (reservation.status) {
                        "CONFIRMED" -> MaterialTheme.colorScheme.primaryContainer
                        "CANCELLED" -> MaterialTheme.colorScheme.errorContainer
                        "COMPLETED" -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdjustCreditsDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.adjust_credits)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Zadejte kladné číslo pro přidání nebo záporné pro odebrání kreditů.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Počet") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Důvod") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountInt = amount.toIntOrNull()
                    when {
                        amountInt == null -> error = "Zadejte platné číslo"
                        amountInt == 0 -> error = "Počet nemůže být nula"
                        reason.isBlank() -> error = "Zadejte důvod"
                        else -> onConfirm(amountInt, reason)
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddNoteDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nová poznámka") },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Text poznámky") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(content) },
                enabled = !isSubmitting && content.isNotBlank()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Přidat")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Zrušit")
            }
        }
    )
}

private fun formatNoteDate(dateString: String): String {
    return try {
        val instant = java.time.Instant.parse(dateString)
        val zdt = java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
        zdt.format(DateTimeFormatter.ofPattern("d. M. yyyy HH:mm"))
    } catch (e: Exception) {
        dateString.take(16)
    }
}
