package com.subcontrol.ui.screens.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subcontrol.domain.model.Budget
import com.subcontrol.domain.model.Category
import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.repository.BudgetRepository
import com.subcontrol.domain.repository.CategoryRepository
import com.subcontrol.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for budget editing (add/edit functionality).
 */
@HiltViewModel
class BudgetEditViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetEditUiState())
    val uiState: StateFlow<BudgetEditUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _subscriptions = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptions: StateFlow<List<Subscription>> = _subscriptions.asStateFlow()

    init {
        loadCategoriesAndSubscriptions()
    }

    /**
     * Loads the budget data for editing.
     */
    fun loadBudget(budgetId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            budgetRepository.getBudgetById(budgetId)
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load budget: ${error.message}"
                    )
                }
                .collect { budget ->
                    if (budget != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            name = budget.name,
                            monthlyLimit = budget.monthlyLimit.toString(),
                            currency = budget.currency,
                            includedCategories = budget.includedCategories.toSet(),
                            includedSubscriptions = budget.includedSubscriptions.toSet(),
                            notificationsEnabled = budget.notificationsEnabled,
                            notificationThreshold = (budget.notificationThreshold * 100).toString(),
                            isEditMode = true,
                            originalBudget = budget
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Budget not found"
                        )
                    }
                }
        }
    }

    /**
     * Updates the budget name.
     */
    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(
            name = name,
            nameError = if (name.isBlank()) "Name is required" else null
        )
    }

    /**
     * Updates the monthly limit.
     */
    fun updateMonthlyLimit(monthlyLimit: String) {
        val limitError = try {
            if (monthlyLimit.isNotBlank()) {
                val value = BigDecimal(monthlyLimit)
                if (value < BigDecimal.ZERO) "Monthly limit cannot be negative" else null
            } else {
                "Monthly limit is required"
            }
        } catch (e: NumberFormatException) {
            "Invalid monthly limit format"
        }

        _uiState.value = _uiState.value.copy(
            monthlyLimit = monthlyLimit,
            monthlyLimitError = limitError
        )
    }

    /**
     * Updates the currency.
     */
    fun updateCurrency(currency: String) {
        _uiState.value = _uiState.value.copy(currency = currency)
    }

    /**
     * Toggles inclusion of a category.
     */
    fun toggleCategory(categoryId: String) {
        val currentCategories = _uiState.value.includedCategories.toMutableSet()
        if (currentCategories.contains(categoryId)) {
            currentCategories.remove(categoryId)
        } else {
            currentCategories.add(categoryId)
        }
        _uiState.value = _uiState.value.copy(includedCategories = currentCategories)
    }

    /**
     * Toggles inclusion of a subscription.
     */
    fun toggleSubscription(subscriptionId: String) {
        val currentSubscriptions = _uiState.value.includedSubscriptions.toMutableSet()
        if (currentSubscriptions.contains(subscriptionId)) {
            currentSubscriptions.remove(subscriptionId)
        } else {
            currentSubscriptions.add(subscriptionId)
        }
        _uiState.value = _uiState.value.copy(includedSubscriptions = currentSubscriptions)
    }

    /**
     * Updates the notifications enabled flag.
     */
    fun updateNotificationsEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
    }

    /**
     * Updates the notification threshold.
     */
    fun updateNotificationThreshold(threshold: String) {
        val thresholdError = try {
            if (threshold.isNotBlank()) {
                val value = threshold.toDouble()
                if (value < 0 || value > 100) "Threshold must be between 0 and 100" else null
            } else {
                "Notification threshold is required"
            }
        } catch (e: NumberFormatException) {
            "Invalid threshold format"
        }

        _uiState.value = _uiState.value.copy(
            notificationThreshold = threshold,
            notificationThresholdError = thresholdError
        )
    }

    /**
     * Clears any existing error messages.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Saves the budget (add or update).
     */
    fun saveBudget() {
        val currentState = _uiState.value
        
        // Validate all required fields
        val hasErrors = validateForm()
        if (hasErrors) return

        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            try {
                val budget = createBudgetFromState(currentState)
                
                val result = if (currentState.isEditMode && currentState.originalBudget != null) {
                    budgetRepository.updateBudget(budget)
                } else {
                    budgetRepository.addBudget(budget)
                }

                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            isSaved = true
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            error = "Failed to save budget: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save budget: ${e.message}"
                )
            }
        }
    }

    private fun validateForm(): Boolean {
        val currentState = _uiState.value
        var hasErrors = false

        // Validate name
        if (currentState.name.isBlank()) {
            _uiState.value = _uiState.value.copy(nameError = "Name is required")
            hasErrors = true
        }

        // Validate monthly limit
        try {
            if (currentState.monthlyLimit.isBlank()) {
                _uiState.value = _uiState.value.copy(monthlyLimitError = "Monthly limit is required")
                hasErrors = true
            } else {
                val limitValue = BigDecimal(currentState.monthlyLimit)
                if (limitValue < BigDecimal.ZERO) {
                    _uiState.value = _uiState.value.copy(monthlyLimitError = "Monthly limit cannot be negative")
                    hasErrors = true
                }
            }
        } catch (e: NumberFormatException) {
            _uiState.value = _uiState.value.copy(monthlyLimitError = "Invalid monthly limit format")
            hasErrors = true
        }

        // Validate notification threshold
        if (currentState.notificationsEnabled) {
            try {
                if (currentState.notificationThreshold.isBlank()) {
                    _uiState.value = _uiState.value.copy(notificationThresholdError = "Notification threshold is required")
                    hasErrors = true
                } else {
                    val thresholdValue = currentState.notificationThreshold.toDouble()
                    if (thresholdValue < 0 || thresholdValue > 100) {
                        _uiState.value = _uiState.value.copy(notificationThresholdError = "Threshold must be between 0 and 100")
                        hasErrors = true
                    }
                }
            } catch (e: NumberFormatException) {
                _uiState.value = _uiState.value.copy(notificationThresholdError = "Invalid threshold format")
                hasErrors = true
            }
        }

        return hasErrors
    }

    private fun createBudgetFromState(state: BudgetEditUiState): Budget {
        val now = LocalDateTime.now()
        val notificationThreshold = state.notificationThreshold.toDoubleOrNull()?.div(100) ?: 0.8

        return if (state.isEditMode && state.originalBudget != null) {
            state.originalBudget.copy(
                name = state.name,
                monthlyLimit = BigDecimal(state.monthlyLimit),
                currency = state.currency,
                includedCategories = state.includedCategories.toList(),
                includedSubscriptions = state.includedSubscriptions.toList(),
                notificationsEnabled = state.notificationsEnabled,
                notificationThreshold = notificationThreshold,
                updatedAt = now
            )
        } else {
            Budget(
                id = UUID.randomUUID().toString(),
                name = state.name,
                monthlyLimit = BigDecimal(state.monthlyLimit),
                currency = state.currency,
                includedCategories = state.includedCategories.toList(),
                includedSubscriptions = state.includedSubscriptions.toList(),
                notificationsEnabled = state.notificationsEnabled,
                notificationThreshold = notificationThreshold,
                createdAt = now,
                updatedAt = now
            )
        }
    }

    private fun loadCategoriesAndSubscriptions() {
        viewModelScope.launch {
            combine(
                categoryRepository.getAllCategories(),
                subscriptionRepository.getAllSubscriptions()
            ) { categories, subscriptions ->
                _categories.value = categories
                _subscriptions.value = subscriptions
            }.catch { error ->
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load data: ${error.message}"
                )
            }.collect { }
        }
    }
}

/**
 * UI state for budget editing.
 */
data class BudgetEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val originalBudget: Budget? = null,
    
    // Form fields
    val name: String = "",
    val nameError: String? = null,
    val monthlyLimit: String = "",
    val monthlyLimitError: String? = null,
    val currency: String = "USD",
    val includedCategories: Set<String> = emptySet(),
    val includedSubscriptions: Set<String> = emptySet(),
    val notificationsEnabled: Boolean = true,
    val notificationThreshold: String = "80",
    val notificationThresholdError: String? = null
) {
    val isFormValid: Boolean
        get() = name.isNotBlank() && 
                monthlyLimit.isNotBlank() && 
                nameError == null && 
                monthlyLimitError == null && 
                notificationThresholdError == null
}