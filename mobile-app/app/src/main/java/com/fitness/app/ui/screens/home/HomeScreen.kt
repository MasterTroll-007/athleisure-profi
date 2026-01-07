package com.fitness.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitness.app.R
import com.fitness.app.data.dto.CreditTransactionDTO
import com.fitness.app.data.dto.ReservationDTO
import com.fitness.app.data.dto.SlotDTO
import com.fitness.app.ui.components.ErrorContent
import com.fitness.app.ui.components.LoadingContent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToReservations: () -> Unit,
    onNavigateToCredits: () -> Unit,
    onNavigateToClients: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    when {
        uiState.isLoading -> LoadingContent()
        uiState.errorResId != null -> ErrorContent(
            message = stringResource(uiState.errorResId!!),
            onRetry = { viewModel.loadData() }
        )
        uiState.isAdmin -> AdminHomeContent(
            uiState = uiState,
            onNavigateToReservations = onNavigateToReservations,
            onNavigateToClients = onNavigateToClients
        )
        else -> ClientHomeContent(
            uiState = uiState,
            onNavigateToReservations = onNavigateToReservations,
            onNavigateToCredits = onNavigateToCredits
        )
    }
}

@Composable
private fun AdminHomeContent(
    uiState: HomeUiState,
    onNavigateToReservations: () -> Unit,
    onNavigateToClients: () -> Unit
) {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.getDefault())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Today's slots
        item {
            Text(
                text = stringResource(R.string.today) + " - " + today.format(dateFormatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.todaySlots.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = stringResource(R.string.no_slots_today),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.todaySlots) { slot ->
                AdminSlotItem(slot = slot)
            }
        }

        // Tomorrow's slots
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.tomorrow) + " - " + tomorrow.format(dateFormatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.tomorrowSlots.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = stringResource(R.string.no_slots_tomorrow),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.tomorrowSlots) { slot ->
                AdminSlotItem(slot = slot)
            }
        }

        // Legend
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SlotStatusLegend()
        }

        // Quick Actions
        item {
            Spacer(modifier = Modifier.height(8.dp))
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
                    onClick = onNavigateToReservations,
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
    }
}

@Composable
private fun AdminSlotItem(slot: SlotDTO) {
    val statusColor = when (slot.status.lowercase()) {
        "reserved", "booked" -> Color(0xFF2196F3) // Blue
        "cancelled" -> Color(0xFFF44336) // Red
        "unlocked" -> Color(0xFF4CAF50) // Green
        "locked" -> Color(0xFF9E9E9E) // Gray
        "blocked" -> Color(0xFFFF9800) // Orange
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            // Time
            Text(
                text = "${slot.startTime.take(5)} - ${slot.endTime.take(5)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            // Client name or status
            Text(
                text = slot.assignedUserName ?: slot.status.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = if (slot.assignedUserName != null)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            // Note indicator
            if (!slot.note.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Default.Notes,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SlotStatusLegend() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.legend),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendItem(color = Color(0xFF2196F3), label = stringResource(R.string.slot_reserved))
                LegendItem(color = Color(0xFF4CAF50), label = stringResource(R.string.slot_unlocked))
                LegendItem(color = Color(0xFF9E9E9E), label = stringResource(R.string.slot_locked))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendItem(color = Color(0xFFF44336), label = stringResource(R.string.slot_cancelled))
                LegendItem(color = Color(0xFFFF9800), label = stringResource(R.string.slot_blocked))
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun ClientHomeContent(
    uiState: HomeUiState,
    onNavigateToReservations: () -> Unit,
    onNavigateToCredits: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Credits Card
        item {
            CreditBalanceCard(
                balance = uiState.creditBalance,
                onClick = onNavigateToCredits
            )
        }

        // Next Training Card
        item {
            NextTrainingCard(
                reservation = uiState.nextReservation,
                onClick = onNavigateToReservations
            )
        }

        // Quick Actions
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
                    title = stringResource(R.string.book_training),
                    onClick = onNavigateToReservations,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = Icons.Default.CreditCard,
                    title = stringResource(R.string.buy_credits),
                    onClick = onNavigateToCredits,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recent Activity
        item {
            Text(
                text = stringResource(R.string.recent_activity),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }

        if (uiState.recentTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.no_transactions),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.recentTransactions.take(5)) { transaction ->
                TransactionItem(transaction = transaction)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CreditBalanceCard(
    balance: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
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
                    text = stringResource(R.string.your_credits),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = balance.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun NextTrainingCard(
    reservation: ReservationDTO?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
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
                    text = stringResource(R.string.next_training),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (reservation != null) {
                    val date = LocalDate.parse(reservation.date)
                    val time = LocalTime.parse(reservation.startTime)
                    val dateTime = LocalDateTime.of(date, time)
                    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM • HH:mm")
                    Text(
                        text = dateTime.format(formatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = stringResource(R.string.no_upcoming),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun TransactionItem(
    transaction: CreditTransactionDTO
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.type,
                    style = MaterialTheme.typography.bodyMedium
                )
                transaction.note?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = if (transaction.amount >= 0) "+${transaction.amount}" else "${transaction.amount}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

