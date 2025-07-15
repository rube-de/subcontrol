package com.subcontrol.ui.screens.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Screen for adding or editing categories.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditScreen(
    categoryId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load category data if editing
    LaunchedEffect(categoryId) {
        if (categoryId != null) {
            viewModel.loadCategory(categoryId)
        }
    }

    // Handle save success
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    // Handle errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (uiState.isEditMode) "Edit Category" else "Add Category"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.saveCategory() },
                    icon = {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.width(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                        }
                    },
                    text = { Text(if (uiState.isSaving) "Saving..." else "Save") },
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            CategoryEditForm(
                uiState = uiState,
                onNameChange = viewModel::updateName,
                onColorChange = viewModel::updateColor,
                onIconChange = viewModel::updateIcon,
                onSortOrderChange = viewModel::updateSortOrder,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditForm(
    uiState: CategoryEditUiState,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onSortOrderChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Basic Information Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Basic Information",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    label = { Text("Category Name *") },
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.sortOrder,
                    onValueChange = onSortOrderChange,
                    label = { Text("Sort Order") },
                    isError = uiState.sortOrderError != null,
                    supportingText = uiState.sortOrderError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Appearance Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium
                )

                // Color Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preview:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    CategoryPreview(
                        name = uiState.name.ifBlank { "Category" },
                        color = uiState.color,
                        icon = uiState.icon
                    )
                }

                // Color Selection
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelMedium
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryEditViewModel.predefinedColors.forEach { color ->
                        ColorOption(
                            color = color,
                            isSelected = uiState.color == color,
                            onColorSelected = onColorChange
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.color,
                    onValueChange = onColorChange,
                    label = { Text("Custom Color (Hex)") },
                    isError = uiState.colorError != null,
                    supportingText = uiState.colorError?.let { { Text(it) } },
                    placeholder = { Text("#6200EE") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Icon Selection
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelMedium
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryEditViewModel.predefinedIcons.forEach { icon ->
                        IconOption(
                            icon = icon,
                            isSelected = uiState.icon == icon,
                            onIconSelected = onIconChange
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.icon,
                    onValueChange = onIconChange,
                    label = { Text("Custom Icon") },
                    placeholder = { Text("category") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Add bottom padding for FAB
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun CategoryPreview(
    name: String,
    color: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    val colorInt = try {
        Color(android.graphics.Color.parseColor(color))
    } catch (e: IllegalArgumentException) {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = modifier
            .background(
                color = colorInt.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = colorInt,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon.take(1).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (ColorUtils.calculateLuminance(colorInt.toArgb()) > 0.5) {
                    Color.Black
                } else {
                    Color.White
                }
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = colorInt
        )
    }
}

@Composable
private fun ColorOption(
    color: String,
    isSelected: Boolean,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorInt = try {
        Color(android.graphics.Color.parseColor(color))
    } catch (e: IllegalArgumentException) {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(colorInt)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.outline else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onColorSelected(color) }
    )
}

@Composable
private fun IconOption(
    icon: String,
    isSelected: Boolean,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onIconSelected(icon) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon.take(1).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}