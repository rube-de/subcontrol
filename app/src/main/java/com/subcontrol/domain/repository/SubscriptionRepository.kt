package com.subcontrol.domain.repository

import com.subcontrol.domain.model.Subscription
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for subscription data operations.
 * 
 * This interface defines the contract for subscription data access,
 * following the repository pattern to abstract data sources.
 */
interface SubscriptionRepository {

    /**
     * Observes all subscriptions.
     * 
     * @return Flow of subscription list
     */
    fun getAllSubscriptions(): Flow<List<Subscription>>

    /**
     * Observes active subscriptions only.
     * 
     * @return Flow of active subscription list
     */
    fun getActiveSubscriptions(): Flow<List<Subscription>>

    /**
     * Gets a specific subscription by ID.
     * 
     * @param id Subscription ID
     * @return Flow of subscription or null if not found
     */
    fun getSubscriptionById(id: String): Flow<Subscription?>

    /**
     * Gets subscriptions by category.
     * 
     * @param category Category name
     * @return Flow of subscription list
     */
    fun getSubscriptionsByCategory(category: String): Flow<List<Subscription>>

    /**
     * Searches subscriptions by name.
     * 
     * @param query Search query
     * @return Flow of subscription list
     */
    fun searchSubscriptions(query: String): Flow<List<Subscription>>

    /**
     * Adds a new subscription.
     * 
     * @param subscription Subscription to add
     * @return Result indicating success or failure
     */
    suspend fun addSubscription(subscription: Subscription): Result<Unit>

    /**
     * Updates an existing subscription.
     * 
     * @param subscription Subscription to update
     * @return Result indicating success or failure
     */
    suspend fun updateSubscription(subscription: Subscription): Result<Unit>

    /**
     * Deletes a subscription.
     * 
     * @param id Subscription ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteSubscription(id: String): Result<Unit>

    /**
     * Deletes all subscriptions.
     * 
     * @return Result indicating success or failure
     */
    suspend fun deleteAllSubscriptions(): Result<Unit>

    /**
     * Updates the billing date for a subscription.
     * 
     * @param id Subscription ID
     * @param newBillingDate New billing date
     * @return Result indicating success or failure
     */
    suspend fun updateBillingDate(id: String, newBillingDate: java.time.LocalDate): Result<Unit>

    /**
     * Updates the subscription status.
     * 
     * @param id Subscription ID
     * @param status New status
     * @return Result indicating success or failure
     */
    suspend fun updateStatus(id: String, status: com.subcontrol.domain.model.SubscriptionStatus): Result<Unit>
}