package com.subcontrol.ui.screens.subscription

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.subcontrol.domain.model.BillingPeriod
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Screen for adding or editing subscriptions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionEditScreen(
    subscriptionId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load subscription data if editing
    LaunchedEffect(subscriptionId) {
        if (subscriptionId != null) {
            viewModel.loadSubscription(subscriptionId)
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
                        text = if (uiState.isEditMode) "Edit Subscription" else "Add Subscription"
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
                    onClick = { viewModel.saveSubscription() },
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
            SubscriptionEditForm(
                uiState = uiState,
                categories = categories,
                onNameChange = viewModel::updateName,
                onDescriptionChange = viewModel::updateDescription,
                onCostChange = viewModel::updateCost,
                onCurrencyChange = viewModel::updateCurrency,
                onBillingPeriodChange = viewModel::updateBillingPeriod,
                onBillingCycleChange = viewModel::updateBillingCycle,
                onStartDateChange = viewModel::updateStartDate,
                onTrialEndDateChange = viewModel::updateTrialEndDate,
                onCategoryChange = viewModel::updateCategory,
                onTagsChange = viewModel::updateTags,
                onNotesChange = viewModel::updateNotes,
                onWebsiteUrlChange = viewModel::updateWebsiteUrl,
                onSupportEmailChange = viewModel::updateSupportEmail,
                onNotificationsEnabledChange = viewModel::updateNotificationsEnabled,
                onNotificationDaysBeforeChange = viewModel::updateNotificationDaysBefore,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionEditForm(
    uiState: SubscriptionEditUiState,
    categories: List<String>,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCostChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onBillingPeriodChange: (BillingPeriod) -> Unit,
    onBillingCycleChange: (String) -> Unit,
    onStartDateChange: (LocalDate) -> Unit,
    onTrialEndDateChange: (LocalDate?) -> Unit,
    onCategoryChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onWebsiteUrlChange: (String) -> Unit,
    onSupportEmailChange: (String) -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onNotificationDaysBeforeChange: (String) -> Unit,
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
                    label = { Text("Subscription Name *") },
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.cost,
                        onValueChange = onCostChange,
                        label = { Text("Cost *") },
                        isError = uiState.costError != null,
                        supportingText = uiState.costError?.let { { Text(it) } },
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

        // Billing Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Billing",
                    style = MaterialTheme.typography.titleMedium
                )

                BillingPeriodSelector(
                    selectedPeriod = uiState.billingPeriod,
                    onPeriodSelected = onBillingPeriodChange
                )

                if (uiState.billingPeriod == BillingPeriod.CUSTOM) {
                    OutlinedTextField(
                        value = uiState.billingCycle,
                        onValueChange = onBillingCycleChange,
                        label = { Text("Billing Cycle (days) *") },
                        isError = uiState.billingCycleError != null,
                        supportingText = uiState.billingCycleError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                DatePickerField(
                    label = "Start Date",
                    selectedDate = uiState.startDate,
                    onDateSelected = onStartDateChange
                )

                DatePickerField(
                    label = "Trial End Date (Optional)",
                    selectedDate = uiState.trialEndDate,
                    onDateSelected = onTrialEndDateChange,
                    allowNull = true
                )
            }
        }

        // Organization Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Organization",
                    style = MaterialTheme.typography.titleMedium
                )

                CategoryDropdown(
                    selectedCategory = uiState.category,
                    categories = categories,
                    onCategorySelected = onCategoryChange
                )

                OutlinedTextField(
                    value = uiState.tags,
                    onValueChange = onTagsChange,
                    label = { Text("Tags (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }
        }

        // Contact Information Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Contact Information",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = uiState.websiteUrl,
                    onValueChange = onWebsiteUrlChange,
                    label = { Text("Website URL") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.supportEmail,
                    onValueChange = onSupportEmailChange,
                    label = { Text("Support Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
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
                    Text("Enable Notifications")
                    Switch(
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = onNotificationsEnabledChange
                    )
                }

                if (uiState.notificationsEnabled) {
                    OutlinedTextField(
                        value = uiState.notificationDaysBefore,
                        onValueChange = onNotificationDaysBeforeChange,
                        label = { Text("Notify Days Before *") },
                        isError = uiState.notificationDaysError != null,
                        supportingText = uiState.notificationDaysError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selectedCategory: String,
    categories: List<String>,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCategory,
            onValueChange = { },
            readOnly = true,
            label = { Text("Category") },
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
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun BillingPeriodSelector(
    selectedPeriod: BillingPeriod,
    onPeriodSelected: (BillingPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Billing Period",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val periods = listOf(
            BillingPeriod.DAILY to "Daily",
            BillingPeriod.WEEKLY to "Weekly",
            BillingPeriod.MONTHLY to "Monthly",
            BillingPeriod.QUARTERLY to "Quarterly",
            BillingPeriod.SEMI_ANNUALLY to "Semi-annually",
            BillingPeriod.ANNUALLY to "Annually",
            BillingPeriod.CUSTOM to "Custom"
        )

        periods.chunked(2).forEach { rowPeriods ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowPeriods.forEach { (period, label) ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(
                                selected = selectedPeriod == period,
                                onClick = { onPeriodSelected(period) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPeriod == period,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    allowNull: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    )

    OutlinedTextField(
        value = selectedDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "",
        onValueChange = { },
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Select date")
            }
        },
        modifier = modifier.fillMaxWidth()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onDateSelected(date)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                if (allowNull) {
                    TextButton(
                        onClick = {
                            onDateSelected(null)
                            showDatePicker = false
                        }
                    ) {
                        Text("Clear")
                    }
                }
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}