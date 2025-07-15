package com.subcontrol.domain.repository

import com.subcontrol.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for category data operations.
 */
interface CategoryRepository {

    /**
     * Observes all categories.
     * 
     * @return Flow of category list
     */
    fun getAllCategories(): Flow<List<Category>>

    /**
     * Gets a specific category by ID.
     * 
     * @param id Category ID
     * @return Flow of category or null if not found
     */
    fun getCategoryById(id: String): Flow<Category?>

    /**
     * Adds a new category.
     * 
     * @param category Category to add
     * @return Result indicating success or failure
     */
    suspend fun addCategory(category: Category): Result<Unit>

    /**
     * Updates an existing category.
     * 
     * @param category Category to update
     * @return Result indicating success or failure
     */
    suspend fun updateCategory(category: Category): Result<Unit>

    /**
     * Deletes a category.
     * 
     * @param id Category ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteCategory(id: String): Result<Unit>

    /**
     * Reorders categories.
     * 
     * @param categoryIds List of category IDs in new order
     * @return Result indicating success or failure
     */
    suspend fun reorderCategories(categoryIds: List<String>): Result<Unit>

    /**
     * Initializes default categories for new users.
     * 
     * @return Result indicating success or failure
     */
    suspend fun initializeDefaultCategories(): Result<Unit>
}