package com.example.scheduleiseu.notification

import android.content.Intent

enum class LessonNotificationType {
    FIRST_LESSON_SOON,
    NEXT_LESSON,
    DAY_FINISHED
}

data class LessonNotificationEvent(
    val type: LessonNotificationType,
    val triggerAtMillis: Long,
    val lessonTitle: String? = null,
    val classroom: String? = null,
    val minutesUntilStart: Int? = null
) {
    fun putTo(intent: Intent) {
        intent.putExtra(EXTRA_TYPE, type.name)
        intent.putExtra(EXTRA_TRIGGER_AT_MILLIS, triggerAtMillis)
        intent.putExtra(EXTRA_LESSON_TITLE, lessonTitle)
        intent.putExtra(EXTRA_CLASSROOM, classroom)
        minutesUntilStart?.let { intent.putExtra(EXTRA_MINUTES_UNTIL_START, it) }
    }

    companion object {
        const val EXTRA_TYPE = "lesson_notification_type"
        const val EXTRA_TRIGGER_AT_MILLIS = "lesson_notification_trigger_at_millis"
        const val EXTRA_LESSON_TITLE = "lesson_notification_lesson_title"
        const val EXTRA_CLASSROOM = "lesson_notification_classroom"
        const val EXTRA_MINUTES_UNTIL_START = "lesson_notification_minutes_until_start"

        fun from(intent: Intent): LessonNotificationEvent? {
            val typeName = intent.getStringExtra(EXTRA_TYPE) ?: return null
            val type = runCatching { LessonNotificationType.valueOf(typeName) }.getOrNull() ?: return null
            val triggerAtMillis = intent.getLongExtra(EXTRA_TRIGGER_AT_MILLIS, -1L).takeIf { it > 0L }
                ?: return null
            val minutes = if (intent.hasExtra(EXTRA_MINUTES_UNTIL_START)) {
                intent.getIntExtra(EXTRA_MINUTES_UNTIL_START, 0).coerceAtLeast(0)
            } else {
                null
            }

            return LessonNotificationEvent(
                type = type,
                triggerAtMillis = triggerAtMillis,
                lessonTitle = intent.getStringExtra(EXTRA_LESSON_TITLE),
                classroom = intent.getStringExtra(EXTRA_CLASSROOM),
                minutesUntilStart = minutes
            )
        }
    }
}
