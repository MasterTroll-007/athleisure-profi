package com.fitness.app.ui.screens.admin

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
import com.fitness.app.data.dto.ReservationDTO
import com.fitness.app.ui.components.ErrorContent
import com.fitness.app.ui.components.LoadingContent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun AdminDashboardScreen(
    onNavigateToCalendar: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToPlans: () -> Unit,
    onNavigateToPricing: () -> Unit,
    onNavigateToPayments: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    when {
        uiState.isLoading -> LoadingContent()
        uiState.error != null -> ErrorContent(
            message = uiState.error!!,
            onRetry = { viewModel.loadData() }
        )
        else -> DashboardContent(
            uiState = uiState,
            onNavigateToCalendar = onNavigateToCalendar,
            onNavigateToClients = onNavigateToClients,
            onNavigateToTemplates = onNavigateToTemplates,
            onNavigateToPlans = onNavigateToPlans,
            onNavigateToPricing = onNavigateToPricing,
            onNavigateToPayments = onNavigateToPayments
        )
    }
}

@Composable
private fun DashboardContent(
    uiState: AdminDashboardUiState,
    onNavigateToCalendar: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToPlans: () -> Unit,
    onNavigateToPricing: () -> Unit,
    onNavigateToPayments: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.total_clients),
                    value = uiState.totalClients.toString(),
                    icon = Icons.Default.People,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.today),
                    value = uiState.todayReservations.size.toString(),
                    icon = Icons.Default.Today,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.this_week),
                    value = uiState.weeklyReservations.toString(),
                    icon = Icons.Default.DateRange,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.revenue),
                    value = "${uiState.weeklyRevenue} CZK",
                    icon = Icons.Default.AttachMoney,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick actions
        item {
            Text(
                text = stringResource(R.string.quick_actions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.CalendarMonth,
                    title = stringResource(R.string.calendar),
                    onClick = onNavigateToCalendar,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = Icons.Default.People,
                    title = stringResource(R.string.clients),
                    onClick = onNavigateToClients,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.Schedule,
                    title = stringResource(R.string.templates),
                    onClick = onNavigateToTemplates,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = Icons.Default.FitnessCenter,
                    title = stringResource(R.string.plans),
                    onClick = onNavigateToPlans,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.Sell,
                    title = stringResource(R.string.pricing),
                    onClick = onNavigateToPricing,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = Icons.Default.Payment,
                    title = stringResource(R.string.payments),
                    onClick = onNavigateToPayments,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Today's trainings
        item {
            Text(
                text = stringResource(R.string.today_trainings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }

        if (uiState.todayReservations.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.no_trainings_today),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.todayReservations) { reservation ->
                TodayReservationItem(reservation = reservation)
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun TodayReservationItem(
    reservation: ReservationDTO
) {
    val startTime = LocalDateTime.parse(reservation.startTime.removeSuffix("Z"))
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
                    text = startTime.format(timeFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = reservation.userName ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
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
    }
}
