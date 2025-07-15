package com.subcontrol.domain.repository

import com.subcontrol.domain.model.Budget
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * Repository interface for budget data operations.
 */
interface BudgetRepository {

    /**
     * Observes all budgets.
     * 
     * @return Flow of budget list
     */
    fun getAllBudgets(): Flow<List<Budget>>

    /**
     * Gets a specific budget by ID.
     * 
     * @param id Budget ID
     * @return Flow of budget or null if not found
     */
    fun getBudgetById(id: String): Flow<Budget?>

    /**
     * Calculates current spending for a budget.
     * 
     * @param budgetId Budget ID
     * @return Flow of current spending amount
     */
    fun getCurrentSpending(budgetId: String): Flow<BigDecimal>

    /**
     * Adds a new budget.
     * 
     * @param budget Budget to add
     * @return Result indicating success or failure
     */
    suspend fun addBudget(budget: Budget): Result<Unit>

    /**
     * Updates an existing budget.
     * 
     * @param budget Budget to update
     * @return Result indicating success or failure
     */
    suspend fun updateBudget(budget: Budget): Result<Unit>

    /**
     * Deletes a budget.
     * 
     * @param id Budget ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteBudget(id: String): Result<Unit>
}