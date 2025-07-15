package com.subcontrol.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.subcontrol.R
import com.subcontrol.domain.usecase.analytics.UpcomingCost
import com.subcontrol.ui.screens.dashboard.components.CostSummaryCard
import com.subcontrol.ui.screens.dashboard.components.UpcomingRenewalsCard
import com.subcontrol.ui.theme.SubControlTheme
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Dashboard screen that displays subscription cost summaries and upcoming renewals.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Handle pull-to-refresh
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            viewModel.refresh()
            pullToRefreshState.endRefresh()
        }
    }
    
    // Handle error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            viewModel.dismissError()
        }
    }
    
    Scaffold(
        topBar = {
            DashboardTopBar(
                onRefreshClick = { viewModel.refresh() }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        modifier = modifier.nestedScroll(pullToRefreshState.nestedScrollConnection)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                else -> {
                    DashboardContent(
                        uiState = uiState,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    )
                }
            }
            
            PullToRefreshContainer(
                modifier = Modifier.align(Alignment.TopCenter),
                state = pullToRefreshState,
            )
        }
    }
}

/**
 * Top app bar for the dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.dashboard),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            IconButton(onClick = onRefreshClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.refresh)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    )
}

/**
 * Main content of the dashboard.
 */
@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick stats row
        QuickStatsRow(
            activeSubscriptions = uiState.activeSubscriptionsCount,
            trialSubscriptions = uiState.trialSubscriptionsCount
        )
        
        // Cost summary card
        CostSummaryCard(
            monthlyCost = uiState.monthlyCost,
            yearlyCost = uiState.yearlyCost,
            currency = "USD"
        )
        
        // Upcoming renewals card
        UpcomingRenewalsCard(
            upcomingRenewals = uiState.upcomingRenewals,
            currency = "USD"
        )
        
        // Category breakdown if available
        if (uiState.categoryBreakdown.isNotEmpty()) {
            CategoryBredownCard(
                categoryBreakdown = uiState.categoryBreakdown,
                currency = "USD"
            )
        }
    }
}

/**
 * Quick stats row showing subscription counts.
 */
@Composable
private fun QuickStatsRow(
    activeSubscriptions: Int,
    trialSubscriptions: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickStatCard(
            title = stringResource(R.string.active_subscriptions),
            value = activeSubscriptions.toString(),
            modifier = Modifier.weight(1f)
        )
        
        QuickStatCard(
            title = stringResource(R.string.trial_subscriptions),
            value = trialSubscriptions.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Individual quick stat card.
 */
@Composable
private fun QuickStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Category breakdown card.
 */
@Composable
private fun CategoryBredownCard(
    categoryBreakdown: Map<String, BigDecimal>,
    currency: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.spending_by_category),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            categoryBreakdown.entries.take(5).forEach { (category, amount) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = formatCurrency(amount, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Loading state component.
 */
@Composable
private fun LoadingState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.loading_dashboard),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Formats a BigDecimal amount as currency.
 */
private fun formatCurrency(amount: BigDecimal, currencyCode: String): String {
    return try {
        val formatter = java.text.NumberFormat.getCurrencyInstance()
        formatter.currency = java.util.Currency.getInstance(currencyCode)
        formatter.format(amount)
    } catch (e: Exception) {
        "$currencyCode ${amount.setScale(2)}"
    }
}

@Preview
@Composable
private fun DashboardScreenPreview() {
    SubControlTheme {
        DashboardContent(
            uiState = DashboardUiState(
                monthlyCost = BigDecimal("49.97"),
                yearlyCost = BigDecimal("599.64"),
                categoryBreakdown = mapOf(
                    "Entertainment" to BigDecimal("21.98"),
                    "Productivity" to BigDecimal("19.99"),
                    "Music" to BigDecimal("8.00")
                ),
                upcomingRenewals = listOf(
                    UpcomingCost(
                        subscriptionId = "1",
                        subscriptionName = "Netflix",
                        amount = BigDecimal("9.99"),
                        currency = "USD",
                        billingDate = LocalDate.now().plusDays(1),
                        daysUntilBilling = 1
                    ),
                    UpcomingCost(
                        subscriptionId = "2",
                        subscriptionName = "Spotify Premium",
                        amount = BigDecimal("11.99"),
                        currency = "USD",
                        billingDate = LocalDate.now().plusDays(3),
                        daysUntilBilling = 3
                    )
                ),
                activeSubscriptionsCount = 5,
                trialSubscriptionsCount = 2
            )
        )
    }
}