package com.subcontrol.data.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker for handling scheduled notifications.
 * This is used as a fallback when exact alarms are not available.
 */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val subscriptionId = inputData.getString("subscription_id") ?: return Result.failure()
            val subscriptionName = inputData.getString("subscription_name") ?: return Result.failure()
            val notificationTypeString = inputData.getString("notification_type") ?: return Result.failure()
            
            val notificationType = try {
                NotificationScheduler.NotificationType.valueOf(notificationTypeString)
            } catch (e: IllegalArgumentException) {
                return Result.failure()
            }
            
            // Create and show the notification
            val notificationHelper = NotificationHelper(applicationContext)
            when (notificationType) {
                NotificationScheduler.NotificationType.RENEWAL -> {
                    notificationHelper.showRenewalNotification(subscriptionId, subscriptionName)
                }
                NotificationScheduler.NotificationType.TRIAL_ENDING -> {
                    notificationHelper.showTrialEndingNotification(subscriptionId, subscriptionName)
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}