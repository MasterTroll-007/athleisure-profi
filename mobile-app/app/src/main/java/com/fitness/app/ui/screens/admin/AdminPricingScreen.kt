package com.fitness.app.ui.screens.admin

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitness.app.R
import com.fitness.app.data.dto.AdminCreditPackageDTO
import com.fitness.app.ui.components.ErrorContent
import com.fitness.app.ui.components.LoadingContent
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPricingScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AdminPricingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadPricing()
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
                title = { Text(stringResource(R.string.pricing)) },
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
                Icon(Icons.Default.Add, contentDescription = "Přidat balíček")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> LoadingContent()
                uiState.error != null && uiState.packages.isEmpty() -> ErrorContent(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadPricing() }
                )
                else -> PricingContent(
                    packages = uiState.packages,
                    onEditClick = { viewModel.showEditDialog(it) },
                    onDeleteClick = { viewModel.deletePackage(it.id) }
                )
            }
        }

        if (uiState.showCreateDialog) {
            PackageDialog(
                editingPackage = uiState.editingPackage,
                isSubmitting = uiState.isSubmitting,
                error = uiState.error,
                onDismiss = { viewModel.dismissDialog() },
                onSave = { nameCs, nameEn, description, credits, bonusCredits, priceCzk, isActive, sortOrder ->
                    if (uiState.editingPackage != null) {
                        viewModel.updatePackage(
                            uiState.editingPackage!!.id,
                            nameCs, nameEn, description, credits, bonusCredits, priceCzk, isActive, sortOrder
                        )
                    } else {
                        viewModel.createPackage(
                            nameCs, nameEn, description, credits, bonusCredits, priceCzk, isActive, sortOrder
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun PricingContent(
    packages: List<AdminCreditPackageDTO>,
    onEditClick: (AdminCreditPackageDTO) -> Unit,
    onDeleteClick: (AdminCreditPackageDTO) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (packages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_pricing_items),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(packages, key = { it.id }) { pkg ->
                    CreditPackageCard(
                        pkg = pkg,
                        onEditClick = { onEditClick(pkg) },
                        onDeleteClick = { onDeleteClick(pkg) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) } // FAB clearance
            }
        }
    }
}

@Composable
private fun CreditPackageCard(
    pkg: AdminCreditPackageDTO,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val priceFormat = remember { NumberFormat.getNumberInstance(Locale("cs", "CZ")) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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
                            text = pkg.nameCs,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    if (pkg.isActive) stringResource(R.string.active)
                                    else stringResource(R.string.inactive),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (pkg.isActive)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    pkg.nameEn?.let { nameEn ->
                        Text(
                            text = nameEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    pkg.description?.let { desc ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = desc,
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${pkg.credits} kreditů",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (pkg.bonusCredits > 0) {
                        Text(
                            text = "+ ${pkg.bonusCredits} bonus",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.price_format, priceFormat.format(pkg.priceCzk.toInt())),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pořadí: ${pkg.sortOrder}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Smazat balíček?") },
            text = { Text("Opravdu chcete smazat balíček \"${pkg.nameCs}\"? Tato akce je nevratná.") },
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackageDialog(
    editingPackage: AdminCreditPackageDTO?,
    isSubmitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?, Int, Int, Double, Boolean, Int) -> Unit
) {
    var nameCs by remember { mutableStateOf(editingPackage?.nameCs ?: "") }
    var nameEn by remember { mutableStateOf(editingPackage?.nameEn ?: "") }
    var description by remember { mutableStateOf(editingPackage?.description ?: "") }
    var credits by remember { mutableStateOf(editingPackage?.credits?.toString() ?: "") }
    var bonusCredits by remember { mutableStateOf(editingPackage?.bonusCredits?.toString() ?: "0") }
    var priceCzk by remember { mutableStateOf(editingPackage?.priceCzk?.toInt()?.toString() ?: "") }
    var isActive by remember { mutableStateOf(editingPackage?.isActive ?: true) }
    var sortOrder by remember { mutableStateOf(editingPackage?.sortOrder?.toString() ?: "0") }

    val isEditing = editingPackage != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Upravit balíček" else "Nový balíček") },
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
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Popis") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = credits,
                        onValueChange = { credits = it.filter { c -> c.isDigit() } },
                        label = { Text("Kredity *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = bonusCredits,
                        onValueChange = { bonusCredits = it.filter { c -> c.isDigit() } },
                        label = { Text("Bonus") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = priceCzk,
                        onValueChange = { priceCzk = it.filter { c -> c.isDigit() } },
                        label = { Text("Cena (Kč) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = sortOrder,
                        onValueChange = { sortOrder = it.filter { c -> c.isDigit() } },
                        label = { Text("Pořadí") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val creditsInt = credits.toIntOrNull() ?: 0
                    val bonusInt = bonusCredits.toIntOrNull() ?: 0
                    val priceDouble = priceCzk.toDoubleOrNull() ?: 0.0
                    val sortInt = sortOrder.toIntOrNull() ?: 0
                    onSave(
                        nameCs,
                        nameEn.ifBlank { null },
                        description.ifBlank { null },
                        creditsInt,
                        bonusInt,
                        priceDouble,
                        isActive,
                        sortInt
                    )
                },
                enabled = !isSubmitting && nameCs.isNotBlank() && credits.isNotBlank() && priceCzk.isNotBlank()
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
