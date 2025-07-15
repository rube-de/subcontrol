package com.subcontrol.ui.screens.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subcontrol.data.manager.BackupManager
import com.subcontrol.data.manager.BackupResult
import com.subcontrol.data.manager.ValidationResult
import com.subcontrol.domain.usecase.RestoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing backup and restore operations.
 * 
 * This ViewModel handles the UI state for backup and restore screens,
 * orchestrating calls to the BackupManager and handling user interactions.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()
    
    /**
     * Creates a backup at the specified directory.
     * 
     * @param directoryUri URI of the directory where backup should be saved
     */
    fun createBackup(directoryUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isBackupInProgress = true,
                backupMessage = null
            )
            
            when (val result = backupManager.createBackup(directoryUri)) {
                is BackupResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isBackupInProgress = false,
                        backupMessage = "Backup created successfully: ${result.filename}\n${result.subscriptionCount} subscriptions backed up",
                        lastBackupUri = result.uri
                    )
                }
                is BackupResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isBackupInProgress = false,
                        backupMessage = "Backup failed: ${result.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Restores subscription data from a backup file.
     * 
     * @param backupUri URI of the backup file to restore
     * @param replaceExisting Whether to replace existing subscriptions
     */
    fun restoreBackup(backupUri: Uri, replaceExisting: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRestoreInProgress = true,
                restoreMessage = null
            )
            
            when (val result = backupManager.restoreBackup(backupUri, replaceExisting)) {
                is RestoreResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isRestoreInProgress = false,
                        restoreMessage = "Restore completed successfully!\n${result.restoredCount} subscriptions restored"
                    )
                }
                is RestoreResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isRestoreInProgress = false,
                        restoreMessage = "Restore failed: ${result.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Validates a backup file before restoration.
     * 
     * @param backupUri URI of the backup file to validate
     */
    fun validateBackupFile(backupUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isValidating = true,
                validationResult = null
            )
            
            val result = backupManager.validateBackupFile(backupUri)
            
            _uiState.value = _uiState.value.copy(
                isValidating = false,
                validationResult = result,
                selectedBackupUri = if (result.isValid) backupUri else null
            )
        }
    }
    
    /**
     * Gets information about a backup file.
     * 
     * @param backupUri URI of the backup file
     */
    fun getBackupFileInfo(backupUri: Uri) {
        viewModelScope.launch {
            val fileInfo = backupManager.getBackupFileInfo(backupUri)
            _uiState.value = _uiState.value.copy(selectedBackupFileInfo = fileInfo)
        }
    }
    
    /**
     * Clears the backup message from the UI state.
     */
    fun clearBackupMessage() {
        _uiState.value = _uiState.value.copy(backupMessage = null)
    }
    
    /**
     * Clears the restore message from the UI state.
     */
    fun clearRestoreMessage() {
        _uiState.value = _uiState.value.copy(restoreMessage = null)
    }
    
    /**
     * Clears the validation result from the UI state.
     */
    fun clearValidationResult() {
        _uiState.value = _uiState.value.copy(
            validationResult = null,
            selectedBackupUri = null,
            selectedBackupFileInfo = null
        )
    }
    
    /**
     * Updates the replace existing setting.
     * 
     * @param replaceExisting Whether to replace existing subscriptions on restore
     */
    fun setReplaceExisting(replaceExisting: Boolean) {
        _uiState.value = _uiState.value.copy(replaceExisting = replaceExisting)
    }
}

/**
 * UI state for backup and restore operations.
 */
data class BackupUiState(
    val isBackupInProgress: Boolean = false,
    val isRestoreInProgress: Boolean = false,
    val isValidating: Boolean = false,
    val backupMessage: String? = null,
    val restoreMessage: String? = null,
    val validationResult: ValidationResult? = null,
    val selectedBackupUri: Uri? = null,
    val selectedBackupFileInfo: com.subcontrol.data.manager.BackupFileInfo? = null,
    val lastBackupUri: Uri? = null,
    val replaceExisting: Boolean = false
)