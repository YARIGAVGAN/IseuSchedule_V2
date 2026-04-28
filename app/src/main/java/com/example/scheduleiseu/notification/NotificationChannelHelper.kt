package com.example.scheduleiseu.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

object NotificationChannelHelper {
    const val LESSONS_CHANNEL_ID = "lesson_notifications"

    fun createLessonNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            LESSONS_CHANNEL_ID,
            "Уведомления о парах",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Напоминания о начале и окончании пар"
        }

        context.getSystemService<NotificationManager>()
            ?.createNotificationChannel(channel)
    }
}
