package com.subcontrol.domain.repository

import com.subcontrol.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user preferences data operations.
 */
interface PreferencesRepository {

    /**
     * Observes user preferences.
     * 
     * @return Flow of user preferences
     */
    fun getUserPreferences(): Flow<UserPreferences>

    /**
     * Updates user preferences.
     * 
     * @param preferences Updated preferences
     * @return Result indicating success or failure
     */
    suspend fun updateUserPreferences(preferences: UserPreferences): Result<Unit>

    /**
     * Updates the default currency.
     * 
     * @param currency Currency code (e.g., "USD", "EUR")
     * @return Result indicating success or failure
     */
    suspend fun updateDefaultCurrency(currency: String): Result<Unit>

    /**
     * Updates the theme mode.
     * 
     * @param themeMode New theme mode
     * @return Result indicating success or failure
     */
    suspend fun updateThemeMode(themeMode: com.subcontrol.domain.model.ThemeMode): Result<Unit>

    /**
     * Updates notification settings.
     * 
     * @param enabled Whether notifications are enabled
     * @param defaultDays Default notification days before billing
     * @param soundEnabled Whether notification sound is enabled
     * @param vibrationEnabled Whether notification vibration is enabled
     * @return Result indicating success or failure
     */
    suspend fun updateNotificationSettings(
        enabled: Boolean,
        defaultDays: Int,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean
    ): Result<Unit>

    /**
     * Updates security settings.
     * 
     * @param requireAuthentication Whether authentication is required
     * @param autoLockEnabled Whether auto-lock is enabled
     * @param autoLockTimeoutMinutes Auto-lock timeout in minutes
     * @return Result indicating success or failure
     */
    suspend fun updateSecuritySettings(
        requireAuthentication: Boolean,
        autoLockEnabled: Boolean,
        autoLockTimeoutMinutes: Int
    ): Result<Unit>

    /**
     * Resets all preferences to defaults.
     * 
     * @return Result indicating success or failure
     */
    suspend fun resetToDefaults(): Result<Unit>
}