package com.subcontrol.domain.usecase.subscription

import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.model.SubscriptionStatus
import com.subcontrol.domain.repository.SubscriptionRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

/**
 * Use case for saving (creating or updating) subscriptions.
 * 
 * This use case provides business logic for subscription persistence,
 * including validation and data transformation.
 */
class SaveSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    
    /**
     * Creates a new subscription.
     * 
     * @param subscription Subscription data to create
     * @return Result indicating success or failure
     */
    suspend fun createSubscription(subscription: Subscription): Result<Subscription> {
        return try {
            // Validate subscription data
            validateSubscription(subscription)
            
            // Create new subscription with generated ID and timestamps
            val newSubscription = subscription.copy(
                id = if (subscription.id.isBlank()) UUID.randomUUID().toString() else subscription.id,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            
            // Save to repository
            repository.addSubscription(newSubscription).fold(
                onSuccess = { Result.success(newSubscription) },
                onFailure = { error -> Result.failure(error) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Updates an existing subscription.
     * 
     * @param subscription Subscription data to update
     * @return Result indicating success or failure
     */
    suspend fun updateSubscription(subscription: Subscription): Result<Subscription> {
        return try {
            // Validate subscription data
            validateSubscription(subscription)
            
            require(subscription.id.isNotBlank()) { "Subscription ID is required for updates" }
            
            // Update subscription with new timestamp
            val updatedSubscription = subscription.copy(
                updatedAt = LocalDateTime.now()
            )
            
            // Save to repository
            repository.updateSubscription(updatedSubscription).fold(
                onSuccess = { Result.success(updatedSubscription) },
                onFailure = { error -> Result.failure(error) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Updates the billing date for a subscription.
     * 
     * @param subscriptionId Subscription ID
     * @param newBillingDate New billing date
     * @return Result indicating success or failure
     */
    suspend fun updateBillingDate(subscriptionId: String, newBillingDate: LocalDate): Result<Unit> {
        return try {
            require(subscriptionId.isNotBlank()) { "Subscription ID cannot be blank" }
            require(!newBillingDate.isBefore(LocalDate.now())) { "Billing date cannot be in the past" }
            
            repository.updateBillingDate(subscriptionId, newBillingDate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Updates the status of a subscription.
     * 
     * @param subscriptionId Subscription ID
     * @param status New status
     * @return Result indicating success or failure
     */
    suspend fun updateStatus(subscriptionId: String, status: SubscriptionStatus): Result<Unit> {
        return try {
            require(subscriptionId.isNotBlank()) { "Subscription ID cannot be blank" }
            
            repository.updateStatus(subscriptionId, status)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Marks a subscription as cancelled.
     * 
     * @param subscriptionId Subscription ID
     * @return Result indicating success or failure
     */
    suspend fun cancelSubscription(subscriptionId: String): Result<Unit> {
        return updateStatus(subscriptionId, SubscriptionStatus.CANCELLED)
    }
    
    /**
     * Marks a subscription as paused.
     * 
     * @param subscriptionId Subscription ID
     * @return Result indicating success or failure
     */
    suspend fun pauseSubscription(subscriptionId: String): Result<Unit> {
        return updateStatus(subscriptionId, SubscriptionStatus.PAUSED)
    }
    
    /**
     * Reactivates a paused subscription.
     * 
     * @param subscriptionId Subscription ID
     * @return Result indicating success or failure
     */
    suspend fun reactivateSubscription(subscriptionId: String): Result<Unit> {
        return updateStatus(subscriptionId, SubscriptionStatus.ACTIVE)
    }
    
    /**
     * Validates subscription data before saving.
     * 
     * @param subscription Subscription to validate
     * @throws IllegalArgumentException if validation fails
     */
    private fun validateSubscription(subscription: Subscription) {
        require(subscription.name.isNotBlank()) { "Subscription name cannot be blank" }
        require(subscription.cost >= BigDecimal.ZERO) { "Subscription cost cannot be negative" }
        require(subscription.currency.isNotBlank()) { "Currency cannot be blank" }
        require(subscription.currency.length == 3) { "Currency must be a valid 3-letter code" }
        require(subscription.billingCycle > 0) { "Billing cycle must be positive" }
        require(!subscription.nextBillingDate.isBefore(LocalDate.now().minusDays(1))) { 
            "Next billing date cannot be more than 1 day in the past" 
        }
        require(subscription.notificationDaysBefore >= 0) { "Notification days before cannot be negative" }
        require(subscription.notificationDaysBefore <= 365) { "Notification days before cannot exceed 365" }
        
        // Validate trial end date if present
        subscription.trialEndDate?.let { trialEnd ->
            require(!trialEnd.isBefore(subscription.startDate)) { 
                "Trial end date cannot be before start date" 
            }
        }
        
        // Validate email format if provided
        if (subscription.supportEmail.isNotBlank()) {
            require(isValidEmail(subscription.supportEmail)) { 
                "Support email format is invalid" 
            }
        }
        
        // Validate URL format if provided
        if (subscription.websiteUrl.isNotBlank()) {
            require(isValidUrl(subscription.websiteUrl)) { 
                "Website URL format is invalid" 
            }
        }
    }
    
    /**
     * Validates email format.
     * 
     * @param email Email to validate
     * @return true if valid, false otherwise
     */
    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }
    
    /**
     * Validates URL format.
     * 
     * @param url URL to validate
     * @return true if valid, false otherwise
     */
    private fun isValidUrl(url: String): Boolean {
        return try {
            java.net.URL(url)
            true
        } catch (e: Exception) {
            false
        }
    }
}