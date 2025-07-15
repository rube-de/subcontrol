package com.subcontrol.ui.screens.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.subcontrol.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Backup and restore screen for managing subscription data.
 * 
 * This screen provides functionality to create encrypted backups of subscription data
 * and restore from existing backup files.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Directory picker for backup location
    val backupDirectoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.createBackup(it) }
    }
    
    // File picker for restore
    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { 
            viewModel.validateBackupFile(it)
            viewModel.getBackupFileInfo(it)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_restore_title)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Backup Section
            BackupSection(
                uiState = uiState,
                onCreateBackup = { backupDirectoryLauncher.launch(null) },
                onClearMessage = viewModel::clearBackupMessage
            )
            
            // Restore Section
            RestoreSection(
                uiState = uiState,
                onSelectBackupFile = { restoreFileLauncher.launch(arrayOf("*/*")) },
                onRestoreBackup = { viewModel.restoreBackup(it, uiState.replaceExisting) },
                onReplaceExistingChanged = viewModel::setReplaceExisting,
                onClearMessages = {
                    viewModel.clearRestoreMessage()
                    viewModel.clearValidationResult()
                }
            )
        }
    }
}

/**
 * Backup section of the screen.
 */
@Composable
private fun BackupSection(
    uiState: BackupUiState,
    onCreateBackup: () -> Unit,
    onClearMessage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Backup,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.create_backup),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = stringResource(R.string.create_backup_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(
                onClick = onCreateBackup,
                enabled = !uiState.isBackupInProgress,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isBackupInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.creating_backup))
                } else {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = stringResource(R.string.cd_backup_icon),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.create_backup))
                }
            }
            
            // Show backup message if available
            uiState.backupMessage?.let { message ->
                MessageCard(
                    message = message,
                    isError = message.contains("failed", ignoreCase = true),
                    onDismiss = onClearMessage
                )
            }
        }
    }
}

/**
 * Restore section of the screen.
 */
@Composable
private fun RestoreSection(
    uiState: BackupUiState,
    onSelectBackupFile: () -> Unit,
    onRestoreBackup: (Uri) -> Unit,
    onReplaceExistingChanged: (Boolean) -> Unit,
    onClearMessages: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.restore_backup),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = stringResource(R.string.restore_backup_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Replace existing checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = uiState.replaceExisting,
                    onCheckedChange = onReplaceExistingChanged
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.replace_existing_subscriptions),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            OutlinedButton(
                onClick = onSelectBackupFile,
                enabled = !uiState.isRestoreInProgress && !uiState.isValidating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.select_backup_file))
            }
            
            // Show validation result
            uiState.validationResult?.let { validation ->
                ValidationResultCard(validation = validation)
            }
            
            // Show backup file info
            uiState.selectedBackupFileInfo?.let { fileInfo ->
                BackupFileInfoCard(fileInfo = fileInfo)
            }
            
            // Restore button
            if (uiState.selectedBackupUri != null && uiState.validationResult?.isValid == true) {
                Button(
                    onClick = { onRestoreBackup(uiState.selectedBackupUri) },
                    enabled = !uiState.isRestoreInProgress,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isRestoreInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.restoring))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = stringResource(R.string.cd_restore_icon),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.restore_backup))
                    }
                }
            }
            
            // Show restore message
            uiState.restoreMessage?.let { message ->
                MessageCard(
                    message = message,
                    isError = message.contains("failed", ignoreCase = true),
                    onDismiss = onClearMessages
                )
            }
        }
    }
}

/**
 * Validation result card showing backup file validation status.
 */
@Composable
private fun ValidationResultCard(validation: com.subcontrol.data.manager.ValidationResult) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (validation.isValid) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        else 
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (validation.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (validation.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Text(
                text = validation.message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (validation.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Backup file info card showing file details.
 */
@Composable
private fun BackupFileInfoCard(fileInfo: com.subcontrol.data.manager.BackupFileInfo) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.file_information),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = stringResource(R.string.file_name, fileInfo.name),
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = stringResource(R.string.file_size, formatFileSize(fileInfo.size)),
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = stringResource(R.string.file_modified, formatDate(fileInfo.lastModified)),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Generic message card for showing success/error messages.
 */
@Composable
private fun MessageCard(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    LaunchedEffect(message) {
        // Auto-dismiss after 5 seconds
        kotlinx.coroutines.delay(5000)
        onDismiss()
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isError) 
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        else 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Formats file size in bytes to human-readable format.
 */
private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    
    return when {
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}

/**
 * Formats timestamp to human-readable date format.
 */
private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}