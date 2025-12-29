package com.fitness.app.ui.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitness.app.R
import com.fitness.app.data.dto.AdminTrainingPlanDTO
import com.fitness.app.ui.components.ErrorContent
import com.fitness.app.ui.components.LoadingContent
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPlansScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AdminPlansViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Track which plan needs upload
    var pendingUploadPlanId by remember { mutableStateOf<String?>(null) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pendingUploadPlanId?.let { planId ->
                // Copy file to cache directory
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileName = "upload_${System.currentTimeMillis()}.pdf"
                val cacheFile = File(context.cacheDir, fileName)
                inputStream?.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.uploadFile(planId, cacheFile)
                pendingUploadPlanId = null
            }
        } ?: run {
            pendingUploadPlanId = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadPlans()
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
                title = { Text(stringResource(R.string.training_plans)) },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Přidat plán")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> LoadingContent()
                uiState.error != null && uiState.plans.isEmpty() -> ErrorContent(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadPlans() }
                )
                else -> PlansContent(
                    plans = uiState.plans,
                    uploadingPlanId = uiState.uploadingPlanId,
                    onEditClick = { viewModel.showEditDialog(it) },
                    onDeleteClick = { viewModel.deletePlan(it.id) },
                    onUploadClick = { plan ->
                        pendingUploadPlanId = plan.id
                        filePickerLauncher.launch("application/pdf")
                    },
                    onDeleteFileClick = { viewModel.deleteFile(it.id) }
                )
            }
        }

        if (uiState.showCreateDialog) {
            PlanDialog(
                editingPlan = uiState.editingPlan,
                isSubmitting = uiState.isSubmitting,
                error = uiState.error,
                onDismiss = { viewModel.dismissDialog() },
                onSave = { nameCs, nameEn, descriptionCs, descriptionEn, credits, isActive ->
                    if (uiState.editingPlan != null) {
                        viewModel.updatePlan(
                            uiState.editingPlan!!.id,
                            nameCs, nameEn, descriptionCs, descriptionEn, credits, isActive
                        )
                    } else {
                        viewModel.createPlan(
                            nameCs, nameEn, descriptionCs, descriptionEn, credits, isActive
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun PlansContent(
    plans: List<AdminTrainingPlanDTO>,
    uploadingPlanId: String?,
    onEditClick: (AdminTrainingPlanDTO) -> Unit,
    onDeleteClick: (AdminTrainingPlanDTO) -> Unit,
    onUploadClick: (AdminTrainingPlanDTO) -> Unit,
    onDeleteFileClick: (AdminTrainingPlanDTO) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (plans.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_plans_available),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(plans, key = { it.id }) { plan ->
                    PlanCard(
                        plan = plan,
                        isUploading = uploadingPlanId == plan.id,
                        onEditClick = { onEditClick(plan) },
                        onDeleteClick = { onDeleteClick(plan) },
                        onUploadClick = { onUploadClick(plan) },
                        onDeleteFileClick = { onDeleteFileClick(plan) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) } // FAB clearance
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: AdminTrainingPlanDTO,
    isUploading: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUploadClick: () -> Unit,
    onDeleteFileClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteFileDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = plan.nameCs,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    if (plan.isActive) stringResource(R.string.active)
                                    else stringResource(R.string.inactive),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (plan.isActive)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    plan.nameEn?.let { nameEn ->
                        Text(
                            text = nameEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Upravit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Smazat",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            plan.descriptionCs?.let { desc ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Credits cost
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${plan.credits} ${stringResource(R.string.credits)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // PDF actions
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else if (plan.filePath != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PDF",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        IconButton(
                            onClick = { showDeleteFileDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Smazat PDF",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    TextButton(
                        onClick = onUploadClick,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nahrát PDF", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Smazat plán?") },
            text = { Text("Opravdu chcete smazat plán \"${plan.nameCs}\"? Tato akce je nevratná.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick()
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

    if (showDeleteFileDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteFileDialog = false },
            title = { Text("Smazat PDF?") },
            text = { Text("Opravdu chcete smazat PDF soubor tohoto plánu?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteFileDialog = false
                        onDeleteFileClick()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Smazat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFileDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanDialog(
    editingPlan: AdminTrainingPlanDTO?,
    isSubmitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?, String?, Int, Boolean) -> Unit
) {
    var nameCs by remember { mutableStateOf(editingPlan?.nameCs ?: "") }
    var nameEn by remember { mutableStateOf(editingPlan?.nameEn ?: "") }
    var descriptionCs by remember { mutableStateOf(editingPlan?.descriptionCs ?: "") }
    var descriptionEn by remember { mutableStateOf(editingPlan?.descriptionEn ?: "") }
    var credits by remember { mutableStateOf(editingPlan?.credits?.toString() ?: "5") }
    var isActive by remember { mutableStateOf(editingPlan?.isActive ?: true) }

    val isEditing = editingPlan != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Upravit plán" else "Nový plán") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = nameCs,
                    onValueChange = { nameCs = it },
                    label = { Text("Název (CZ) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text("Název (EN)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descriptionCs,
                    onValueChange = { descriptionCs = it },
                    label = { Text("Popis (CZ)") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descriptionEn,
                    onValueChange = { descriptionEn = it },
                    label = { Text("Popis (EN)") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = credits,
                    onValueChange = { credits = it.filter { c -> c.isDigit() } },
                    label = { Text("Kredity *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isActive) "Aktivní" else "Neaktivní",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (editingPlan?.filePath != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PDF soubor je připojen",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val creditsInt = credits.toIntOrNull() ?: 5
                    onSave(
                        nameCs,
                        nameEn.ifBlank { null },
                        descriptionCs.ifBlank { null },
                        descriptionEn.ifBlank { null },
                        creditsInt,
                        isActive
                    )
                },
                enabled = !isSubmitting && nameCs.isNotBlank() && credits.isNotBlank()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (isEditing) "Uložit" else "Vytvořit")
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
