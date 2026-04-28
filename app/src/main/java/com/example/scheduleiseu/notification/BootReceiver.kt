package com.example.scheduleiseu.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.scheduleiseu.data.repository.core.BsuCabinetDataComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        BsuCabinetDataComponent.initialize(appContext)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val scheduler = BsuCabinetDataComponent.lessonNotificationScheduler
                if (BsuCabinetDataComponent.preferences.isLessonNotificationsEnabled()) {
                    scheduler.scheduleNext()
                } else {
                    scheduler.cancelAll()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
