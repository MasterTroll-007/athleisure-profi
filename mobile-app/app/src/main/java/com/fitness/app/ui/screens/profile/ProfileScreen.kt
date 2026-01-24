package com.fitness.app.ui.screens.profile

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitness.app.R
import com.fitness.app.data.local.PreferencesManager
import com.fitness.app.ui.components.ErrorContent
import com.fitness.app.ui.components.LoadingContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToClients: () -> Unit = {},
    onNavigateToReservations: () -> Unit = {},
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToPlans: () -> Unit = {},
    onNavigateToPricing: () -> Unit = {},
    onNavigateToPayments: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
    preferencesManager: PreferencesManager
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedLocale by preferencesManager.selectedLocale.collectAsState(initial = null)

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    LaunchedEffect(uiState.logoutSuccess) {
        if (uiState.logoutSuccess) {
            onLogout()
        }
    }

    when {
        uiState.isLoading -> LoadingContent()
        uiState.errorResId != null -> ErrorContent(
            message = stringResource(uiState.errorResId!!),
            onRetry = { viewModel.loadProfile() }
        )
        else -> ProfileContent(
            uiState = uiState,
            selectedLocale = selectedLocale,
            onEditProfile = { showEditDialog = true },
            onChangePassword = { showPasswordDialog = true },
            onLanguageClick = { showLanguageDialog = true },
            onBiometricToggle = { enabled -> viewModel.setBiometricEnabled(enabled) },
            onLogout = { showLogoutDialog = true },
            onNavigateToClients = onNavigateToClients,
            onNavigateToReservations = onNavigateToReservations,
            onNavigateToTemplates = onNavigateToTemplates,
            onNavigateToPlans = onNavigateToPlans,
            onNavigateToPricing = onNavigateToPricing,
            onNavigateToPayments = onNavigateToPayments,
            onNavigateToSettings = onNavigateToSettings,
            onReminderToggle = { enabled ->
                viewModel.updateReminderSettings(enabled, uiState.reminderHoursBefore)
            },
            onReminderTimingChange = { hours ->
                viewModel.updateReminderSettings(uiState.emailRemindersEnabled, hours)
            }
        )
    }

    // Edit Profile Dialog
    if (showEditDialog) {
        EditProfileDialog(
            uiState = uiState,
            onDismiss = { showEditDialog = false },
            onSave = { firstName, lastName, phone ->
                viewModel.updateProfile(firstName, lastName, phone)
                showEditDialog = false
            }
        )
    }

    // Change Password Dialog
    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onSave = { current, new ->
                viewModel.changePassword(current, new)
                showPasswordDialog = false
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout)) },
            text = { Text(stringResource(R.string.confirm_logout)) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLocale = selectedLocale,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { locale ->
                scope.launch {
                    preferencesManager.setLocale(locale)
                    showLanguageDialog = false
                    // Recreate activity to apply language change
                    (context as? Activity)?.recreate()
                }
            }
        )
    }
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    selectedLocale: String?,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onLanguageClick: () -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToReservations: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToPlans: () -> Unit,
    onNavigateToPricing: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onReminderToggle: (Boolean) -> Unit,
    onReminderTimingChange: (Int) -> Unit
) {
    val isAdmin = uiState.role == "admin"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Profile Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar placeholder
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (uiState.firstName?.firstOrNull() ?: uiState.email.first()).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = listOfNotNull(uiState.firstName, uiState.lastName)
                        .joinToString(" ")
                        .ifEmpty { uiState.email.substringBefore("@") },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = uiState.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                uiState.phone?.let { phone ->
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Account Actions
        Text(
            text = stringResource(R.string.account),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                ProfileMenuItem(
                    icon = Icons.Default.Edit,
                    title = stringResource(R.string.edit_profile),
                    onClick = onEditProfile
                )
                HorizontalDivider()
                ProfileMenuItem(
                    icon = Icons.Default.Lock,
                    title = stringResource(R.string.change_password),
                    onClick = onChangePassword
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Settings Section
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                ProfileMenuItemWithValue(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.language),
                    value = getLanguageDisplayName(selectedLocale),
                    onClick = onLanguageClick
                )
                if (uiState.isBiometricAvailable) {
                    HorizontalDivider()
                    BiometricToggleItem(
                        isEnabled = uiState.isBiometricEnabled,
                        onToggle = onBiometricToggle
                    )
                }
                HorizontalDivider()
                // Email Reminders Toggle
                ProfileMenuItemWithSwitch(
                    icon = Icons.Outlined.Notifications,
                    title = stringResource(R.string.email_reminders),
                    checked = uiState.emailRemindersEnabled,
                    onCheckedChange = onReminderToggle
                )
                // Reminder Timing (only visible if reminders enabled)
                if (uiState.emailRemindersEnabled) {
                    HorizontalDivider()
                    ProfileMenuItemWithValue(
                        icon = Icons.Default.Schedule,
                        title = stringResource(R.string.reminder_timing),
                        value = if (uiState.reminderHoursBefore == 1)
                            stringResource(R.string.reminder_1h)
                        else
                            stringResource(R.string.reminder_24h),
                        onClick = {
                            // Toggle between 1h and 24h
                            onReminderTimingChange(if (uiState.reminderHoursBefore == 1) 24 else 1)
                        }
                    )
                }
            }
        }

        // Admin Section - only visible for admins
        if (isAdmin) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.admin),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ProfileMenuItem(
                        icon = Icons.Default.People,
                        title = stringResource(R.string.clients),
                        onClick = onNavigateToClients
                    )
                    HorizontalDivider()
                    ProfileMenuItem(
                        icon = Icons.Default.EventNote,
                        title = stringResource(R.string.admin_reservations),
                        onClick = onNavigateToReservations
                    )
                    HorizontalDivider()
                    ProfileMenuItem(
                        icon = Icons.Default.ViewModule,
                        title = stringResource(R.string.templates),
                        onClick = onNavigateToTemplates
                    )
                    HorizontalDivider()
                    ProfileMenuItem(
                        icon = Icons.Default.FitnessCenter,
                        title = stringResource(R.string.plans),
                        onClick = onNavigateToPlans
                    )
                    HorizontalDivider()
                    ProfileMenuItem(
                        icon = Icons.Default.Sell,
                        title = stringResource(R.string.pricing),
                        onClick = onNavigateToPricing
                    )
                    HorizontalDivider()
                    ProfileMenuItem(
                        icon = Icons.Default.Payment,
                        title = stringResource(R.string.payments),
                        onClick = onNavigateToPayments
                    )
                    HorizontalDivider()
                    ProfileMenuItem(
                        icon = Icons.Default.Settings,
                        title = stringResource(R.string.settings),
                        onClick = onNavigateToSettings
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logout Button
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.logout))
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    uiState: ProfileUiState,
    onDismiss: () -> Unit,
    onSave: (String?, String?, String?) -> Unit
) {
    var firstName by remember { mutableStateOf(uiState.firstName ?: "") }
    var lastName by remember { mutableStateOf(uiState.lastName ?: "") }
    var phone by remember { mutableStateOf(uiState.phone ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_profile)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text(stringResource(R.string.first_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text(stringResource(R.string.last_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.phone)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        firstName.takeIf { it.isNotBlank() },
                        lastName.takeIf { it.isNotBlank() },
                        phone.takeIf { it.isNotBlank() }
                    )
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
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorType by remember { mutableStateOf<PasswordErrorType?>(null) }

    val errorMessage = when (errorType) {
        PasswordErrorType.FILL_ALL_FIELDS -> stringResource(R.string.fill_all_fields)
        PasswordErrorType.PASSWORDS_NOT_MATCH -> stringResource(R.string.passwords_not_match)
        PasswordErrorType.PASSWORD_TOO_SHORT -> stringResource(R.string.password_min_length)
        null -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.change_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(stringResource(R.string.current_password)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.new_password)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.confirm_password)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        currentPassword.isBlank() || newPassword.isBlank() -> {
                            errorType = PasswordErrorType.FILL_ALL_FIELDS
                        }
                        newPassword != confirmPassword -> {
                            errorType = PasswordErrorType.PASSWORDS_NOT_MATCH
                        }
                        newPassword.length < 8 -> {
                            errorType = PasswordErrorType.PASSWORD_TOO_SHORT
                        }
                        else -> {
                            onSave(currentPassword, newPassword)
                        }
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

private enum class PasswordErrorType {
    FILL_ALL_FIELDS,
    PASSWORDS_NOT_MATCH,
    PASSWORD_TOO_SHORT
}

@Composable
private fun ProfileMenuItemWithSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ProfileMenuItemWithValue(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BiometricToggleItem(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Fingerprint,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.biometric_login),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.biometric_login_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun getLanguageDisplayName(locale: String?): String {
    return when (locale) {
        "en" -> stringResource(R.string.english)
        "cs" -> stringResource(R.string.czech)
        else -> stringResource(R.string.system_default)
    }
}

@Composable
private fun LanguageSelectionDialog(
    currentLocale: String?,
    onDismiss: () -> Unit,
    onLanguageSelected: (String?) -> Unit
) {
    val options = listOf(
        null to stringResource(R.string.system_default),
        "en" to stringResource(R.string.english),
        "cs" to stringResource(R.string.czech)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_language)) },
        text = {
            Column(
                modifier = Modifier.selectableGroup()
            ) {
                options.forEach { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = currentLocale == code,
                                onClick = { onLanguageSelected(code) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLocale == code,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
