package com.subcontrol.ui.screens.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subcontrol.domain.model.Category
import com.subcontrol.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for category editing (add/edit functionality).
 */
@HiltViewModel
class CategoryEditViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryEditUiState())
    val uiState: StateFlow<CategoryEditUiState> = _uiState.asStateFlow()

    /**
     * Loads the category data for editing.
     */
    fun loadCategory(categoryId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            categoryRepository.getCategoryById(categoryId)
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load category: ${error.message}"
                    )
                }
                .collect { category ->
                    if (category != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            name = category.name,
                            color = category.color,
                            icon = category.icon,
                            sortOrder = category.sortOrder.toString(),
                            isEditMode = true,
                            originalCategory = category
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Category not found"
                        )
                    }
                }
        }
    }

    /**
     * Updates the category name.
     */
    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(
            name = name,
            nameError = if (name.isBlank()) "Name is required" else null
        )
    }

    /**
     * Updates the category color.
     */
    fun updateColor(color: String) {
        _uiState.value = _uiState.value.copy(
            color = color,
            colorError = if (!isValidHexColor(color)) "Invalid color format" else null
        )
    }

    /**
     * Updates the category icon.
     */
    fun updateIcon(icon: String) {
        _uiState.value = _uiState.value.copy(icon = icon)
    }

    /**
     * Updates the sort order.
     */
    fun updateSortOrder(sortOrder: String) {
        val orderError = try {
            if (sortOrder.isNotBlank()) {
                val value = sortOrder.toInt()
                if (value < 0) "Sort order cannot be negative" else null
            } else {
                null // Optional field
            }
        } catch (e: NumberFormatException) {
            "Invalid sort order format"
        }

        _uiState.value = _uiState.value.copy(
            sortOrder = sortOrder,
            sortOrderError = orderError
        )
    }

    /**
     * Clears any existing error messages.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Saves the category (add or update).
     */
    fun saveCategory() {
        val currentState = _uiState.value
        
        // Validate all required fields
        val hasErrors = validateForm()
        if (hasErrors) return

        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            try {
                val category = createCategoryFromState(currentState)
                
                val result = if (currentState.isEditMode && currentState.originalCategory != null) {
                    categoryRepository.updateCategory(category)
                } else {
                    categoryRepository.addCategory(category)
                }

                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            isSaved = true
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            error = "Failed to save category: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save category: ${e.message}"
                )
            }
        }
    }

    private fun validateForm(): Boolean {
        val currentState = _uiState.value
        var hasErrors = false

        // Validate name
        if (currentState.name.isBlank()) {
            _uiState.value = _uiState.value.copy(nameError = "Name is required")
            hasErrors = true
        }

        // Validate color
        if (currentState.color.isNotBlank() && !isValidHexColor(currentState.color)) {
            _uiState.value = _uiState.value.copy(colorError = "Invalid color format")
            hasErrors = true
        }

        // Validate sort order
        if (currentState.sortOrder.isNotBlank()) {
            try {
                val orderValue = currentState.sortOrder.toInt()
                if (orderValue < 0) {
                    _uiState.value = _uiState.value.copy(sortOrderError = "Sort order cannot be negative")
                    hasErrors = true
                }
            } catch (e: NumberFormatException) {
                _uiState.value = _uiState.value.copy(sortOrderError = "Invalid sort order format")
                hasErrors = true
            }
        }

        return hasErrors
    }

    private fun createCategoryFromState(state: CategoryEditUiState): Category {
        val now = LocalDateTime.now()
        val sortOrder = state.sortOrder.toIntOrNull() ?: 0

        return if (state.isEditMode && state.originalCategory != null) {
            state.originalCategory.copy(
                name = state.name,
                color = state.color.ifBlank { "#6200EE" }, // Default material color
                icon = state.icon.ifBlank { "category" }, // Default icon
                sortOrder = sortOrder,
                updatedAt = now
            )
        } else {
            Category(
                id = UUID.randomUUID().toString(),
                name = state.name,
                color = state.color.ifBlank { "#6200EE" }, // Default material color
                icon = state.icon.ifBlank { "category" }, // Default icon
                sortOrder = sortOrder,
                createdAt = now,
                updatedAt = now
            )
        }
    }

    private fun isValidHexColor(color: String): Boolean {
        return color.matches(Regex("^#[0-9A-Fa-f]{6}$"))
    }

    companion object {
        /**
         * Predefined colors for categories.
         */
        val predefinedColors = listOf(
            "#6200EE", // Purple
            "#3700B3", // Dark Purple
            "#03DAC6", // Teal
            "#018786", // Dark Teal
            "#B00020", // Error Red
            "#FF6D00", // Orange
            "#2196F3", // Blue
            "#4CAF50", // Green
            "#FF9800", // Amber
            "#9C27B0", // Purple
            "#E91E63", // Pink
            "#795548"  // Brown
        )

        /**
         * Predefined Material Design icons for categories.
         */
        val predefinedIcons = listOf(
            "category",
            "label",
            "folder",
            "star",
            "bookmark",
            "tag",
            "group",
            "collections",
            "dashboard",
            "grid_view"
        )
    }
}

/**
 * UI state for category editing.
 */
data class CategoryEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val originalCategory: Category? = null,
    
    // Form fields
    val name: String = "",
    val nameError: String? = null,
    val color: String = "#6200EE",
    val colorError: String? = null,
    val icon: String = "category",
    val sortOrder: String = "0",
    val sortOrderError: String? = null
) {
    val isFormValid: Boolean
        get() = name.isNotBlank() && 
                nameError == null && 
                colorError == null && 
                sortOrderError == null
}