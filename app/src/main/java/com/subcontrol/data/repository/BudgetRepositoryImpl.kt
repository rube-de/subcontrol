package com.subcontrol.data.repository

import androidx.datastore.core.DataStore
import com.subcontrol.data.model.proto.AppData
import com.subcontrol.data.model.proto.Budget as ProtoBudget
import com.subcontrol.data.model.proto.BudgetList
import com.subcontrol.di.IoDispatcher
import com.subcontrol.domain.model.Budget
import com.subcontrol.domain.repository.BudgetRepository
import com.subcontrol.domain.repository.SubscriptionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of BudgetRepository using Proto DataStore.
 */
@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<AppData>,
    private val subscriptionRepository: SubscriptionRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BudgetRepository {

    override fun getAllBudgets(): Flow<List<Budget>> {
        return dataStore.data
            .map { appData ->
                appData.budgetList.budgetsList.map { it.toDomain() }
            }
            .catch { emit(emptyList()) }
            .flowOn(ioDispatcher)
    }

    override fun getBudgetById(id: String): Flow<Budget?> {
        return getAllBudgets()
            .map { budgets ->
                budgets.find { it.id == id }
            }
            .flowOn(ioDispatcher)
    }

    override fun getCurrentSpending(budgetId: String): Flow<BigDecimal> {
        return combine(
            getBudgetById(budgetId),
            subscriptionRepository.getActiveSubscriptions()
        ) { budget, subscriptions ->
            if (budget == null) {
                BigDecimal.ZERO
            } else {
                val relevantSubscriptions = subscriptions.filter { subscription ->
                    // Include subscription if it matches budget criteria
                    val matchesCategory = budget.includedCategories.isEmpty() || 
                        budget.includedCategories.contains(subscription.category)
                    val matchesSpecific = budget.includedSubscriptions.isEmpty() || 
                        budget.includedSubscriptions.contains(subscription.id)
                    
                    matchesCategory || matchesSpecific
                }
                
                // Calculate total monthly spending
                relevantSubscriptions.sumOf { subscription ->
                    subscription.getMonthlyEquivalent()
                }
            }
        }.flowOn(ioDispatcher)
    }

    override suspend fun addBudget(budget: Budget): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    val currentBudgets = currentData.budgetList.budgetsList.toMutableList()
                    currentBudgets.add(budget.toProto())
                    
                    val updatedBudgetList = BudgetList.newBuilder()
                        .addAllBudgets(currentBudgets)
                        .setLastUpdated(System.currentTimeMillis() / 1000)
                        .build()
                    
                    currentData.toBuilder()
                        .setBudgetList(updatedBudgetList)
                        .build()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateBudget(budget: Budget): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    val currentBudgets = currentData.budgetList.budgetsList.toMutableList()
                    val index = currentBudgets.indexOfFirst { it.id == budget.id }
                    
                    if (index != -1) {
                        currentBudgets[index] = budget.toProto()
                        
                        val updatedBudgetList = BudgetList.newBuilder()
                            .addAllBudgets(currentBudgets)
                            .setLastUpdated(System.currentTimeMillis() / 1000)
                            .build()
                        
                        currentData.toBuilder()
                            .setBudgetList(updatedBudgetList)
                            .build()
                    } else {
                        currentData // No changes if budget not found
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteBudget(id: String): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    val currentBudgets = currentData.budgetList.budgetsList.toMutableList()
                    currentBudgets.removeAll { it.id == id }
                    
                    val updatedBudgetList = BudgetList.newBuilder()
                        .addAllBudgets(currentBudgets)
                        .setLastUpdated(System.currentTimeMillis() / 1000)
                        .build()
                    
                    currentData.toBuilder()
                        .setBudgetList(updatedBudgetList)
                        .build()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Converts proto budget to domain budget.
     */
    private fun ProtoBudget.toDomain(): Budget {
        return Budget(
            id = id,
            name = name,
            monthlyLimit = BigDecimal(monthlyLimit.toString()),
            currency = currency,
            includedCategories = includedCategoriesList,
            includedSubscriptions = includedSubscriptionsList,
            notificationsEnabled = notificationsEnabled,
            notificationThreshold = notificationThreshold,
            createdAt = Instant.ofEpochSecond(createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime(),
            updatedAt = Instant.ofEpochSecond(updatedAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
        )
    }

    /**
     * Converts domain budget to proto budget.
     */
    private fun Budget.toProto(): ProtoBudget {
        return ProtoBudget.newBuilder()
            .setId(id)
            .setName(name)
            .setMonthlyLimit(monthlyLimit.toDouble())
            .setCurrency(currency)
            .addAllIncludedCategories(includedCategories)
            .addAllIncludedSubscriptions(includedSubscriptions)
            .setNotificationsEnabled(notificationsEnabled)
            .setNotificationThreshold(notificationThreshold)
            .setCreatedAt(createdAt.atZone(ZoneId.systemDefault()).toEpochSecond())
            .setUpdatedAt(updatedAt.atZone(ZoneId.systemDefault()).toEpochSecond())
            .build()
    }
}