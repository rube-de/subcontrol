package com.subcontrol.domain.usecase.notification

import com.subcontrol.data.notification.NotificationScheduler
import com.subcontrol.domain.model.Subscription
import com.subcontrol.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for scheduling subscription notifications.
 * 
 * This use case provides business logic for notification scheduling,
 * including validation and coordination with the notification scheduler.
 */
class ScheduleNotificationUseCase @Inject constructor(
    private val notificationScheduler: NotificationScheduler,
    private val subscriptionRepository: SubscriptionRepository
) {
    
    /**
     * Schedules notifications for a single subscription.
     * 
     * @param subscription The subscription to schedule notifications for
     * @return Result indicating success or failure
     */
    suspend fun scheduleNotifications(subscription: Subscription): Result<Unit> {
        return try {
            if (!subscription.notificationsEnabled) {
                return Result.success(Unit)
            }
            
            // Schedule renewal notification
            val renewalResult = notificationScheduler.scheduleRenewalNotification(subscription)
            if (renewalResult.isFailure) {
                return renewalResult
            }
            
            // Schedule trial ending notification if applicable
            if (subscription.trialEndDate != null) {
                val trialResult = notificationScheduler.scheduleTrialEndingNotification(subscription)
                if (trialResult.isFailure) {
                    return trialResult
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Schedules notifications for all active subscriptions.
     * 
     * @return Result indicating success or failure with count of scheduled notifications
     */
    suspend fun scheduleAllNotifications(): Result<Int> {
        return try {
            val activeSubscriptions = subscriptionRepository.getActiveSubscriptions().first()
            var scheduledCount = 0
            var lastError: Exception? = null
            
            for (subscription in activeSubscriptions) {
                val result = scheduleNotifications(subscription)
                if (result.isSuccess) {
                    scheduledCount++
                } else {
                    lastError = result.exceptionOrNull() as? Exception
                }
            }
            
            if (scheduledCount == 0 && lastError != null) {
                Result.failure(lastError)
            } else {
                Result.success(scheduledCount)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Cancels notifications for a subscription.
     * 
     * @param subscriptionId The ID of the subscription
     * @return Result indicating success or failure
     */
    suspend fun cancelNotifications(subscriptionId: String): Result<Unit> {
        return try {
            require(subscriptionId.isNotBlank()) { "Subscription ID cannot be blank" }
            
            notificationScheduler.cancelNotifications(subscriptionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Reschedules notifications for a subscription.
     * This is useful when subscription details change.
     * 
     * @param subscription The updated subscription
     * @return Result indicating success or failure
     */
    suspend fun rescheduleNotifications(subscription: Subscription): Result<Unit> {
        return try {
            notificationScheduler.rescheduleNotifications(subscription)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Cancels all scheduled notifications.
     * 
     * @return Result indicating success or failure with count of canceled notifications
     */
    suspend fun cancelAllNotifications(): Result<Int> {
        return try {
            val allSubscriptions = subscriptionRepository.getAllSubscriptions().first()
            var canceledCount = 0
            var lastError: Exception? = null
            
            for (subscription in allSubscriptions) {
                val result = cancelNotifications(subscription.id)
                if (result.isSuccess) {
                    canceledCount++
                } else {
                    lastError = result.exceptionOrNull() as? Exception
                }
            }
            
            if (canceledCount == 0 && lastError != null) {
                Result.failure(lastError)
            } else {
                Result.success(canceledCount)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Schedules notifications for subscriptions with upcoming renewals.
     * 
     * @param days Number of days to look ahead for upcoming renewals (default: 7)
     * @return Result indicating success or failure with count of scheduled notifications
     */
    suspend fun scheduleUpcomingRenewalNotifications(days: Int = 7): Result<Int> {
        return try {
            require(days > 0) { "Days must be positive" }
            
            val activeSubscriptions = subscriptionRepository.getActiveSubscriptions().first()
            val upcomingSubscriptions = activeSubscriptions.filter { subscription ->
                val daysUntilRenewal = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(),
                    subscription.nextBillingDate
                )
                daysUntilRenewal <= days && daysUntilRenewal >= 0
            }
            
            var scheduledCount = 0
            var lastError: Exception? = null
            
            for (subscription in upcomingSubscriptions) {
                val result = scheduleNotifications(subscription)
                if (result.isSuccess) {
                    scheduledCount++
                } else {
                    lastError = result.exceptionOrNull() as? Exception
                }
            }
            
            if (scheduledCount == 0 && lastError != null) {
                Result.failure(lastError)
            } else {
                Result.success(scheduledCount)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Schedules notifications for subscriptions with ending trials.
     * 
     * @param days Number of days to look ahead for ending trials (default: 7)
     * @return Result indicating success or failure with count of scheduled notifications
     */
    suspend fun scheduleEndingTrialNotifications(days: Int = 7): Result<Int> {
        return try {
            require(days > 0) { "Days must be positive" }
            
            val activeSubscriptions = subscriptionRepository.getActiveSubscriptions().first()
            val endingTrialSubscriptions = activeSubscriptions.filter { subscription ->
                subscription.trialEndDate?.let { trialEndDate ->
                    val daysUntilTrialEnd = java.time.temporal.ChronoUnit.DAYS.between(
                        java.time.LocalDate.now(),
                        trialEndDate
                    )
                    daysUntilTrialEnd <= days && daysUntilTrialEnd >= 0
                } ?: false
            }
            
            var scheduledCount = 0
            var lastError: Exception? = null
            
            for (subscription in endingTrialSubscriptions) {
                val result = notificationScheduler.scheduleTrialEndingNotification(subscription)
                if (result.isSuccess) {
                    scheduledCount++
                } else {
                    lastError = result.exceptionOrNull() as? Exception
                }
            }
            
            if (scheduledCount == 0 && lastError != null) {
                Result.failure(lastError)
            } else {
                Result.success(scheduledCount)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}