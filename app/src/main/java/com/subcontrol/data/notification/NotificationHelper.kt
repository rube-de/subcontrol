package com.subcontrol.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.subcontrol.R
import com.subcontrol.ui.MainActivity

/**
 * Helper class for creating and showing notifications.
 * Handles notification channels and styling.
 */
class NotificationHelper(private val context: Context) {
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    companion object {
        private const val RENEWAL_CHANNEL_ID = "renewal_notifications"
        private const val TRIAL_CHANNEL_ID = "trial_notifications"
        private const val RENEWAL_NOTIFICATION_ID = 1001
        private const val TRIAL_NOTIFICATION_ID = 1002
    }
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Shows a renewal notification for a subscription.
     */
    fun showRenewalNotification(subscriptionId: String, subscriptionName: String) {
        if (!areNotificationsEnabled()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("subscription_id", subscriptionId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            subscriptionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, RENEWAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.renewal_notification_title))
            .setContentText(context.getString(R.string.renewal_notification_text, subscriptionName))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(context.getString(R.string.renewal_notification_text, subscriptionName)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        
        notificationManager.notify(RENEWAL_NOTIFICATION_ID, notification)
    }
    
    /**
     * Shows a trial ending notification for a subscription.
     */
    fun showTrialEndingNotification(subscriptionId: String, subscriptionName: String) {
        if (!areNotificationsEnabled()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("subscription_id", subscriptionId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            subscriptionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, TRIAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.trial_ending_notification_title))
            .setContentText(context.getString(R.string.trial_ending_notification_text, subscriptionName))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(context.getString(R.string.trial_ending_notification_text, subscriptionName)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        
        notificationManager.notify(TRIAL_NOTIFICATION_ID, notification)
    }
    
    /**
     * Creates notification channels for Android O and above.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Renewal notifications channel
            val renewalChannel = NotificationChannel(
                RENEWAL_CHANNEL_ID,
                context.getString(R.string.renewal_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.renewal_notification_channel_description)
                enableVibration(true)
                enableLights(true)
            }
            
            // Trial ending notifications channel
            val trialChannel = NotificationChannel(
                TRIAL_CHANNEL_ID,
                context.getString(R.string.trial_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.trial_notification_channel_description)
                enableVibration(true)
                enableLights(true)
            }
            
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemNotificationManager.createNotificationChannel(renewalChannel)
            systemNotificationManager.createNotificationChannel(trialChannel)
        }
    }
    
    /**
     * Checks if notifications are enabled for the app.
     */
    private fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }
}