package com.fitness.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitness.app.R
import com.fitness.app.data.dto.PaymentDTO
import com.fitness.app.ui.components.LoadingContent
import com.fitness.app.ui.components.ErrorContent
import java.text.NumberFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPaymentsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AdminPaymentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPayments()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.payments)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingContent(modifier = Modifier.padding(paddingValues))
            }
            uiState.error != null -> {
                ErrorContent(
                    message = uiState.error ?: "Error loading payments",
                    onRetry = { viewModel.loadPayments() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
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
                                icon = Icons.Default.AttachMoney,
                                label = "Tento měsíc",
                                value = formatCurrency(uiState.monthlyStats.thisMonthRevenue),
                                color = Color(0xFF22C55E),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                icon = Icons.Default.CheckCircle,
                                label = "Úspěšných",
                                value = uiState.monthlyStats.successfulPayments.toString(),
                                color = Color(0xFF3B82F6),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                icon = Icons.Default.Schedule,
                                label = "Čekajících",
                                value = uiState.monthlyStats.pendingPayments.toString(),
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Filter tabs
                    item {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PaymentFilter.entries.forEachIndexed { index, filter ->
                                SegmentedButton(
                                    selected = uiState.selectedFilter == filter,
                                    onClick = { viewModel.setFilter(filter) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = PaymentFilter.entries.size
                                    )
                                ) {
                                    Text(
                                        text = when (filter) {
                                            PaymentFilter.ALL -> "Vše"
                                            PaymentFilter.PAID -> "Zaplaceno"
                                            PaymentFilter.PENDING -> "Čeká"
                                            PaymentFilter.CANCELLED -> "Zrušeno"
                                        },
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Group payments by month
                    val groupedPayments = groupPaymentsByMonth(uiState.payments)

                    if (groupedPayments.isEmpty()) {
                        item {
                            EmptyPaymentsContent()
                        }
                    } else {
                        groupedPayments.forEach { (month, payments) ->
                            item {
                                Text(
                                    text = month,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            items(payments, key = { it.id }) { payment ->
                                PaymentCard(payment = payment)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PaymentCard(payment: PaymentDTO) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column {
                    Text(
                        text = payment.userName ?: "Neznámý",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatDate(payment.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = payment.creditPackageName ?: "Balíček",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatCurrency(payment.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                PaymentStatusBadge(state = payment.state)
            }
        }
    }
}

@Composable
private fun PaymentStatusBadge(state: String) {
    val (backgroundColor, textColor, text, icon) = when (state.uppercase()) {
        "PAID" -> Quadruple(
            Color(0xFF22C55E).copy(alpha = 0.1f),
            Color(0xFF22C55E),
            "Zaplaceno",
            Icons.Default.Check
        )
        "CREATED", "PENDING" -> Quadruple(
            Color(0xFFF59E0B).copy(alpha = 0.1f),
            Color(0xFFF59E0B),
            "Čeká",
            Icons.Default.Schedule
        )
        "CANCELED", "TIMEOUTED" -> Quadruple(
            Color(0xFFEF4444).copy(alpha = 0.1f),
            Color(0xFFEF4444),
            "Zrušeno",
            Icons.Default.Close
        )
        "REFUNDED" -> Quadruple(
            Color(0xFF3B82F6).copy(alpha = 0.1f),
            Color(0xFF3B82F6),
            "Vráceno",
            Icons.Default.Undo
        )
        else -> Quadruple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            state,
            Icons.Default.Info
        )
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = textColor
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmptyPaymentsContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AttachMoney,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Žádné platby",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("cs", "CZ"))
    return format.format(amount)
}

private fun formatDate(dateString: String): String {
    return try {
        val zonedDateTime = ZonedDateTime.parse(dateString)
        zonedDateTime.format(DateTimeFormatter.ofPattern("d. M. yyyy"))
    } catch (e: Exception) {
        dateString.take(10)
    }
}

private fun groupPaymentsByMonth(payments: List<PaymentDTO>): Map<String, List<PaymentDTO>> {
    return payments.groupBy { payment ->
        try {
            val zonedDateTime = ZonedDateTime.parse(payment.createdAt)
            val formatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("cs", "CZ"))
            zonedDateTime.format(formatter).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            "Neznámý měsíc"
        }
    }.toSortedMap(compareByDescending { it })
}
