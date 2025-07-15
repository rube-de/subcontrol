package com.subcontrol.ui.screens.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subcontrol.domain.model.BillingPeriod
import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.model.SubscriptionStatus
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for subscription editing (add/edit functionality).
 */
@HiltViewModel
class SubscriptionEditViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionEditUiState())
    val uiState: StateFlow<SubscriptionEditUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    init {
        loadCategories()
    }

    /**
     * Loads the subscription data for editing.
     */
    fun loadSubscription(subscriptionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            subscriptionRepository.getSubscriptionById(subscriptionId)
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load subscription: ${error.message}"
                    )
                }
                .collect { subscription ->
                    if (subscription != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            name = subscription.name,
                            description = subscription.description,
                            cost = subscription.cost.toString(),
                            currency = subscription.currency,
                            billingPeriod = subscription.billingPeriod,
                            billingCycle = subscription.billingCycle.toString(),
                            startDate = subscription.startDate,
                            category = subscription.category,
                            tags = subscription.tags.joinToString(", "),
                            notes = subscription.notes,
                            websiteUrl = subscription.websiteUrl,
                            supportEmail = subscription.supportEmail,
                            notificationsEnabled = subscription.notificationsEnabled,
                            notificationDaysBefore = subscription.notificationDaysBefore.toString(),
                            trialEndDate = subscription.trialEndDate,
                            isEditMode = true,
                            originalSubscription = subscription
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Subscription not found"
                        )
                    }
                }
        }
    }

    /**
     * Updates the subscription name.
     */
    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(
            name = name,
            nameError = if (name.isBlank()) "Name is required" else null
        )
    }

    /**
     * Updates the subscription description.
     */
    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    /**
     * Updates the subscription cost.
     */
    fun updateCost(cost: String) {
        val costError = try {
            if (cost.isNotBlank()) {
                val value = BigDecimal(cost)
                if (value < BigDecimal.ZERO) "Cost cannot be negative" else null
            } else {
                "Cost is required"
            }
        } catch (e: NumberFormatException) {
            "Invalid cost format"
        }

        _uiState.value = _uiState.value.copy(
            cost = cost,
            costError = costError
        )
    }

    /**
     * Updates the subscription currency.
     */
    fun updateCurrency(currency: String) {
        _uiState.value = _uiState.value.copy(currency = currency)
    }

    /**
     * Updates the billing period.
     */
    fun updateBillingPeriod(billingPeriod: BillingPeriod) {
        _uiState.value = _uiState.value.copy(billingPeriod = billingPeriod)
    }

    /**
     * Updates the billing cycle.
     */
    fun updateBillingCycle(billingCycle: String) {
        val cycleError = try {
            if (billingCycle.isNotBlank()) {
                val value = billingCycle.toInt()
                if (value <= 0) "Billing cycle must be positive" else null
            } else {
                null // Optional for non-custom billing periods
            }
        } catch (e: NumberFormatException) {
            "Invalid billing cycle format"
        }

        _uiState.value = _uiState.value.copy(
            billingCycle = billingCycle,
            billingCycleError = cycleError
        )
    }

    /**
     * Updates the start date.
     */
    fun updateStartDate(startDate: LocalDate) {
        _uiState.value = _uiState.value.copy(startDate = startDate)
    }

    /**
     * Updates the trial end date.
     */
    fun updateTrialEndDate(trialEndDate: LocalDate?) {
        _uiState.value = _uiState.value.copy(trialEndDate = trialEndDate)
    }

    /**
     * Updates the category.
     */
    fun updateCategory(category: String) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    /**
     * Updates the tags.
     */
    fun updateTags(tags: String) {
        _uiState.value = _uiState.value.copy(tags = tags)
    }

    /**
     * Updates the notes.
     */
    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    /**
     * Updates the website URL.
     */
    fun updateWebsiteUrl(websiteUrl: String) {
        _uiState.value = _uiState.value.copy(websiteUrl = websiteUrl)
    }

    /**
     * Updates the support email.
     */
    fun updateSupportEmail(supportEmail: String) {
        _uiState.value = _uiState.value.copy(supportEmail = supportEmail)
    }

    /**
     * Updates the notifications enabled flag.
     */
    fun updateNotificationsEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
    }

    /**
     * Updates the notification days before.
     */
    fun updateNotificationDaysBefore(daysBefore: String) {
        val daysError = try {
            if (daysBefore.isNotBlank()) {
                val value = daysBefore.toInt()
                if (value < 0) "Days before cannot be negative" else null
            } else {
                "Notification days is required"
            }
        } catch (e: NumberFormatException) {
            "Invalid days format"
        }

        _uiState.value = _uiState.value.copy(
            notificationDaysBefore = daysBefore,
            notificationDaysError = daysError
        )
    }

    /**
     * Clears any existing error messages.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Saves the subscription (add or update).
     */
    fun saveSubscription() {
        val currentState = _uiState.value
        
        // Validate all required fields
        val hasErrors = validateForm()
        if (hasErrors) return

        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            try {
                val subscription = createSubscriptionFromState(currentState)
                
                val result = if (currentState.isEditMode && currentState.originalSubscription != null) {
                    subscriptionRepository.updateSubscription(subscription)
                } else {
                    subscriptionRepository.addSubscription(subscription)
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
                            error = "Failed to save subscription: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save subscription: ${e.message}"
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

        // Validate cost
        try {
            if (currentState.cost.isBlank()) {
                _uiState.value = _uiState.value.copy(costError = "Cost is required")
                hasErrors = true
            } else {
                val costValue = BigDecimal(currentState.cost)
                if (costValue < BigDecimal.ZERO) {
                    _uiState.value = _uiState.value.copy(costError = "Cost cannot be negative")
                    hasErrors = true
                }
            }
        } catch (e: NumberFormatException) {
            _uiState.value = _uiState.value.copy(costError = "Invalid cost format")
            hasErrors = true
        }

        // Validate billing cycle for custom periods
        if (currentState.billingPeriod == BillingPeriod.CUSTOM) {
            try {
                if (currentState.billingCycle.isBlank()) {
                    _uiState.value = _uiState.value.copy(billingCycleError = "Billing cycle is required for custom period")
                    hasErrors = true
                } else {
                    val cycleValue = currentState.billingCycle.toInt()
                    if (cycleValue <= 0) {
                        _uiState.value = _uiState.value.copy(billingCycleError = "Billing cycle must be positive")
                        hasErrors = true
                    }
                }
            } catch (e: NumberFormatException) {
                _uiState.value = _uiState.value.copy(billingCycleError = "Invalid billing cycle format")
                hasErrors = true
            }
        }

        // Validate notification days
        if (currentState.notificationsEnabled) {
            try {
                if (currentState.notificationDaysBefore.isBlank()) {
                    _uiState.value = _uiState.value.copy(notificationDaysError = "Notification days is required")
                    hasErrors = true
                } else {
                    val daysValue = currentState.notificationDaysBefore.toInt()
                    if (daysValue < 0) {
                        _uiState.value = _uiState.value.copy(notificationDaysError = "Days before cannot be negative")
                        hasErrors = true
                    }
                }
            } catch (e: NumberFormatException) {
                _uiState.value = _uiState.value.copy(notificationDaysError = "Invalid days format")
                hasErrors = true
            }
        }

        return hasErrors
    }

    private fun createSubscriptionFromState(state: SubscriptionEditUiState): Subscription {
        val now = LocalDateTime.now()
        val tags = state.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val billingCycle = if (state.billingPeriod == BillingPeriod.CUSTOM) {
            state.billingCycle.toIntOrNull() ?: 1
        } else {
            1
        }

        // Calculate next billing date based on start date and billing period
        val nextBillingDate = calculateNextBillingDate(state.startDate, state.billingPeriod, billingCycle)

        return if (state.isEditMode && state.originalSubscription != null) {
            state.originalSubscription.copy(
                name = state.name,
                description = state.description,
                cost = BigDecimal(state.cost),
                currency = state.currency,
                billingPeriod = state.billingPeriod,
                billingCycle = billingCycle,
                startDate = state.startDate,
                nextBillingDate = nextBillingDate,
                trialEndDate = state.trialEndDate,
                notificationsEnabled = state.notificationsEnabled,
                notificationDaysBefore = state.notificationDaysBefore.toIntOrNull() ?: 3,
                category = state.category,
                tags = tags,
                notes = state.notes,
                websiteUrl = state.websiteUrl,
                supportEmail = state.supportEmail,
                updatedAt = now
            )
        } else {
            Subscription(
                id = UUID.randomUUID().toString(),
                name = state.name,
                description = state.description,
                cost = BigDecimal(state.cost),
                currency = state.currency,
                billingPeriod = state.billingPeriod,
                billingCycle = billingCycle,
                startDate = state.startDate,
                nextBillingDate = nextBillingDate,
                trialEndDate = state.trialEndDate,
                status = if (state.trialEndDate != null && LocalDate.now().isBefore(state.trialEndDate)) {
                    SubscriptionStatus.TRIAL
                } else {
                    SubscriptionStatus.ACTIVE
                },
                notificationsEnabled = state.notificationsEnabled,
                notificationDaysBefore = state.notificationDaysBefore.toIntOrNull() ?: 3,
                category = state.category,
                tags = tags,
                notes = state.notes,
                websiteUrl = state.websiteUrl,
                supportEmail = state.supportEmail,
                createdAt = now,
                updatedAt = now
            )
        }
    }

    private fun calculateNextBillingDate(startDate: LocalDate, billingPeriod: BillingPeriod, billingCycle: Int): LocalDate {
        return when (billingPeriod) {
            BillingPeriod.DAILY -> startDate.plusDays(billingCycle.toLong())
            BillingPeriod.WEEKLY -> startDate.plusWeeks(billingCycle.toLong())
            BillingPeriod.MONTHLY -> startDate.plusMonths(billingCycle.toLong())
            BillingPeriod.QUARTERLY -> startDate.plusMonths(3L * billingCycle)
            BillingPeriod.SEMI_ANNUALLY -> startDate.plusMonths(6L * billingCycle)
            BillingPeriod.ANNUALLY -> startDate.plusYears(billingCycle.toLong())
            BillingPeriod.CUSTOM -> startDate.plusDays(billingCycle.toLong())
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories()
                .catch { error ->
                    // Use default categories if loading fails
                    _categories.value = com.subcontrol.domain.model.Category.defaultCategories
                }
                .collect { categories ->
                    _categories.value = categories.map { it.name }.ifEmpty {
                        com.subcontrol.domain.model.Category.defaultCategories
                    }
                }
        }
    }
}

/**
 * UI state for subscription editing.
 */
data class SubscriptionEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val originalSubscription: Subscription? = null,
    
    // Form fields
    val name: String = "",
    val nameError: String? = null,
    val description: String = "",
    val cost: String = "",
    val costError: String? = null,
    val currency: String = "USD",
    val billingPeriod: BillingPeriod = BillingPeriod.MONTHLY,
    val billingCycle: String = "1",
    val billingCycleError: String? = null,
    val startDate: LocalDate = LocalDate.now(),
    val trialEndDate: LocalDate? = null,
    val category: String = "",
    val tags: String = "",
    val notes: String = "",
    val websiteUrl: String = "",
    val supportEmail: String = "",
    val notificationsEnabled: Boolean = true,
    val notificationDaysBefore: String = "3",
    val notificationDaysError: String? = null
) {
    val isFormValid: Boolean
        get() = name.isNotBlank() && 
                cost.isNotBlank() && 
                nameError == null && 
                costError == null && 
                billingCycleError == null && 
                notificationDaysError == null
}