package com.subcontrol.data.repository

import androidx.datastore.core.DataStore
import com.subcontrol.data.model.proto.AppData
import com.subcontrol.data.model.proto.UserPreferences as ProtoUserPreferences
import com.subcontrol.di.IoDispatcher
import com.subcontrol.domain.model.BackupFrequency
import com.subcontrol.domain.model.ThemeMode
import com.subcontrol.domain.model.UserPreferences
import com.subcontrol.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PreferencesRepository using Proto DataStore.
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<AppData>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PreferencesRepository {

    override fun getUserPreferences(): Flow<UserPreferences> {
        return dataStore.data
            .map { appData ->
                if (appData.hasUserPreferences()) {
                    appData.userPreferences.toDomain()
                } else {
                    UserPreferences() // Return default preferences
                }
            }
            .catch { emit(UserPreferences()) }
            .flowOn(ioDispatcher)
    }

    override suspend fun updateUserPreferences(preferences: UserPreferences): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    currentData.toBuilder()
                        .setUserPreferences(preferences.toProto())
                        .build()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateDefaultCurrency(currency: String): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    val currentPreferences = if (currentData.hasUserPreferences()) {
                        currentData.userPreferences.toDomain()
                    } else {
                        UserPreferences()
                    }
                    
                    val updatedPreferences = currentPreferences.copy(
                        defaultCurrency = currency,
                        updatedAt = LocalDateTime.now()
                    )
                    
                    currentData.toBuilder()
                        .setUserPreferences(updatedPreferences.toProto())
                        .build()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    val currentPreferences = if (currentData.hasUserPreferences()) {
                        currentData.userPreferences.toDomain()
                    } else {
                        UserPreferences()
                    }
                    
                    val updatedPreferences = currentPreferences.copy(
                        themeMode = themeMode,
                        updatedAt = LocalDateTime.now()
                    )
                    
                    currentData.toBuilder()
                        .setUserPreferences(updatedPreferences.toProto())
                        .build()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateNotificationSettings(
        enabled: Boolean,
        defaultDays: Int,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean
    ): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    val currentPreferences = if (currentData.hasUserPreferences()) {
                        currentData.userPreferences.toDomain()
                    } else {
                        UserPreferences()
                    }
                    
                    val updatedPreferences = currentPreferences.copy(
                        globalNotificationsEnabled = enabled,
                        defaultNotificationDays = defaultDays,
                        notificationSoundEnabled = soundEnabled,
                        notificationVibrationEnabled = vibrationEnabled,
                        updatedAt = LocalDateTime.now()
                    )
                    
                    currentData.toBuilder()
                        .setUserPreferences(updatedPreferences.toProto())
                        .build()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateSecuritySettings(
        requireAuthentication: Boolean,
        autoLockEnabled: Boolean,
        autoLockTimeoutMinutes: Int
    ): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    val currentPreferences = if (currentData.hasUserPreferences()) {
                        currentData.userPreferences.toDomain()
                    } else {
                        UserPreferences()
                    }
                    
                    val updatedPreferences = currentPreferences.copy(
                        requireAuthentication = requireAuthentication,
                        autoLockEnabled = autoLockEnabled,
                        autoLockTimeoutMinutes = autoLockTimeoutMinutes,
                        updatedAt = LocalDateTime.now()
                    )
                    
                    currentData.toBuilder()
                        .setUserPreferences(updatedPreferences.toProto())
                        .build()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun resetToDefaults(): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    currentData.toBuilder()
                        .setUserPreferences(UserPreferences().toProto())
                        .build()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Converts proto user preferences to domain user preferences.
     */
    private fun ProtoUserPreferences.toDomain(): UserPreferences {
        return UserPreferences(
            defaultCurrency = defaultCurrency,
            showTrialSubscriptions = showTrialSubscriptions,
            showCancelledSubscriptions = showCancelledSubscriptions,
            globalNotificationsEnabled = globalNotificationsEnabled,
            defaultNotificationDays = defaultNotificationDays,
            notificationSoundEnabled = notificationSoundEnabled,
            notificationVibrationEnabled = notificationVibrationEnabled,
            requireAuthentication = requireAuthentication,
            autoLockEnabled = autoLockEnabled,
            autoLockTimeoutMinutes = autoLockTimeoutMinutes,
            backupEnabled = backupEnabled,
            backupFrequency = when (backupFrequency) {
                "daily" -> BackupFrequency.DAILY
                "weekly" -> BackupFrequency.WEEKLY
                "monthly" -> BackupFrequency.MONTHLY
                "never" -> BackupFrequency.NEVER
                else -> BackupFrequency.WEEKLY
            },
            themeMode = when (themeMode) {
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                "system" -> ThemeMode.SYSTEM
                else -> ThemeMode.SYSTEM
            },
            analyticsEnabled = false, // Always false for privacy
            crashReportingEnabled = false, // Always false for privacy
            createdAt = Instant.ofEpochSecond(createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime(),
            updatedAt = Instant.ofEpochSecond(updatedAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
        )
    }

    /**
     * Converts domain user preferences to proto user preferences.
     */
    private fun UserPreferences.toProto(): ProtoUserPreferences {
        return ProtoUserPreferences.newBuilder()
            .setDefaultCurrency(defaultCurrency)
            .setShowTrialSubscriptions(showTrialSubscriptions)
            .setShowCancelledSubscriptions(showCancelledSubscriptions)
            .setGlobalNotificationsEnabled(globalNotificationsEnabled)
            .setDefaultNotificationDays(defaultNotificationDays)
            .setNotificationSoundEnabled(notificationSoundEnabled)
            .setNotificationVibrationEnabled(notificationVibrationEnabled)
            .setRequireAuthentication(requireAuthentication)
            .setAutoLockEnabled(autoLockEnabled)
            .setAutoLockTimeoutMinutes(autoLockTimeoutMinutes)
            .setBackupEnabled(backupEnabled)
            .setBackupFrequency(when (backupFrequency) {
                BackupFrequency.DAILY -> "daily"
                BackupFrequency.WEEKLY -> "weekly"
                BackupFrequency.MONTHLY -> "monthly"
                BackupFrequency.NEVER -> "never"
            })
            .setThemeMode(when (themeMode) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
                ThemeMode.SYSTEM -> "system"
            })
            .setAnalyticsEnabled(false) // Always false for privacy
            .setCrashReportingEnabled(false) // Always false for privacy
            .setCreatedAt(createdAt.atZone(ZoneId.systemDefault()).toEpochSecond())
            .setUpdatedAt(updatedAt.atZone(ZoneId.systemDefault()).toEpochSecond())
            .build()
    }
}