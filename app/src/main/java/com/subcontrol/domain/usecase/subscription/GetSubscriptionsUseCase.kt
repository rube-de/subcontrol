package com.subcontrol.domain.usecase.subscription

import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for retrieving subscriptions with various filtering options.
 * 
 * This use case provides business logic for accessing subscription data
 * through the repository layer, including filtering and search capabilities.
 */
class GetSubscriptionsUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    
    /**
     * Gets all subscriptions.
     * 
     * @return Flow of all subscriptions
     */
    fun getAllSubscriptions(): Flow<List<Subscription>> {
        return repository.getAllSubscriptions()
    }
    
    /**
     * Gets only active subscriptions (ACTIVE or TRIAL status).
     * 
     * @return Flow of active subscriptions
     */
    fun getActiveSubscriptions(): Flow<List<Subscription>> {
        return repository.getActiveSubscriptions()
    }
    
    /**
     * Gets a specific subscription by ID.
     * 
     * @param id Subscription ID
     * @return Flow of subscription or null if not found
     */
    fun getSubscriptionById(id: String): Flow<Subscription?> {
        require(id.isNotBlank()) { "Subscription ID cannot be blank" }
        return repository.getSubscriptionById(id)
    }
    
    /**
     * Gets subscriptions filtered by category.
     * 
     * @param category Category name to filter by
     * @return Flow of subscriptions in the specified category
     */
    fun getSubscriptionsByCategory(category: String): Flow<List<Subscription>> {
        require(category.isNotBlank()) { "Category cannot be blank" }
        return repository.getSubscriptionsByCategory(category)
    }
    
    /**
     * Searches subscriptions by name.
     * 
     * @param query Search query (minimum 1 character)
     * @return Flow of subscriptions matching the search query
     */
    fun searchSubscriptions(query: String): Flow<List<Subscription>> {
        require(query.isNotBlank()) { "Search query cannot be blank" }
        return repository.searchSubscriptions(query.trim())
    }
    
    /**
     * Gets subscriptions that have upcoming renewals within the specified number of days.
     * 
     * @param days Number of days to look ahead (default: 7)
     * @return Flow of subscriptions with upcoming renewals
     */
    fun getUpcomingRenewals(days: Int = 7): Flow<List<Subscription>> {
        require(days > 0) { "Days must be positive" }
        
        return repository.getActiveSubscriptions().map { subscriptions ->
            val currentDate = java.time.LocalDate.now()
            val cutoffDate = currentDate.plusDays(days.toLong())
            
            subscriptions.filter { subscription ->
                !subscription.nextBillingDate.isAfter(cutoffDate)
            }.sortedBy { it.nextBillingDate }
        }
    }
    
    /**
     * Gets subscriptions that are currently in trial period.
     * 
     * @return Flow of trial subscriptions
     */
    fun getTrialSubscriptions(): Flow<List<Subscription>> {
        return repository.getActiveSubscriptions().map { subscriptions ->
            subscriptions.filter { it.isInTrial }
        }
    }
    
    /**
     * Gets subscriptions grouped by category.
     * 
     * @return Flow of map where key is category and value is list of subscriptions
     */
    fun getSubscriptionsGroupedByCategory(): Flow<Map<String, List<Subscription>>> {
        return repository.getAllSubscriptions().map { subscriptions ->
            subscriptions.groupBy { subscription ->
                subscription.category.ifBlank { "Other" }
            }
        }
    }
}