package com.subcontrol.ui.screens.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.model.SubscriptionStatus
import com.subcontrol.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * ViewModel for the subscription list screen.
 */
@HiltViewModel
class SubscriptionListViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionListUiState())
    val uiState: StateFlow<SubscriptionListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterStatus = MutableStateFlow(FilterStatus.ALL)
    val filterStatus: StateFlow<FilterStatus> = _filterStatus.asStateFlow()

    init {
        loadSubscriptions()
    }

    /**
     * Updates the search query and filters subscriptions.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        loadSubscriptions()
    }

    /**
     * Updates the filter status and reloads subscriptions.
     */
    fun updateFilterStatus(status: FilterStatus) {
        _filterStatus.value = status
        loadSubscriptions()
    }

    /**
     * Deletes a subscription.
     */
    fun deleteSubscription(subscriptionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            subscriptionRepository.deleteSubscription(subscriptionId)
                .fold(
                    onSuccess = {
                        // Reload subscriptions after successful deletion
                        loadSubscriptions()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to delete subscription: ${error.message}"
                        )
                    }
                )
        }
    }

    /**
     * Updates the status of a subscription.
     */
    fun updateSubscriptionStatus(subscriptionId: String, status: SubscriptionStatus) {
        viewModelScope.launch {
            subscriptionRepository.updateStatus(subscriptionId, status)
                .fold(
                    onSuccess = {
                        // Status updated successfully - list will automatically refresh via Flow
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to update subscription status: ${error.message}"
                        )
                    }
                )
        }
    }

    /**
     * Clears any existing error messages.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Refreshes the subscription list.
     */
    fun refresh() {
        loadSubscriptions()
    }

    private fun loadSubscriptions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            combine(
                if (_searchQuery.value.isBlank()) {
                    subscriptionRepository.getAllSubscriptions()
                } else {
                    subscriptionRepository.searchSubscriptions(_searchQuery.value)
                },
                _filterStatus
            ) { subscriptions, filterStatus ->
                val filteredSubscriptions = when (filterStatus) {
                    FilterStatus.ALL -> subscriptions
                    FilterStatus.ACTIVE -> subscriptions.filter { it.status == SubscriptionStatus.ACTIVE }
                    FilterStatus.TRIAL -> subscriptions.filter { it.status == SubscriptionStatus.TRIAL }
                    FilterStatus.PAUSED -> subscriptions.filter { it.status == SubscriptionStatus.PAUSED }
                    FilterStatus.CANCELLED -> subscriptions.filter { it.status == SubscriptionStatus.CANCELLED }
                    FilterStatus.EXPIRED -> subscriptions.filter { it.status == SubscriptionStatus.EXPIRED }
                }

                val totalMonthlyCost = calculateTotalMonthlyCost(filteredSubscriptions)
                val totalAnnualCost = calculateTotalAnnualCost(filteredSubscriptions)

                SubscriptionListUiState(
                    isLoading = false,
                    subscriptions = filteredSubscriptions,
                    totalMonthlyCost = totalMonthlyCost,
                    totalAnnualCost = totalAnnualCost,
                    isEmpty = filteredSubscriptions.isEmpty(),
                    error = null
                )
            }
            .catch { error ->
                _uiState.value = SubscriptionListUiState(
                    isLoading = false,
                    error = "Failed to load subscriptions: ${error.message}"
                )
            }
            .collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun calculateTotalMonthlyCost(subscriptions: List<Subscription>): BigDecimal {
        return subscriptions
            .filter { it.isActive }
            .sumOf { it.getMonthlyEquivalent() }
    }

    private fun calculateTotalAnnualCost(subscriptions: List<Subscription>): BigDecimal {
        return subscriptions
            .filter { it.isActive }
            .sumOf { it.getAnnualCost() }
    }
}

/**
 * UI state for the subscription list screen.
 */
data class SubscriptionListUiState(
    val isLoading: Boolean = false,
    val subscriptions: List<Subscription> = emptyList(),
    val totalMonthlyCost: BigDecimal = BigDecimal.ZERO,
    val totalAnnualCost: BigDecimal = BigDecimal.ZERO,
    val isEmpty: Boolean = false,
    val error: String? = null
)

/**
 * Filter options for subscription status.
 */
enum class FilterStatus {
    ALL,
    ACTIVE,
    TRIAL,
    PAUSED,
    CANCELLED,
    EXPIRED
}