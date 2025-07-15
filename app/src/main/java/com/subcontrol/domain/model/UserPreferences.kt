package com.subcontrol.domain.model

import java.time.LocalDateTime

/**
 * Domain model for user preferences.
 */
data class UserPreferences(
    val defaultCurrency: String = "USD",
    val showTrialSubscriptions: Boolean = true,
    val showCancelledSubscriptions: Boolean = false,
    val globalNotificationsEnabled: Boolean = true,
    val defaultNotificationDays: Int = 3,
    val notificationSoundEnabled: Boolean = true,
    val notificationVibrationEnabled: Boolean = true,
    val requireAuthentication: Boolean = false,
    val autoLockEnabled: Boolean = false,
    val autoLockTimeoutMinutes: Int = 5,
    val backupEnabled: Boolean = true,
    val backupFrequency: BackupFrequency = BackupFrequency.WEEKLY,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val analyticsEnabled: Boolean = false, // Always false for privacy
    val crashReportingEnabled: Boolean = false, // Always false for privacy
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Enumeration of backup frequencies.
 */
enum class BackupFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    NEVER
}

/**
 * Enumeration of theme modes.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}