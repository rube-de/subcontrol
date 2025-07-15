package com.subcontrol.domain.usecase.subscription

import com.subcontrol.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

/**
 * Use case for deleting subscriptions.
 * 
 * This use case provides business logic for subscription deletion,
 * including validation and batch operations.
 */
class DeleteSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    
    /**
     * Deletes a single subscription by ID.
     * 
     * @param subscriptionId ID of the subscription to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteSubscription(subscriptionId: String): Result<Unit> {
        return try {
            require(subscriptionId.isNotBlank()) { "Subscription ID cannot be blank" }
            
            repository.deleteSubscription(subscriptionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Deletes multiple subscriptions by their IDs.
     * 
     * @param subscriptionIds List of subscription IDs to delete
     * @return Result indicating success or failure with count of deleted items
     */
    suspend fun deleteSubscriptions(subscriptionIds: List<String>): Result<Int> {
        return try {
            require(subscriptionIds.isNotEmpty()) { "Subscription IDs list cannot be empty" }
            require(subscriptionIds.all { it.isNotBlank() }) { "All subscription IDs must be non-blank" }
            
            var deletedCount = 0
            var lastError: Exception? = null
            
            for (id in subscriptionIds) {
                repository.deleteSubscription(id).fold(
                    onSuccess = { deletedCount++ },
                    onFailure = { error -> 
                        lastError = error as? Exception ?: Exception(error.message)
                    }
                )
            }
            
            if (deletedCount == 0 && lastError != null) {
                Result.failure(lastError)
            } else {
                Result.success(deletedCount)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Deletes all subscriptions.
     * 
     * This operation is irreversible and should be used with caution.
     * 
     * @return Result indicating success or failure
     */
    suspend fun deleteAllSubscriptions(): Result<Unit> {
        return try {
            repository.deleteAllSubscriptions()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Deletes all subscriptions in a specific category.
     * 
     * @param category Category name
     * @return Result indicating success or failure with count of deleted items
     */
    suspend fun deleteSubscriptionsByCategory(category: String): Result<Int> {
        return try {
            require(category.isNotBlank()) { "Category cannot be blank" }
            
            // Get all subscriptions in the category
            val subscriptionsToDelete = mutableListOf<String>()
            repository.getSubscriptionsByCategory(category).collect { subscriptions ->
                subscriptionsToDelete.addAll(subscriptions.map { it.id })
            }
            
            if (subscriptionsToDelete.isEmpty()) {
                Result.success(0)
            } else {
                deleteSubscriptions(subscriptionsToDelete)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Deletes all cancelled subscriptions.
     * 
     * This is useful for cleanup operations.
     * 
     * @return Result indicating success or failure with count of deleted items
     */
    suspend fun deleteCancelledSubscriptions(): Result<Int> {
        return try {
            val subscriptionsToDelete = mutableListOf<String>()
            
            repository.getAllSubscriptions().collect { subscriptions ->
                subscriptionsToDelete.addAll(
                    subscriptions.filter { 
                        it.status == com.subcontrol.domain.model.SubscriptionStatus.CANCELLED 
                    }.map { it.id }
                )
            }
            
            if (subscriptionsToDelete.isEmpty()) {
                Result.success(0)
            } else {
                deleteSubscriptions(subscriptionsToDelete)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Deletes all expired subscriptions.
     * 
     * This is useful for cleanup operations.
     * 
     * @return Result indicating success or failure with count of deleted items
     */
    suspend fun deleteExpiredSubscriptions(): Result<Int> {
        return try {
            val subscriptionsToDelete = mutableListOf<String>()
            
            repository.getAllSubscriptions().collect { subscriptions ->
                subscriptionsToDelete.addAll(
                    subscriptions.filter { 
                        it.status == com.subcontrol.domain.model.SubscriptionStatus.EXPIRED 
                    }.map { it.id }
                )
            }
            
            if (subscriptionsToDelete.isEmpty()) {
                Result.success(0)
            } else {
                deleteSubscriptions(subscriptionsToDelete)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}