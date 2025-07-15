package com.subcontrol.data.manager

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.subcontrol.data.datastore.EncryptionManager
import com.subcontrol.domain.model.BackupData
import com.subcontrol.domain.usecase.BackupUseCase
import com.subcontrol.domain.usecase.RestoreUseCase
import com.subcontrol.domain.usecase.RestoreResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages backup and restore operations for subscription data.
 * 
 * This class handles the creation of encrypted backup files and
 * restoration from those files using the Android Storage Access Framework.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupUseCase: BackupUseCase,
    private val restoreUseCase: RestoreUseCase
) {
    
    /**
     * Creates an encrypted backup and saves it to the specified directory.
     * 
     * @param directoryUri URI of the directory where backup should be saved
     * @return BackupResult indicating success or failure
     */
    suspend fun createBackup(directoryUri: Uri): BackupResult {
        return withContext(Dispatchers.IO) {
            try {
                // Generate backup data
                val backupData = backupUseCase.execute()
                val jsonData = backupUseCase.toJson(backupData)
                
                // Encrypt the JSON data
                val encryptedData = EncryptionManager.encrypt(jsonData.toByteArray())
                
                // Generate filename with timestamp
                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                val filename = "subcontrol_backup_$timestamp.scb"
                
                // Write encrypted data to file
                val documentFile = DocumentFile.fromTreeUri(context, directoryUri)
                val backupFile = documentFile?.createFile("application/octet-stream", filename)
                
                if (backupFile == null) {
                    return@withContext BackupResult.Error("Failed to create backup file")
                }
                
                context.contentResolver.openOutputStream(backupFile.uri)?.use { outputStream ->
                    outputStream.write(encryptedData)
                    outputStream.flush()
                }
                
                BackupResult.Success(filename, backupFile.uri, backupData.subscriptions.size)
                
            } catch (e: Exception) {
                BackupResult.Error("Failed to create backup: ${e.message}")
            }
        }
    }
    
    /**
     * Restores subscription data from an encrypted backup file.
     * 
     * @param backupUri URI of the backup file to restore
     * @param replaceExisting Whether to replace existing subscriptions
     * @return RestoreResult indicating success or failure
     */
    suspend fun restoreBackup(backupUri: Uri, replaceExisting: Boolean = false): RestoreResult {
        return withContext(Dispatchers.IO) {
            try {
                // Read encrypted data from file
                val encryptedData = context.contentResolver.openInputStream(backupUri)?.use { inputStream ->
                    inputStream.readBytes()
                } ?: return@withContext RestoreResult.Error("Failed to read backup file")
                
                // Decrypt the data
                val decryptedData = EncryptionManager.decrypt(encryptedData)
                val jsonData = String(decryptedData)
                
                // Restore subscription data
                restoreUseCase.execute(jsonData, replaceExisting)
                
            } catch (e: SecurityException) {
                RestoreResult.Error("Security error: Cannot access backup file")
            } catch (e: IOException) {
                RestoreResult.Error("IO error: Failed to read backup file")
            } catch (e: Exception) {
                RestoreResult.Error("Decryption failed: Invalid backup file or corrupted data")
            }
        }
    }
    
    /**
     * Validates if a file is a valid SubControl backup file.
     * 
     * @param backupUri URI of the file to validate
     * @return ValidationResult indicating if file is valid
     */
    suspend fun validateBackupFile(backupUri: Uri): ValidationResult {
        return withContext(Dispatchers.IO) {
            try {
                // Check file extension
                val fileName = DocumentFile.fromSingleUri(context, backupUri)?.name
                if (fileName?.endsWith(".scb") != true) {
                    return@withContext ValidationResult(false, "Invalid file format. Expected .scb file")
                }
                
                // Try to read and decrypt a small portion to verify format
                val encryptedData = context.contentResolver.openInputStream(backupUri)?.use { inputStream ->
                    inputStream.readBytes()
                } ?: return@withContext ValidationResult(false, "Cannot read backup file")
                
                // Attempt decryption
                val decryptedData = EncryptionManager.decrypt(encryptedData)
                val jsonData = String(decryptedData)
                
                // Validate JSON structure
                val backupData = restoreUseCase.fromJson(jsonData)
                
                ValidationResult(true, "Valid backup file with ${backupData.subscriptions.size} subscriptions")
                
            } catch (e: Exception) {
                ValidationResult(false, "Invalid backup file: ${e.message}")
            }
        }
    }
    
    /**
     * Gets backup file information without full validation.
     * 
     * @param backupUri URI of the backup file
     * @return BackupFileInfo containing metadata
     */
    suspend fun getBackupFileInfo(backupUri: Uri): BackupFileInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val documentFile = DocumentFile.fromSingleUri(context, backupUri)
                val fileName = documentFile?.name ?: return@withContext null
                val fileSize = documentFile.length()
                val lastModified = documentFile.lastModified()
                
                BackupFileInfo(
                    name = fileName,
                    size = fileSize,
                    lastModified = lastModified,
                    uri = backupUri
                )
                
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Result sealed class for backup operations.
 */
sealed class BackupResult {
    data class Success(
        val filename: String,
        val uri: Uri,
        val subscriptionCount: Int
    ) : BackupResult()
    
    data class Error(val message: String) : BackupResult()
}

/**
 * Result class for backup file validation.
 */
data class ValidationResult(
    val isValid: Boolean,
    val message: String
)

/**
 * Data class containing backup file information.
 */
data class BackupFileInfo(
    val name: String,
    val size: Long,
    val lastModified: Long,
    val uri: Uri
)