package com.example.scheduleiseu.notification

import android.annotation.SuppressLint
import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.scheduleiseu.MainActivity
import com.example.scheduleiseu.data.repository.core.BsuCabinetDataComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LessonAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != LessonNotificationScheduler.ACTION_SHOW_LESSON_NOTIFICATION) return

        val event = LessonNotificationEvent.from(intent) ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        BsuCabinetDataComponent.initialize(appContext)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val preferences = BsuCabinetDataComponent.preferences
                if (!preferences.isLessonNotificationsEnabled()) {
                    BsuCabinetDataComponent.lessonNotificationScheduler.cancelAll()
                    return@launch
                }

                if (canShowNotifications(appContext)) {
                    NotificationChannelHelper.createLessonNotificationChannel(appContext)
                    showNotification(appContext, event)
                }

                BsuCabinetDataComponent.lessonNotificationScheduler.scheduleNext()
            } finally {
                pendingResult.finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(context: Context, event: LessonNotificationEvent) {
        if (!canShowNotifications(context)) return

        val formatted = LessonNotificationFormatter.format(event)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannelHelper.LESSONS_CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_dialog_info)
            .setContentTitle(formatted.title)
            .setContentText(formatted.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(formatted.text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(LESSON_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // The notification permission can be revoked after scheduling.
        }
    }

    private fun canShowNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val LESSON_NOTIFICATION_ID = 5016
    }
}
