package com.fitness.app.ui.screens.credits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fitness.app.R
import com.fitness.app.data.dto.CreditPackageDTO
import com.fitness.app.data.dto.CreditTransactionDTO
import com.fitness.app.ui.components.ErrorContent
import com.fitness.app.ui.components.LoadingContent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun CreditsScreen(
    onOpenPaymentUrl: (String) -> Unit,
    viewModel: CreditsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    // Handle payment URL
    LaunchedEffect(uiState.paymentUrl) {
        uiState.paymentUrl?.let { url ->
            onOpenPaymentUrl(url)
            viewModel.clearPaymentUrl()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Balance Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        text = uiState.balance.toString(),
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

        // Packages content directly
        PackagesTab(
            uiState = uiState,
            onPurchase = { viewModel.purchasePackage(it) },
            onRetry = { viewModel.loadPackages() }
        )
    }
}

@Composable
private fun PackagesTab(
    uiState: CreditsUiState,
    onPurchase: (CreditPackageDTO) -> Unit,
    onRetry: () -> Unit
) {
    when {
        uiState.isPackagesLoading -> LoadingContent()
        uiState.packagesErrorResId != null -> ErrorContent(
            message = stringResource(uiState.packagesErrorResId),
            onRetry = onRetry
        )
        uiState.packages.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_packages_available),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        else -> {
            // Memoize expensive calculations
            val sortedPackages = remember(uiState.packages) {
                uiState.packages.sortedBy { it.credits }
            }
            val basePricePerCredit = remember(sortedPackages) {
                sortedPackages.firstOrNull()?.let { it.price / it.credits } ?: 0.0
            }
            val bestValuePackage = remember(uiState.packages) {
                uiState.packages.minByOrNull { it.price / it.credits }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(sortedPackages) { index, pkg ->
                    val pricePerCredit = pkg.price / pkg.credits
                    val savingsPercent = if (basePricePerCredit > 0) {
                        ((basePricePerCredit - pricePerCredit) / basePricePerCredit * 100).toInt()
                    } else 0
                    val isBestValue = pkg == bestValuePackage && sortedPackages.size > 1
                    val isPopular = index == sortedPackages.size / 2 && sortedPackages.size > 2

                    PackageItem(
                        pkg = pkg,
                        onPurchase = { onPurchase(pkg) },
                        isPurchasing = uiState.isPurchasing,
                        savingsPercent = savingsPercent,
                        isBestValue = isBestValue,
                        isPopular = isPopular
                    )
                }
            }
        }
    }
}

@Composable
private fun PackageItem(
    pkg: CreditPackageDTO,
    onPurchase: () -> Unit,
    isPurchasing: Boolean,
    savingsPercent: Int = 0,
    isBestValue: Boolean = false,
    isPopular: Boolean = false
) {
    val pricePerCredit = pkg.price / pkg.credits
    val hasHighlight = isBestValue || isPopular

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = if (hasHighlight) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = if (hasHighlight) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Badge row
            if (hasHighlight || savingsPercent > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isBestValue) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = stringResource(R.string.best_value),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    if (isPopular) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = stringResource(R.string.most_popular),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                    }
                    if (savingsPercent > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = stringResource(R.string.save_percent, savingsPercent),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    // Credits count - prominent display
                    Text(
                        text = stringResource(R.string.credits_format, pkg.credits),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Price per credit
                    Text(
                        text = stringResource(R.string.price_per_credit, formatPrice(pricePerCredit)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Total price on the right
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.price_format, formatPrice(pkg.price)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            pkg.description?.let { desc ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onPurchase,
                enabled = !isPurchasing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isPurchasing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.purchase))
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(
    uiState: CreditsUiState,
    onRetry: () -> Unit
) {
    when {
        uiState.isHistoryLoading -> LoadingContent()
        uiState.historyErrorResId != null -> ErrorContent(
            message = stringResource(uiState.historyErrorResId),
            onRetry = onRetry
        )
        uiState.transactions.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_transactions),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.transactions) { transaction ->
                    TransactionItem(transaction = transaction)
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: CreditTransactionDTO
) {
    val createdAt = LocalDateTime.parse(transaction.createdAt.removeSuffix("Z"))
    val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.type.replace("_", " "),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                transaction.note?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = createdAt.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

private fun formatPrice(price: Double): String {
    return kotlin.math.round(price).toLong().toString()
}
