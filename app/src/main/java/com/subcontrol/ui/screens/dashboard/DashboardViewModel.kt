package com.subcontrol.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.usecase.analytics.CalculateCostsUseCase
import com.subcontrol.domain.usecase.analytics.UpcomingCost
import com.subcontrol.domain.usecase.subscription.GetSubscriptionsUseCase
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
 * ViewModel for the dashboard screen.
 * 
 * Manages the state for cost summaries, upcoming renewals, and subscription analytics.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getSubscriptionsUseCase: GetSubscriptionsUseCase,
    private val calculateCostsUseCase: CalculateCostsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadDashboardData()
    }
    
    /**
     * Loads all dashboard data including costs and upcoming renewals.
     */
    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Combine all the data streams
                combine(
                    calculateCostsUseCase.getTotalMonthlyCost(),
                    calculateCostsUseCase.getTotalYearlyCost(),
                    calculateCostsUseCase.getCostsByCategory(),
                    calculateCostsUseCase.getUpcomingCosts(7),
                    getSubscriptionsUseCase.getActiveSubscriptions(),
                    getSubscriptionsUseCase.getTrialSubscriptions()
                ) { values ->
                    val monthlyCost = values[0] as BigDecimal
                    val yearlyCost = values[1] as BigDecimal
                    val categoryBreakdown = values[2] as Map<String, BigDecimal>
                    val upcomingCosts = values[3] as List<UpcomingCost>
                    val activeSubscriptions = values[4] as List<Subscription>
                    val trialSubscriptions = values[5] as List<Subscription>
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        monthlyCost = monthlyCost,
                        yearlyCost = yearlyCost,
                        categoryBreakdown = categoryBreakdown,
                        upcomingRenewals = upcomingCosts,
                        activeSubscriptionsCount = activeSubscriptions.size,
                        trialSubscriptionsCount = trialSubscriptions.size,
                        error = null
                    )
                }.catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Unknown error occurred"
                    )
                }.collect { }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    /**
     * Refreshes the dashboard data.
     */
    fun refresh() {
        loadDashboardData()
    }
    
    /**
     * Dismisses the current error.
     */
    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Gets the top spending categories (top 3).
     */
    fun getTopSpendingCategories(): List<CategorySpending> {
        return _uiState.value.categoryBreakdown
            .map { (category, amount) -> CategorySpending(category, amount) }
            .sortedByDescending { it.amount }
            .take(3)
    }
    
    /**
     * Gets the next renewal date from upcoming renewals.
     */
    fun getNextRenewalDate(): String? {
        return _uiState.value.upcomingRenewals.firstOrNull()?.billingDate?.toString()
    }
    
    /**
     * Gets the total amount of upcoming renewals in the next 7 days.
     */
    fun getUpcomingRenewalsTotal(): BigDecimal {
        return _uiState.value.upcomingRenewals.sumOf { it.amount }
    }
    
    /**
     * Calculates the savings compared to individual monthly subscriptions.
     */
    fun getAnnualSavings(): BigDecimal {
        val monthlyTotal = _uiState.value.monthlyCost
        val yearlyTotal = _uiState.value.yearlyCost
        val projectedMonthlyTotal = monthlyTotal.multiply(BigDecimal(12))
        
        return if (projectedMonthlyTotal > yearlyTotal) {
            projectedMonthlyTotal.subtract(yearlyTotal)
        } else {
            BigDecimal.ZERO
        }
    }
}

/**
 * Data class representing the dashboard UI state.
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val monthlyCost: BigDecimal = BigDecimal.ZERO,
    val yearlyCost: BigDecimal = BigDecimal.ZERO,
    val categoryBreakdown: Map<String, BigDecimal> = emptyMap(),
    val upcomingRenewals: List<UpcomingCost> = emptyList(),
    val activeSubscriptionsCount: Int = 0,
    val trialSubscriptionsCount: Int = 0,
    val error: String? = null
)

/**
 * Data class representing category spending.
 */
data class CategorySpending(
    val category: String,
    val amount: BigDecimal
)