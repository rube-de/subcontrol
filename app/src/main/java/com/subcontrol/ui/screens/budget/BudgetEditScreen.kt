package com.subcontrol.ui.screens.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.subcontrol.domain.model.Category
import com.subcontrol.domain.model.Subscription

/**
 * Screen for adding or editing budgets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEditScreen(
    budgetId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load budget data if editing
    LaunchedEffect(budgetId) {
        if (budgetId != null) {
            viewModel.loadBudget(budgetId)
        }
    }

    // Handle save success
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

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
                title = { 
                    Text(
                        text = if (uiState.isEditMode) "Edit Budget" else "Add Budget"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.saveBudget() },
                    icon = {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.width(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                        }
                    },
                    text = { Text(if (uiState.isSaving) "Saving..." else "Save") },
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            BudgetEditForm(
                uiState = uiState,
                categories = categories,
                subscriptions = subscriptions,
                onNameChange = viewModel::updateName,
                onMonthlyLimitChange = viewModel::updateMonthlyLimit,
                onCurrencyChange = viewModel::updateCurrency,
                onToggleCategory = viewModel::toggleCategory,
                onToggleSubscription = viewModel::toggleSubscription,
                onNotificationsEnabledChange = viewModel::updateNotificationsEnabled,
                onNotificationThresholdChange = viewModel::updateNotificationThreshold,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetEditForm(
    uiState: BudgetEditUiState,
    categories: List<Category>,
    subscriptions: List<Subscription>,
    onNameChange: (String) -> Unit,
    onMonthlyLimitChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onToggleCategory: (String) -> Unit,
    onToggleSubscription: (String) -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onNotificationThresholdChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Basic Information Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Basic Information",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    label = { Text("Budget Name *") },
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.monthlyLimit,
                        onValueChange = onMonthlyLimitChange,
                        label = { Text("Monthly Limit *") },
                        isError = uiState.monthlyLimitError != null,
                        supportingText = uiState.monthlyLimitError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )

                    CurrencyDropdown(
                        selectedCurrency = uiState.currency,
                        onCurrencySelected = onCurrencyChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Scope Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Budget Scope",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Select which categories and subscriptions to include in this budget. Leave empty to include all.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (categories.isNotEmpty()) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.labelLarge
                    )

                    CategorySelectionList(
                        categories = categories,
                        selectedCategories = uiState.includedCategories,
                        onToggleCategory = onToggleCategory
                    )
                }

                if (subscriptions.isNotEmpty()) {
                    Text(
                        text = "Subscriptions",
                        style = MaterialTheme.typography.labelLarge
                    )

                    SubscriptionSelectionList(
                        subscriptions = subscriptions,
                        selectedSubscriptions = uiState.includedSubscriptions,
                        onToggleSubscription = onToggleSubscription
                    )
                }
            }
        }

        // Notifications Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Budget Notifications")
                    Switch(
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = onNotificationsEnabledChange
                    )
                }

                if (uiState.notificationsEnabled) {
                    OutlinedTextField(
                        value = uiState.notificationThreshold,
                        onValueChange = onNotificationThresholdChange,
                        label = { Text("Notification Threshold (%) *") },
                        isError = uiState.notificationThresholdError != null,
                        supportingText = uiState.notificationThresholdError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "You'll be notified when spending reaches this percentage of your budget limit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Add bottom padding for FAB
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val currencies = listOf("USD", "EUR", "GBP", "CAD", "AUD", "JPY", "CHF", "CNY")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCurrency,
            onValueChange = { },
            readOnly = true,
            label = { Text("Currency") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = {
                        onCurrencySelected(currency)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CategorySelectionList(
    categories: List<Category>,
    selectedCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectedCategories.contains(category.id),
                    onCheckedChange = { onToggleCategory(category.id) }
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SubscriptionSelectionList(
    subscriptions: List<Subscription>,
    selectedSubscriptions: Set<String>,
    onToggleSubscription: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        subscriptions.forEach { subscription ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectedSubscriptions.contains(subscription.id),
                    onCheckedChange = { onToggleSubscription(subscription.id) }
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = subscription.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = "${subscription.cost} ${subscription.currency} / ${subscription.billingPeriod.name.lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}