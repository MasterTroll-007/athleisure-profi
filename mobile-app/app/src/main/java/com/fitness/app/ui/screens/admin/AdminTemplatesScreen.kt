package com.fitness.app.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitness.app.R
import com.fitness.app.data.dto.SlotTemplateDTO
import com.fitness.app.data.dto.TemplateSlotDTO
import com.fitness.app.ui.components.ErrorContent
import com.fitness.app.ui.components.LoadingContent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTemplatesScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AdminTemplatesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Editor state
    var showEditor by remember { mutableStateOf(false) }
    var editorTemplateName by remember { mutableStateOf("") }
    var editorTemplateSlots by remember { mutableStateOf<List<TemplateSlotDTO>>(emptyList()) }
    var editingTemplateId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadTemplates()
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main list view
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.templates)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                when {
                    uiState.isLoading -> LoadingContent()
                    uiState.error != null -> ErrorContent(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadTemplates() }
                    )
                    else -> TemplatesContent(
                        templates = uiState.templates,
                        onCreateClick = {
                            editorTemplateName = ""
                            editorTemplateSlots = emptyList()
                            editingTemplateId = null
                            showEditor = true
                        },
                        onEditClick = { template ->
                            editorTemplateName = template.name
                            editorTemplateSlots = template.slots
                            editingTemplateId = template.id
                            showEditor = true
                        },
                        onApplyClick = { viewModel.showApplyDialog(it) },
                        onDeleteClick = { viewModel.deleteTemplate(it.id) }
                    )
                }
            }
        }

        // Calendar editor overlay
        AnimatedVisibility(
            visible = showEditor,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                TemplateCalendarEditorScreen(
                    templateName = editorTemplateName,
                    initialSlots = editorTemplateSlots,
                    onSave = { name, slots ->
                        if (editingTemplateId != null) {
                            viewModel.updateTemplate(editingTemplateId!!, name, slots, null)
                        } else {
                            viewModel.createTemplate(name, slots)
                        }
                        showEditor = false
                    },
                    onCancel = {
                        showEditor = false
                        editingTemplateId = null
                    },
                    isProcessing = uiState.isProcessing
                )
            }
        }
    }

    // Apply Template Dialog
    if (uiState.showApplyDialog && uiState.selectedTemplate != null) {
        ApplyTemplateDialog(
            template = uiState.selectedTemplate!!,
            onDismiss = { viewModel.dismissDialogs() },
            onApply = { date ->
                viewModel.applyTemplate(uiState.selectedTemplate!!.id, date)
            },
            isProcessing = uiState.isProcessing
        )
    }
}

@Composable
private fun TemplatesContent(
    templates: List<SlotTemplateDTO>,
    onCreateClick: () -> Unit,
    onEditClick: (SlotTemplateDTO) -> Unit,
    onApplyClick: (SlotTemplateDTO) -> Unit,
    onDeleteClick: (SlotTemplateDTO) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            FilledTonalButton(onClick = onCreateClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.create_template))
            }
        }

        if (templates.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_templates),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(templates) { template ->
                    TemplateCard(
                        template = template,
                        onEditClick = { onEditClick(template) },
                        onApplyClick = { onApplyClick(template) },
                        onDeleteClick = { onDeleteClick(template) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: SlotTemplateDTO,
    onEditClick: () -> Unit,
    onApplyClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${template.slots.size} ${stringResource(R.string.slots)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = {},
                    label = {
                        Text(if (template.isActive) stringResource(R.string.active) else stringResource(R.string.inactive))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (template.isActive)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (template.slots.isNotEmpty()) {
                val slotsByDay = template.slots.groupBy { it.dayOfWeek }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (1..7).forEach { day ->
                        val daySlots = slotsByDay[day] ?: emptyList()
                        val dayName = DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        Surface(
                            color = if (daySlots.isNotEmpty())
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = dayName, style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = daySlots.size.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onEditClick, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.edit))
                }
                Button(onClick = onApplyClick, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.apply_template))
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_template)) },
            text = { Text(stringResource(R.string.confirm_delete_template)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteClick()
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
private fun ApplyTemplateDialog(
    template: SlotTemplateDTO,
    onDismiss: () -> Unit,
    onApply: (LocalDate) -> Unit,
    isProcessing: Boolean
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochDay() * 24 * 60 * 60 * 1000
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.apply_template)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.apply_template_description, template.name),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.select_week_starting, selectedDate.toString()))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onApply(selectedDate) }, enabled = !isProcessing) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.apply))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    if (showDatePicker) {
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
