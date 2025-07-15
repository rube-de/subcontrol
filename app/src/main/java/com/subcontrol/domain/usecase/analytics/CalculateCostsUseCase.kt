package com.subcontrol.domain.usecase.analytics

import com.subcontrol.domain.model.BillingPeriod
import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.model.SubscriptionStatus
import com.subcontrol.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Use case for calculating various subscription cost metrics.
 * 
 * This use case provides business logic for cost calculations,
 * including monthly/yearly totals, category breakdowns, and projections.
 */
class CalculateCostsUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    
    /**
     * Calculates the total monthly cost of all active subscriptions.
     * 
     * @param currency Target currency for calculations (default: USD)
     * @return Flow of total monthly cost
     */
    fun getTotalMonthlyCost(currency: String = "USD"): Flow<BigDecimal> {
        return repository.getActiveSubscriptions().map { subscriptions ->
            subscriptions.sumOf { subscription ->
                if (subscription.currency == currency) {
                    subscription.getMonthlyEquivalent()
                } else {
                    // For now, return zero for different currencies
                    // In a real app, you would implement currency conversion
                    BigDecimal.ZERO
                }
            }
        }
    }
    
    /**
     * Calculates the total yearly cost of all active subscriptions.
     * 
     * @param currency Target currency for calculations (default: USD)
     * @return Flow of total yearly cost
     */
    fun getTotalYearlyCost(currency: String = "USD"): Flow<BigDecimal> {
        return repository.getActiveSubscriptions().map { subscriptions ->
            subscriptions.sumOf { subscription ->
                if (subscription.currency == currency) {
                    subscription.getAnnualCost()
                } else {
                    BigDecimal.ZERO
                }
            }
        }
    }
    
    /**
     * Calculates costs grouped by category.
     * 
     * @param currency Target currency for calculations (default: USD)
     * @return Flow of map where key is category and value is monthly cost
     */
    fun getCostsByCategory(currency: String = "USD"): Flow<Map<String, BigDecimal>> {
        return repository.getActiveSubscriptions().map { subscriptions ->
            subscriptions
                .filter { it.currency == currency }
                .groupBy { it.category.ifBlank { "Other" } }
                .mapValues { (_, subscriptions) ->
                    subscriptions.sumOf { it.getMonthlyEquivalent() }
                }
        }
    }
    
    /**
     * Calculates costs grouped by billing period.
     * 
     * @param currency Target currency for calculations (default: USD)
     * @return Flow of map where key is billing period and value is total cost
     */
    fun getCostsByBillingPeriod(currency: String = "USD"): Flow<Map<BillingPeriod, BigDecimal>> {
        return repository.getActiveSubscriptions().map { subscriptions ->
            subscriptions
                .filter { it.currency == currency }
                .groupBy { it.billingPeriod }
                .mapValues { (_, subscriptions) ->
                    subscriptions.sumOf { it.cost }
                }
        }
    }
    
    /**
     * Calculates upcoming costs for the next specified number of days.
     * 
     * @param days Number of days to look ahead (default: 30)
     * @param currency Target currency for calculations (default: USD)
     * @return Flow of list of upcoming costs with dates
     */
    fun getUpcomingCosts(days: Int = 30, currency: String = "USD"): Flow<List<UpcomingCost>> {
        return repository.getActiveSubscriptions().map { subscriptions ->
            val today = LocalDate.now()
            val endDate = today.plusDays(days.toLong())
            
            subscriptions
                .filter { it.currency == currency }
                .filter { !it.nextBillingDate.isAfter(endDate) }
                .map { subscription ->
                    UpcomingCost(
                        subscriptionId = subscription.id,
                        subscriptionName = subscription.name,
                        amount = subscription.cost,
                        currency = subscription.currency,
                        billingDate = subscription.nextBillingDate,
                        daysUntilBilling = ChronoUnit.DAYS.between(today, subscription.nextBillingDate).toInt()
                    )
                }
                .sortedBy { it.billingDate }
        }
    }
    
    /**
     * Calculates cost trend over the past specified number of months.
     * 
     * @param months Number of months to look back (default: 12)
     * @param currency Target currency for calculations (default: USD)
     * @return Flow of list of monthly cost data
     */
    fun getCostTrend(months: Int = 12, currency: String = "USD"): Flow<List<MonthlyCost>> {
        return repository.getAllSubscriptions().map { subscriptions ->
            val currentMonth = YearMonth.now()
            val startMonth = currentMonth.minusMonths(months.toLong() - 1)
            
            val monthlyData = mutableListOf<MonthlyCost>()
            
            for (i in 0 until months) {
                val month = startMonth.plusMonths(i.toLong())
                val monthStart = month.atDay(1)
                val monthEnd = month.atEndOfMonth()
                
                val activeSubscriptions = subscriptions.filter { subscription ->
                    subscription.currency == currency &&
                    subscription.startDate <= monthEnd &&
                    (subscription.status == SubscriptionStatus.ACTIVE || 
                     subscription.status == SubscriptionStatus.TRIAL) &&
                    // Check if subscription was active during this month
                    !(subscription.status == SubscriptionStatus.CANCELLED && 
                      subscription.updatedAt.toLocalDate().isBefore(monthStart))
                }
                
                val totalCost = activeSubscriptions.sumOf { it.getMonthlyEquivalent() }
                
                monthlyData.add(
                    MonthlyCost(
                        month = month,
                        totalCost = totalCost,
                        subscriptionCount = activeSubscriptions.size
                    )
                )
            }
            
            monthlyData
        }
    }
    
    /**
     * Calculates the average monthly cost over the past specified number of months.
     * 
     * @param months Number of months to calculate average over (default: 12)
     * @param currency Target currency for calculations (default: USD)
     * @return Flow of average monthly cost
     */
    fun getAverageMonthlyCost(months: Int = 12, currency: String = "USD"): Flow<BigDecimal> {
        return getCostTrend(months, currency).map { monthlyCosts ->
            if (monthlyCosts.isEmpty()) {
                BigDecimal.ZERO
            } else {
                val totalCost = monthlyCosts.sumOf { it.totalCost }
                totalCost.divide(BigDecimal(monthlyCosts.size), 2, RoundingMode.HALF_UP)
            }
        }
    }
    
    /**
     * Calculates cost savings from cancelled subscriptions.
     * 
     * @param currency Target currency for calculations (default: USD)
     * @return Flow of monthly savings from cancelled subscriptions
     */
    fun getCostSavings(currency: String = "USD"): Flow<BigDecimal> {
        return repository.getAllSubscriptions().map { subscriptions ->
            subscriptions
                .filter { it.currency == currency }
                .filter { it.status == SubscriptionStatus.CANCELLED }
                .sumOf { it.getMonthlyEquivalent() }
        }
    }
    
    /**
     * Calculates projected annual cost based on current subscriptions.
     * 
     * @param currency Target currency for calculations (default: USD)
     * @return Flow of projected annual cost
     */
    fun getProjectedAnnualCost(currency: String = "USD"): Flow<BigDecimal> {
        return getTotalMonthlyCost(currency).map { monthlyCost ->
            monthlyCost.multiply(BigDecimal(12))
        }
    }
}

/**
 * Data class representing an upcoming cost.
 */
data class UpcomingCost(
    val subscriptionId: String,
    val subscriptionName: String,
    val amount: BigDecimal,
    val currency: String,
    val billingDate: LocalDate,
    val daysUntilBilling: Int
)

/**
 * Data class representing monthly cost data.
 */
data class MonthlyCost(
    val month: YearMonth,
    val totalCost: BigDecimal,
    val subscriptionCount: Int
)