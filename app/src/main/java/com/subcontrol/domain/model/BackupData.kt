package com.subcontrol.domain.model

import com.subcontrol.domain.usecase.BackupSubscription
import kotlinx.serialization.Serializable

/**
 * Data class representing a complete backup of subscription data.
 * 
 * This class contains all the necessary information to restore
 * subscription data, including metadata for version compatibility.
 */
@Serializable
data class BackupData(
    /**
     * Version of the backup format for compatibility checking.
     */
    val version: String,
    
    /**
     * Timestamp when the backup was created (ISO format).
     */
    val createdAt: String,
    
    /**
     * List of all subscriptions in the backup.
     */
    val subscriptions: List<BackupSubscription>
)