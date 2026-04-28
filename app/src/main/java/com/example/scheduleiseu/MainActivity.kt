package com.example.scheduleiseu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.scheduleiseu.app.AppRoot
import com.example.scheduleiseu.core.designsystem.theme.ScheduleIsEuTheme
import com.example.scheduleiseu.data.repository.core.BsuCabinetDataComponent
import com.example.scheduleiseu.notification.NotificationChannelHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BsuCabinetDataComponent.initialize(applicationContext)
        NotificationChannelHelper.createLessonNotificationChannel(this)
        setContent {
            ScheduleIsEuTheme {
                AppRoot()
            }
        }
    }
}
