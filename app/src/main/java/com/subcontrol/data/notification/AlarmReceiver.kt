package com.subcontrol.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint

/**
 * BroadcastReceiver for handling scheduled alarm notifications.
 * This receiver is triggered when an AlarmManager alarm fires.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val subscriptionId = intent.getStringExtra("subscription_id") ?: return
        val subscriptionName = intent.getStringExtra("subscription_name") ?: return
        val notificationTypeString = intent.getStringExtra("notification_type") ?: return
        
        val notificationType = try {
            NotificationScheduler.NotificationType.valueOf(notificationTypeString)
        } catch (e: IllegalArgumentException) {
            return
        }
        
        // Create and show the notification
        val notificationHelper = NotificationHelper(context)
        when (notificationType) {
            NotificationScheduler.NotificationType.RENEWAL -> {
                notificationHelper.showRenewalNotification(subscriptionId, subscriptionName)
            }
            NotificationScheduler.NotificationType.TRIAL_ENDING -> {
                notificationHelper.showTrialEndingNotification(subscriptionId, subscriptionName)
            }
        }
    }
}