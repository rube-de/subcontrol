package com.subcontrol.ui.screens.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.model.SubscriptionStatus
import java.time.format.DateTimeFormatter

/**
 * Screen for displaying the list of subscriptions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionListScreen(
    onNavigateToAddSubscription: () -> Unit,
    onNavigateToEditSubscription: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscriptions") }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddSubscription,
                icon = {
                    Icon(Icons.Default.Add, contentDescription = null)
                },
                text = { Text("Add Subscription") },
                modifier = Modifier.padding(16.dp)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search and Filter Section
            SearchAndFilterSection(
                searchQuery = searchQuery,
                filterStatus = filterStatus,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onFilterStatusChange = viewModel::updateFilterStatus,
                modifier = Modifier.padding(16.dp)
            )

            // Summary Section
            if (!uiState.isEmpty && !uiState.isLoading) {
                SummarySection(
                    totalMonthlyCost = uiState.totalMonthlyCost,
                    totalAnnualCost = uiState.totalAnnualCost,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Content Section
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.isEmpty -> {
                    EmptyState(
                        onAddSubscription = onNavigateToAddSubscription,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    SubscriptionList(
                        subscriptions = uiState.subscriptions,
                        onEditSubscription = onNavigateToEditSubscription,
                        onDeleteSubscription = viewModel::deleteSubscription,
                        onUpdateStatus = viewModel::updateSubscriptionStatus,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchAndFilterSection(
    searchQuery: String,
    filterStatus: FilterStatus,
    onSearchQueryChange: (String) -> Unit,
    onFilterStatusChange: (FilterStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search subscriptions") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(FilterStatus.values()) { status ->
                FilterChip(
                    selected = filterStatus == status,
                    onClick = { onFilterStatusChange(status) },
                    label = { 
                        Text(
                            text = when (status) {
                                FilterStatus.ALL -> "All"
                                FilterStatus.ACTIVE -> "Active"
                                FilterStatus.TRIAL -> "Trial"
                                FilterStatus.PAUSED -> "Paused"
                                FilterStatus.CANCELLED -> "Cancelled"
                                FilterStatus.EXPIRED -> "Expired"
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SummarySection(
    totalMonthlyCost: java.math.BigDecimal,
    totalAnnualCost: java.math.BigDecimal,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Monthly Total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$${totalMonthlyCost}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Annual Total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$${totalAnnualCost}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SubscriptionList(
    subscriptions: List<Subscription>,
    onEditSubscription: (String) -> Unit,
    onDeleteSubscription: (String) -> Unit,
    onUpdateStatus: (String, SubscriptionStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(subscriptions, key = { it.id }) { subscription ->
            SubscriptionItem(
                subscription = subscription,
                onEdit = { onEditSubscription(subscription.id) },
                onDelete = { onDeleteSubscription(subscription.id) },
                onUpdateStatus = { status -> onUpdateStatus(subscription.id, status) }
            )
        }
        
        // Add bottom padding for FAB
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SubscriptionItem(
    subscription: Subscription,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdateStatus: (SubscriptionStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(getStatusColor(subscription.status))
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Subscription Name
                Text(
                    text = subscription.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Actions Menu
                Box {
                    IconButton(onClick = { showDropdown = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                onEdit()
                                showDropdown = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                        
                        if (subscription.status == SubscriptionStatus.ACTIVE) {
                            DropdownMenuItem(
                                text = { Text("Pause") },
                                onClick = {
                                    onUpdateStatus(SubscriptionStatus.PAUSED)
                                    showDropdown = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Pause, contentDescription = null)
                                }
                            )
                        } else if (subscription.status == SubscriptionStatus.PAUSED) {
                            DropdownMenuItem(
                                text = { Text("Resume") },
                                onClick = {
                                    onUpdateStatus(SubscriptionStatus.ACTIVE)
                                    showDropdown = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                }
                            )
                        }
                        
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                onDelete()
                                showDropdown = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Cost and Billing Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${subscription.cost} ${subscription.currency}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "per ${subscription.billingPeriod.name.lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Next billing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = subscription.nextBillingDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Category and Status
            if (subscription.category.isNotEmpty() || subscription.isInTrial) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (subscription.category.isNotEmpty()) {
                        Text(
                            text = subscription.category,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    if (subscription.isInTrial) {
                        Text(
                            text = "Trial",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    onAddSubscription: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No subscriptions yet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Start tracking your subscriptions to better manage your expenses",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        ExtendedFloatingActionButton(
            onClick = onAddSubscription,
            icon = {
                Icon(Icons.Default.Add, contentDescription = null)
            },
            text = { Text("Add Your First Subscription") }
        )
    }
}

@Composable
private fun getStatusColor(status: SubscriptionStatus): Color {
    return when (status) {
        SubscriptionStatus.ACTIVE -> Color(0xFF4CAF50) // Green
        SubscriptionStatus.TRIAL -> Color(0xFF2196F3) // Blue  
        SubscriptionStatus.PAUSED -> Color(0xFFFF9800) // Orange
        SubscriptionStatus.CANCELLED -> Color(0xFF9E9E9E) // Gray
        SubscriptionStatus.EXPIRED -> Color(0xFFF44336) // Red
    }
}