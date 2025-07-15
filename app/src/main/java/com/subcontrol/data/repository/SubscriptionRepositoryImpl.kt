package com.subcontrol.data.repository

import androidx.datastore.core.DataStore
import com.subcontrol.data.mapper.SubscriptionMapper.toDomain
import com.subcontrol.data.mapper.SubscriptionMapper.toProto
import com.subcontrol.data.model.proto.AppData
import com.subcontrol.data.model.proto.SubscriptionList
import com.subcontrol.di.IoDispatcher
import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.model.SubscriptionStatus
import com.subcontrol.domain.repository.SubscriptionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SubscriptionRepository using Proto DataStore.
 */
@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<AppData>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SubscriptionRepository {

    override fun getAllSubscriptions(): Flow<List<Subscription>> {
        return dataStore.data
            .map { appData ->
                appData.subscriptionList.subscriptionsList.map { it.toDomain() }
            }
            .catch { emit(emptyList()) }
            .flowOn(ioDispatcher)
    }

    override fun getActiveSubscriptions(): Flow<List<Subscription>> {
        return getAllSubscriptions()
            .map { subscriptions ->
                subscriptions.filter { it.isActive }
            }
            .flowOn(ioDispatcher)
    }

    override fun getSubscriptionById(id: String): Flow<Subscription?> {
        return getAllSubscriptions()
            .map { subscriptions ->
                subscriptions.find { it.id == id }
            }
            .flowOn(ioDispatcher)
    }

    override fun getSubscriptionsByCategory(category: String): Flow<List<Subscription>> {
        return getAllSubscriptions()
            .map { subscriptions ->
                subscriptions.filter { it.category.equals(category, ignoreCase = true) }
            }
            .flowOn(ioDispatcher)
    }

    override fun searchSubscriptions(query: String): Flow<List<Subscription>> {
        return getAllSubscriptions()
            .map { subscriptions ->
                subscriptions.filter { subscription ->
                    subscription.name.contains(query, ignoreCase = true) ||
                    subscription.description.contains(query, ignoreCase = true) ||
                    subscription.category.contains(query, ignoreCase = true) ||
                    subscription.tags.any { tag -> tag.contains(query, ignoreCase = true) }
                }
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun addSubscription(subscription: Subscription): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    val currentSubscriptions = currentData.subscriptionList.subscriptionsList.toMutableList()
                    currentSubscriptions.add(subscription.toProto())
                    
                    val updatedSubscriptionList = SubscriptionList.newBuilder()
                        .addAllSubscriptions(currentSubscriptions)
                        .setLastUpdated(System.currentTimeMillis() / 1000)
                        .setVersion(currentData.subscriptionList.version + 1)
                        .build()
                    
                    currentData.toBuilder()
                        .setSubscriptionList(updatedSubscriptionList)
                        .build()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateSubscription(subscription: Subscription): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    val currentSubscriptions = currentData.subscriptionList.subscriptionsList.toMutableList()
                    val index = currentSubscriptions.indexOfFirst { it.id == subscription.id }
                    
                    if (index != -1) {
                        currentSubscriptions[index] = subscription.toProto()
                        
                        val updatedSubscriptionList = SubscriptionList.newBuilder()
                            .addAllSubscriptions(currentSubscriptions)
                            .setLastUpdated(System.currentTimeMillis() / 1000)
                            .setVersion(currentData.subscriptionList.version + 1)
                            .build()
                        
                        currentData.toBuilder()
                            .setSubscriptionList(updatedSubscriptionList)
                            .build()
                    } else {
                        currentData // No changes if subscription not found
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteSubscription(id: String): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    val currentSubscriptions = currentData.subscriptionList.subscriptionsList.toMutableList()
                    currentSubscriptions.removeAll { it.id == id }
                    
                    val updatedSubscriptionList = SubscriptionList.newBuilder()
                        .addAllSubscriptions(currentSubscriptions)
                        .setLastUpdated(System.currentTimeMillis() / 1000)
                        .setVersion(currentData.subscriptionList.version + 1)
                        .build()
                    
                    currentData.toBuilder()
                        .setSubscriptionList(updatedSubscriptionList)
                        .build()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteAllSubscriptions(): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    val updatedSubscriptionList = SubscriptionList.newBuilder()
                        .setLastUpdated(System.currentTimeMillis() / 1000)
                        .setVersion(currentData.subscriptionList.version + 1)
                        .build()
                    
                    currentData.toBuilder()
                        .setSubscriptionList(updatedSubscriptionList)
                        .build()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateBillingDate(id: String, newBillingDate: LocalDate): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    val currentSubscriptions = currentData.subscriptionList.subscriptionsList.toMutableList()
                    val index = currentSubscriptions.indexOfFirst { it.id == id }
                    
                    if (index != -1) {
                        val subscription = currentSubscriptions[index]
                        val updatedSubscription = subscription.toBuilder()
                            .setNextBillingDate(newBillingDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond())
                            .setUpdatedAt(System.currentTimeMillis() / 1000)
                            .build()
                        
                        currentSubscriptions[index] = updatedSubscription
                        
                        val updatedSubscriptionList = SubscriptionList.newBuilder()
                            .addAllSubscriptions(currentSubscriptions)
                            .setLastUpdated(System.currentTimeMillis() / 1000)
                            .setVersion(currentData.subscriptionList.version + 1)
                            .build()
                        
                        currentData.toBuilder()
                            .setSubscriptionList(updatedSubscriptionList)
                            .build()
                    } else {
                        currentData // No changes if subscription not found
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateStatus(id: String, status: SubscriptionStatus): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                dataStore.updateData { currentData ->
                    val currentSubscriptions = currentData.subscriptionList.subscriptionsList.toMutableList()
                    val index = currentSubscriptions.indexOfFirst { it.id == id }
                    
                    if (index != -1) {
                        val subscription = currentSubscriptions[index]
                        val protoStatus = when (status) {
                            SubscriptionStatus.ACTIVE -> com.subcontrol.data.model.proto.SubscriptionStatus.ACTIVE
                            SubscriptionStatus.TRIAL -> com.subcontrol.data.model.proto.SubscriptionStatus.TRIAL
                            SubscriptionStatus.PAUSED -> com.subcontrol.data.model.proto.SubscriptionStatus.PAUSED
                            SubscriptionStatus.CANCELLED -> com.subcontrol.data.model.proto.SubscriptionStatus.CANCELLED
                            SubscriptionStatus.EXPIRED -> com.subcontrol.data.model.proto.SubscriptionStatus.EXPIRED
                        }
                        
                        val updatedSubscription = subscription.toBuilder()
                            .setStatus(protoStatus)
                            .setUpdatedAt(System.currentTimeMillis() / 1000)
                            .build()
                        
                        currentSubscriptions[index] = updatedSubscription
                        
                        val updatedSubscriptionList = SubscriptionList.newBuilder()
                            .addAllSubscriptions(currentSubscriptions)
                            .setLastUpdated(System.currentTimeMillis() / 1000)
                            .setVersion(currentData.subscriptionList.version + 1)
                            .build()
                        
                        currentData.toBuilder()
                            .setSubscriptionList(updatedSubscriptionList)
                            .build()
                    } else {
                        currentData // No changes if subscription not found
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}