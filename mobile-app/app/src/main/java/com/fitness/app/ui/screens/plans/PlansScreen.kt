package com.fitness.app.ui.screens.plans

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
import com.fitness.app.data.dto.PurchasedPlanDTO
import com.fitness.app.data.dto.TrainingPlanDTO
import com.fitness.app.ui.components.ErrorContent
import com.fitness.app.ui.components.LoadingContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    onDownloadPlan: (String) -> Unit,
    viewModel: PlansViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(R.string.training_plans, R.string.my_plans)

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plans)) }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, titleRes ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(stringResource(titleRes)) }
                    )
                }
            }

            when (selectedTab) {
                0 -> AllPlansTab(
                    uiState = uiState,
                    onPurchase = { viewModel.purchasePlan(it) },
                    onRetry = { viewModel.loadPlans() }
                )
                1 -> MyPlansTab(
                    uiState = uiState,
                    onDownload = onDownloadPlan,
                    onRetry = { viewModel.loadMyPlans() }
                )
            }
        }
    }

    // Purchase confirmation dialog
    if (uiState.showPurchaseDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPurchaseDialog() },
            title = { Text("Purchase Plan") },
            text = {
                Text("Purchase this plan for ${uiState.selectedPlan?.creditCost ?: 0} credits?")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmPurchase() },
                    enabled = !uiState.isPurchasing
                ) {
                    if (uiState.isPurchasing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.purchase))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPurchaseDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun AllPlansTab(
    uiState: PlansUiState,
    onPurchase: (TrainingPlanDTO) -> Unit,
    onRetry: () -> Unit
) {
    when {
        uiState.isPlansLoading -> LoadingContent()
        uiState.plansError != null -> ErrorContent(
            message = uiState.plansError,
            onRetry = onRetry
        )
        uiState.plans.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No plans available",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.plans) { plan ->
                    PlanItem(
                        plan = plan,
                        onPurchase = { onPurchase(plan) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanItem(
    plan: TrainingPlanDTO,
    onPurchase: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = plan.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            plan.description?.let { desc ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.credits_format, plan.creditCost),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(onClick = onPurchase) {
                    Text(stringResource(R.string.purchase))
                }
            }
        }
    }
}

@Composable
private fun MyPlansTab(
    uiState: PlansUiState,
    onDownload: (String) -> Unit,
    onRetry: () -> Unit
) {
    when {
        uiState.isMyPlansLoading -> LoadingContent()
        uiState.myPlansError != null -> ErrorContent(
            message = uiState.myPlansError,
            onRetry = onRetry
        )
        uiState.myPlans.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No purchased plans yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.myPlans) { plan ->
                    PurchasedPlanItem(
                        plan = plan,
                        onDownload = { plan.pdfUrl?.let { onDownload(it) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchasedPlanItem(
    plan: PurchasedPlanDTO,
    onDownload: () -> Unit
) {
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
                    text = plan.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                plan.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (plan.pdfUrl != null) {
                IconButton(onClick = onDownload) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = stringResource(R.string.download),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
