package com.subcontrol.data.repository

import androidx.datastore.core.DataStore
import com.subcontrol.data.model.proto.AppData
import com.subcontrol.data.model.proto.Category as ProtoCategory
import com.subcontrol.data.model.proto.CategoryList
import com.subcontrol.di.IoDispatcher
import com.subcontrol.domain.model.Category
import com.subcontrol.domain.repository.CategoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CategoryRepository using Proto DataStore.
 */
@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<AppData>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return dataStore.data
            .map { appData ->
                appData.categoryList.categoriesList.map { it.toDomain() }
                    .sortedBy { it.sortOrder }
            }
            .catch { emit(emptyList()) }
            .flowOn(ioDispatcher)
    }

    override fun getCategoryById(id: String): Flow<Category?> {
        return getAllCategories()
            .map { categories ->
                categories.find { it.id == id }
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun addCategory(category: Category): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    val currentCategories = currentData.categoryList.categoriesList.toMutableList()
                    currentCategories.add(category.toProto())
                    
                    val updatedCategoryList = CategoryList.newBuilder()
                        .addAllCategories(currentCategories)
                        .setLastUpdated(System.currentTimeMillis() / 1000)
                        .build()
                    
                    currentData.toBuilder()
                        .setCategoryList(updatedCategoryList)
                        .build()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateCategory(category: Category): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    val currentCategories = currentData.categoryList.categoriesList.toMutableList()
                    val index = currentCategories.indexOfFirst { it.id == category.id }
                    
                    if (index != -1) {
                        currentCategories[index] = category.toProto()
                        
                        val updatedCategoryList = CategoryList.newBuilder()
                            .addAllCategories(currentCategories)
                            .setLastUpdated(System.currentTimeMillis() / 1000)
                            .build()
                        
                        currentData.toBuilder()
                            .setCategoryList(updatedCategoryList)
                            .build()
                    } else {
                        currentData // No changes if category not found
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteCategory(id: String): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    val currentCategories = currentData.categoryList.categoriesList.toMutableList()
                    currentCategories.removeAll { it.id == id }
                    
                    val updatedCategoryList = CategoryList.newBuilder()
                        .addAllCategories(currentCategories)
                        .setLastUpdated(System.currentTimeMillis() / 1000)
                        .build()
                    
                    currentData.toBuilder()
                        .setCategoryList(updatedCategoryList)
                        .build()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun reorderCategories(categoryIds: List<String>): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    val currentCategories = currentData.categoryList.categoriesList.associateBy { it.id }
                    val reorderedCategories = categoryIds.mapIndexedNotNull { index, id ->
                        currentCategories[id]?.toBuilder()
                            ?.setSortOrder(index)
                            ?.setUpdatedAt(System.currentTimeMillis() / 1000)
                            ?.build()
                    }
                    
                    val updatedCategoryList = CategoryList.newBuilder()
                        .addAllCategories(reorderedCategories)
                        .setLastUpdated(System.currentTimeMillis() / 1000)
                        .build()
                    
                    currentData.toBuilder()
                        .setCategoryList(updatedCategoryList)
                        .build()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun initializeDefaultCategories(): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    // Only initialize if no categories exist
                    if (currentData.categoryList.categoriesCount == 0) {
                        val defaultCategories = Category.defaultCategories.mapIndexed { index, name ->
                            val now = LocalDateTime.now()
                            Category(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                color = getDefaultCategoryColor(index),
                                icon = getDefaultCategoryIcon(name),
                                sortOrder = index,
                                createdAt = now,
                                updatedAt = now
                            ).toProto()
                        }
                        
                        val updatedCategoryList = CategoryList.newBuilder()
                            .addAllCategories(defaultCategories)
                            .setLastUpdated(System.currentTimeMillis() / 1000)
                            .build()
                        
                        currentData.toBuilder()
                            .setCategoryList(updatedCategoryList)
                            .build()
                    } else {
                        currentData // No changes if categories already exist
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Converts proto category to domain category.
     */
    private fun ProtoCategory.toDomain(): Category {
        return Category(
            id = id,
            name = name,
            color = color,
            icon = icon,
            sortOrder = sortOrder,
            createdAt = Instant.ofEpochSecond(createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime(),
            updatedAt = Instant.ofEpochSecond(updatedAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
        )
    }

    /**
     * Converts domain category to proto category.
     */
    private fun Category.toProto(): ProtoCategory {
        return ProtoCategory.newBuilder()
            .setId(id)
            .setName(name)
            .setColor(color)
            .setIcon(icon)
            .setSortOrder(sortOrder)
            .setCreatedAt(createdAt.atZone(ZoneId.systemDefault()).toEpochSecond())
            .setUpdatedAt(updatedAt.atZone(ZoneId.systemDefault()).toEpochSecond())
            .build()
    }

    /**
     * Gets default color for category based on index.
     */
    private fun getDefaultCategoryColor(index: Int): String {
        val colors = listOf(
            "#6750A4", "#625B71", "#7D5260", "#1B6F3C", "#F57C00",
            "#D32F2F", "#1976D2", "#7B1FA2", "#388E3C", "#F57C00"
        )
        return colors[index % colors.size]
    }

    /**
     * Gets default icon for category based on name.
     */
    private fun getDefaultCategoryIcon(name: String): String {
        return when (name.lowercase()) {
            "entertainment" -> "movie"
            "software" -> "computer"
            "news & media" -> "newspaper"
            "education" -> "school"
            "health & fitness" -> "fitness_center"
            "productivity" -> "work"
            "music" -> "music_note"
            "cloud storage" -> "cloud"
            "gaming" -> "sports_esports"
            else -> "category"
        }
    }
}