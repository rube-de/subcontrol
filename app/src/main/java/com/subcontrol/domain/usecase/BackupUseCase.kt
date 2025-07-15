package com.subcontrol.domain.usecase

import com.subcontrol.domain.model.BackupData
import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Use case for creating encrypted backups of subscription data.
 * 
 * This use case handles the export of all subscription data into a JSON format
 * that can be encrypted and saved to local storage.
 */
class BackupUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) {
    
    /**
     * Creates a backup of all subscription data.
     * 
     * @return BackupData containing all subscriptions and metadata
     */
    suspend fun execute(): BackupData {
        val subscriptions = subscriptionRepository.getAllSubscriptions().first()
        
        return BackupData(
            version = BACKUP_VERSION,
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            subscriptions = subscriptions.map { it.toBackupSubscription() }
        )
    }
    
    /**
     * Converts a BackupData object to JSON string.
     * 
     * @param backupData The backup data to serialize
     * @return JSON string representation
     */
    fun toJson(backupData: BackupData): String {
        return Json.encodeToString(BackupData.serializer(), backupData)
    }
    
    /**
     * Converts a Subscription domain model to BackupSubscription data class.
     */
    private fun Subscription.toBackupSubscription(): BackupSubscription {
        return BackupSubscription(
            id = id,
            name = name,
            cost = cost.toString(),
            currency = currency,
            billingPeriod = billingPeriod.name,
            nextRenewal = nextBillingDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            category = category,
            tags = tags,
            notes = notes,
            isActive = isActive,
            reminderDaysBefore = notificationDaysBefore,
            trialEndDate = trialEndDate?.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    }
    
    companion object {
        const val BACKUP_VERSION = "1.0"
    }
}

/**
 * Serializable data class for backup representation of a subscription.
 */
@Serializable
data class BackupSubscription(
    val id: String,
    val name: String,
    val cost: String,
    val currency: String,
    val billingPeriod: String,
    val nextRenewal: String,
    val category: String,
    val tags: List<String>,
    val notes: String,
    val isActive: Boolean,
    val reminderDaysBefore: Int,
    val trialEndDate: String?
)