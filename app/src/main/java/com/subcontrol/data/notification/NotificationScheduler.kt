package com.subcontrol.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.subcontrol.domain.model.Subscription
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles scheduling and canceling of subscription reminder notifications.
 * Uses AlarmManager for exact timing with WorkManager as fallback.
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Schedules a notification for a subscription renewal.
     * 
     * @param subscription The subscription to schedule notification for
     * @return Result indicating success or failure
     */
    fun scheduleRenewalNotification(subscription: Subscription): Result<Unit> {
        return try {
            if (!subscription.notificationsEnabled) {
                return Result.success(Unit)
            }
            
            val notificationTime = calculateNotificationTime(
                subscription.nextBillingDate,
                subscription.notificationDaysBefore
            )
            
            // Don't schedule notifications for past dates
            if (notificationTime.isBefore(LocalDateTime.now())) {
                return Result.success(Unit)
            }
            
            val triggerTimeMillis = notificationTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            
            if (canScheduleExactAlarm()) {
                scheduleExactAlarm(subscription, triggerTimeMillis)
            } else {
                scheduleWorkManagerNotification(subscription, triggerTimeMillis)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Schedules a trial ending notification.
     * 
     * @param subscription The subscription with trial to schedule notification for
     * @return Result indicating success or failure
     */
    fun scheduleTrialEndingNotification(subscription: Subscription): Result<Unit> {
        return try {
            if (!subscription.notificationsEnabled || subscription.trialEndDate == null) {
                return Result.success(Unit)
            }
            
            val notificationTime = calculateNotificationTime(
                subscription.trialEndDate,
                subscription.notificationDaysBefore
            )
            
            // Don't schedule notifications for past dates
            if (notificationTime.isBefore(LocalDateTime.now())) {
                return Result.success(Unit)
            }
            
            val triggerTimeMillis = notificationTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            
            if (canScheduleExactAlarm()) {
                scheduleExactTrialAlarm(subscription, triggerTimeMillis)
            } else {
                scheduleWorkManagerTrialNotification(subscription, triggerTimeMillis)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Cancels all notifications for a subscription.
     * 
     * @param subscriptionId The ID of the subscription
     * @return Result indicating success or failure
     */
    fun cancelNotifications(subscriptionId: String): Result<Unit> {
        return try {
            // Cancel AlarmManager notifications
            cancelAlarmNotification(subscriptionId, NotificationType.RENEWAL)
            cancelAlarmNotification(subscriptionId, NotificationType.TRIAL_ENDING)
            
            // Cancel WorkManager notifications
            workManager.cancelAllWorkByTag(getWorkTag(subscriptionId, NotificationType.RENEWAL))
            workManager.cancelAllWorkByTag(getWorkTag(subscriptionId, NotificationType.TRIAL_ENDING))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Reschedules all notifications for a subscription.
     * This is useful when subscription details change.
     * 
     * @param subscription The updated subscription
     * @return Result indicating success or failure
     */
    fun rescheduleNotifications(subscription: Subscription): Result<Unit> {
        return try {
            // Cancel existing notifications
            cancelNotifications(subscription.id)
            
            // Schedule new notifications
            scheduleRenewalNotification(subscription)
            scheduleTrialEndingNotification(subscription)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Checks if the app can schedule exact alarms.
     */
    private fun canScheduleExactAlarm(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
    
    /**
     * Schedules an exact alarm using AlarmManager.
     */
    private fun scheduleExactAlarm(subscription: Subscription, triggerTimeMillis: Long) {
        val intent = createAlarmIntent(subscription.id, NotificationType.RENEWAL, subscription.name)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getRequestCode(subscription.id, NotificationType.RENEWAL),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMillis,
            pendingIntent
        )
    }
    
    /**
     * Schedules an exact alarm for trial ending.
     */
    private fun scheduleExactTrialAlarm(subscription: Subscription, triggerTimeMillis: Long) {
        val intent = createAlarmIntent(subscription.id, NotificationType.TRIAL_ENDING, subscription.name)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getRequestCode(subscription.id, NotificationType.TRIAL_ENDING),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMillis,
            pendingIntent
        )
    }
    
    /**
     * Schedules a WorkManager notification as fallback.
     */
    private fun scheduleWorkManagerNotification(subscription: Subscription, triggerTimeMillis: Long) {
        val delay = triggerTimeMillis - System.currentTimeMillis()
        if (delay <= 0) return
        
        val workRequest: WorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(
                "subscription_id" to subscription.id,
                "subscription_name" to subscription.name,
                "notification_type" to NotificationType.RENEWAL.name
            ))
            .addTag(getWorkTag(subscription.id, NotificationType.RENEWAL))
            .build()
        
        workManager.enqueue(workRequest)
    }
    
    /**
     * Schedules a WorkManager trial notification as fallback.
     */
    private fun scheduleWorkManagerTrialNotification(subscription: Subscription, triggerTimeMillis: Long) {
        val delay = triggerTimeMillis - System.currentTimeMillis()
        if (delay <= 0) return
        
        val workRequest: WorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(
                "subscription_id" to subscription.id,
                "subscription_name" to subscription.name,
                "notification_type" to NotificationType.TRIAL_ENDING.name
            ))
            .addTag(getWorkTag(subscription.id, NotificationType.TRIAL_ENDING))
            .build()
        
        workManager.enqueue(workRequest)
    }
    
    /**
     * Cancels an alarm notification.
     */
    private fun cancelAlarmNotification(subscriptionId: String, type: NotificationType) {
        val intent = createAlarmIntent(subscriptionId, type, "")
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getRequestCode(subscriptionId, type),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
    
    /**
     * Creates an intent for alarm notifications.
     */
    private fun createAlarmIntent(subscriptionId: String, type: NotificationType, subscriptionName: String): Intent {
        return Intent(context, AlarmReceiver::class.java).apply {
            putExtra("subscription_id", subscriptionId)
            putExtra("subscription_name", subscriptionName)
            putExtra("notification_type", type.name)
        }
    }
    
    /**
     * Calculates the notification time based on the target date and days before.
     */
    private fun calculateNotificationTime(targetDate: LocalDate, daysBefore: Int): LocalDateTime {
        return targetDate
            .minusDays(daysBefore.toLong())
            .atTime(9, 0) // 9:00 AM default notification time
    }
    
    /**
     * Generates a unique request code for each notification.
     */
    private fun getRequestCode(subscriptionId: String, type: NotificationType): Int {
        return (subscriptionId + type.name).hashCode()
    }
    
    /**
     * Generates a work tag for WorkManager notifications.
     */
    private fun getWorkTag(subscriptionId: String, type: NotificationType): String {
        return "notification_${subscriptionId}_${type.name}"
    }
    
    /**
     * Types of notifications that can be scheduled.
     */
    enum class NotificationType {
        RENEWAL,
        TRIAL_ENDING
    }
}