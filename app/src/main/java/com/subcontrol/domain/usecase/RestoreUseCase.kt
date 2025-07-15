package com.subcontrol.domain.usecase

import com.subcontrol.domain.model.BackupData
import com.subcontrol.domain.model.BillingPeriod
import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.repository.SubscriptionRepository
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Use case for restoring subscription data from encrypted backups.
 * 
 * This use case handles the import and validation of backup data,
 * converting it back to domain models and saving to the repository.
 */
class RestoreUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) {
    
    /**
     * Restores subscription data from a JSON backup string.
     * 
     * @param jsonData JSON string containing backup data
     * @param replaceExisting Whether to replace existing subscriptions
     * @return RestoreResult indicating success or failure
     */
    suspend fun execute(jsonData: String, replaceExisting: Boolean = false): RestoreResult {
        return try {
            val backupData = fromJson(jsonData)
            
            // Validate backup version compatibility
            if (!isVersionCompatible(backupData.version)) {
                return RestoreResult.Error("Backup version ${backupData.version} is not compatible with current app version")
            }
            
            // Validate backup data integrity
            val validationResult = validateBackupData(backupData)
            if (!validationResult.isValid) {
                return RestoreResult.Error("Backup data validation failed: ${validationResult.error}")
            }
            
            // Convert backup subscriptions to domain models
            val subscriptions = backupData.subscriptions.mapNotNull { backupSub ->
                try {
                    backupSub.toDomainSubscription()
                } catch (e: Exception) {
                    null // Skip invalid subscriptions
                }
            }
            
            if (subscriptions.isEmpty()) {
                return RestoreResult.Error("No valid subscriptions found in backup")
            }
            
            // Clear existing data if requested
            if (replaceExisting) {
                subscriptionRepository.deleteAllSubscriptions()
            }
            
            // Save restored subscriptions
            subscriptions.forEach { subscription ->
                subscriptionRepository.saveSubscription(subscription)
            }
            
            RestoreResult.Success(subscriptions.size)
            
        } catch (e: Exception) {
            RestoreResult.Error("Failed to restore backup: ${e.message}")
        }
    }
    
    /**
     * Converts JSON string to BackupData object.
     * 
     * @param jsonData JSON string to deserialize
     * @return BackupData object
     * @throws Exception if JSON parsing fails
     */
    fun fromJson(jsonData: String): BackupData {
        return Json.decodeFromString(BackupData.serializer(), jsonData)
    }
    
    /**
     * Validates backup data integrity and format.
     * 
     * @param backupData The backup data to validate
     * @return ValidationResult indicating success or failure
     */
    private fun validateBackupData(backupData: BackupData): ValidationResult {
        // Check for required fields
        if (backupData.subscriptions.isEmpty()) {
            return ValidationResult(false, "Backup contains no subscriptions")
        }
        
        // Validate each subscription
        for (subscription in backupData.subscriptions) {
            if (subscription.name.isBlank()) {
                return ValidationResult(false, "Subscription name cannot be blank")
            }
            
            if (subscription.cost.toBigDecimalOrNull() == null) {
                return ValidationResult(false, "Invalid cost format: ${subscription.cost}")
            }
            
            if (subscription.currency.isBlank()) {
                return ValidationResult(false, "Currency cannot be blank")
            }
            
            try {
                BillingPeriod.valueOf(subscription.billingPeriod)
            } catch (e: IllegalArgumentException) {
                return ValidationResult(false, "Invalid billing period: ${subscription.billingPeriod}")
            }
            
            try {
                LocalDate.parse(subscription.nextRenewal, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: Exception) {
                return ValidationResult(false, "Invalid renewal date format: ${subscription.nextRenewal}")
            }
            
            subscription.trialEndDate?.let { trialDate ->
                try {
                    LocalDate.parse(trialDate, DateTimeFormatter.ISO_LOCAL_DATE)
                } catch (e: Exception) {
                    return ValidationResult(false, "Invalid trial end date format: $trialDate")
                }
            }
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Checks if the backup version is compatible with the current app version.
     * 
     * @param version The backup version to check
     * @return true if compatible, false otherwise
     */
    private fun isVersionCompatible(version: String): Boolean {
        // Currently only supporting version 1.0
        return version == "1.0"
    }
    
    /**
     * Converts a BackupSubscription to a Subscription domain model.
     */
    private fun BackupSubscription.toDomainSubscription(): Subscription {
        return Subscription(
            id = id,
            name = name,
            cost = BigDecimal(cost),
            currency = currency,
            billingPeriod = BillingPeriod.valueOf(billingPeriod),
            nextRenewal = LocalDate.parse(nextRenewal, DateTimeFormatter.ISO_LOCAL_DATE),
            category = category,
            tags = tags,
            notes = notes,
            isActive = isActive,
            reminderDaysBefore = reminderDaysBefore,
            trialEndDate = trialEndDate?.let { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }
        )
    }
}

/**
 * Result sealed class for restore operations.
 */
sealed class RestoreResult {
    data class Success(val restoredCount: Int) : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}

/**
 * Internal data class for validation results.
 */
private data class ValidationResult(
    val isValid: Boolean,
    val error: String?
)