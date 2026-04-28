package com.example.scheduleiseu.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.example.scheduleiseu.data.local.cache.ScheduleCacheCodec
import com.example.scheduleiseu.data.local.db.ScheduleCacheDao
import com.example.scheduleiseu.data.local.preferences.AppPreferencesDataSource
import com.example.scheduleiseu.domain.core.model.ScheduleWeek
import com.example.scheduleiseu.domain.core.model.UserRole
import com.example.scheduleiseu.domain.core.usecase.ScheduleLessonVisibilityFilter

class LessonNotificationScheduler(
    private val context: Context,
    private val preferencesDataSource: AppPreferencesDataSource,
    private val scheduleCacheDao: ScheduleCacheDao,
    private val planner: LessonNotificationPlanner = LessonNotificationPlanner(),
    private val visibilityFilter: ScheduleLessonVisibilityFilter = ScheduleLessonVisibilityFilter()
) {
    suspend fun scheduleNext(nowMillis: Long = System.currentTimeMillis()) {
        if (!preferencesDataSource.isLessonNotificationsEnabled()) {
            cancelAll()
            return
        }

        val event = findNextEvent(nowMillis)
        cancelPendingAlarm()
        if (event == null) return

        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val pendingIntent = createPendingIntent(event)
        val triggerAtMillis = event.triggerAtMillis.coerceAtLeast(nowMillis + MIN_TRIGGER_DELAY_MILLIS)

        if (canUseExactAlarms(alarmManager)) {
            runCatching {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }.onFailure {
                setFallbackAlarm(alarmManager, triggerAtMillis, pendingIntent)
            }
        } else {
            setFallbackAlarm(alarmManager, triggerAtMillis, pendingIntent)
        }
    }

    suspend fun cancelAll() {
        cancelPendingAlarm()
    }

    private suspend fun findNextEvent(nowMillis: Long): LessonNotificationEvent? {
        val role = preferencesDataSource.getUserRole() ?: return null
        val cachedWeeks = when (role) {
            UserRole.STUDENT -> loadStudentScheduleWeeks()
            UserRole.TEACHER -> loadTeacherScheduleWeeks()
        }

        return cachedWeeks
            .asSequence()
            .mapNotNull { week -> planner.findNextEvent(week, nowMillis) }
            .minByOrNull { event -> event.triggerAtMillis }
    }

    private suspend fun loadStudentScheduleWeeks(): List<ScheduleWeek> {
        val profile = preferencesDataSource.getStudentProfile() ?: return emptyList()
        val facultyId = profile.facultyId?.takeIf { it.isNotBlank() } ?: return emptyList()
        val departmentId = profile.departmentId?.takeIf { it.isNotBlank() } ?: return emptyList()
        val courseId = profile.courseId?.takeIf { it.isNotBlank() } ?: return emptyList()
        val groupId = profile.groupId?.takeIf { it.isNotBlank() } ?: return emptyList()
        val ownerId = listOf(facultyId, departmentId, courseId, groupId).joinToString(separator = ":")
        return scheduleCacheDao.getWeeksForOwner(ROLE_STUDENT, ownerId)
            .mapNotNull { entity -> runCatching { ScheduleCacheCodec.fromEntity(entity) }.getOrNull() }
            .map { week ->
                visibilityFilter.filterForStudentSubgroup(
                    week = week,
                    registeredSubgroup = profile.subgroup,
                    showMismatchedSubgroupLessons = false
                )
            }
    }

    private suspend fun loadTeacherScheduleWeeks(): List<ScheduleWeek> {
        val profile = preferencesDataSource.getTeacherProfile() ?: return emptyList()
        val teacherId = profile.teacherId?.trim()?.takeIf { it.isNotBlank() } ?: return emptyList()
        return scheduleCacheDao.getWeeksForOwner(ROLE_TEACHER, teacherId)
            .mapNotNull { entity -> runCatching { ScheduleCacheCodec.fromEntity(entity) }.getOrNull() }
    }

    private fun createPendingIntent(event: LessonNotificationEvent): PendingIntent {
        val intent = Intent(context, LessonAlarmReceiver::class.java).apply {
            action = ACTION_SHOW_LESSON_NOTIFICATION
            event.putTo(this)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_LESSON_NOTIFICATION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelPendingAlarm() {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val pendingIntent = findPendingIntent() ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun findPendingIntent(): PendingIntent? {
        val intent = Intent(context, LessonAlarmReceiver::class.java).apply {
            action = ACTION_SHOW_LESSON_NOTIFICATION
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_LESSON_NOTIFICATION,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canUseExactAlarms(alarmManager: AlarmManager): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun setFallbackAlarm(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                FALLBACK_WINDOW_MILLIS,
                pendingIntent
            )
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    companion object {
        const val ACTION_SHOW_LESSON_NOTIFICATION = "com.example.scheduleiseu.notification.SHOW_LESSON_NOTIFICATION"
        private const val REQUEST_CODE_LESSON_NOTIFICATION = 5015
        private const val ROLE_STUDENT = "student"
        private const val ROLE_TEACHER = "teacher"
        private const val MIN_TRIGGER_DELAY_MILLIS = 1_000L
        private const val FALLBACK_WINDOW_MILLIS = 5 * 60 * 1000L
    }
}
