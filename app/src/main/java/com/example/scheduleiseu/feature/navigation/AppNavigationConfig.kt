package com.example.scheduleiseu.feature.navigation

import com.example.scheduleiseu.domain.core.model.UserRole
import com.example.scheduleiseu.domain.model.SettingsItem

internal const val CACHE_CURRENT_AND_PREVIOUS_WEEK_ID = "cache_current_previous_week"
internal const val LESSON_NOTIFICATIONS_ID = "lesson_notifications"
internal const val SHOW_MISMATCHED_SUBGROUP_LESSONS_ID = "show_mismatched_subgroup_lessons"
internal const val NAVIGATION_OFFLINE_MESSAGE = "Нет подключения к интернету. Показываем сохраненные данные."
internal const val NO_SAVED_SCHEDULE_CACHE_MESSAGE =
    "Без сохраненного расписания уведомления невозможны"
internal const val NOTIFICATIONS_DISABLED_MESSAGE = "Уведомления отключены"

internal fun settingsFrom(
    role: UserRole,
    cacheCurrentAndPreviousWeek: Boolean,
    lessonNotificationsEnabled: Boolean,
    showMismatchedSubgroupLessons: Boolean
): List<SettingsItem> {
    val notificationsSetting = SettingsItem(
        id = LESSON_NOTIFICATIONS_ID,
        title = "Уведомления о парах",
        checked = lessonNotificationsEnabled
    )

    return when (role) {
        UserRole.TEACHER -> listOf(notificationsSetting)
        UserRole.STUDENT -> listOf(
            SettingsItem(
                id = CACHE_CURRENT_AND_PREVIOUS_WEEK_ID,
                title = "Сохранять текущую и следующую неделю в кэш",
                checked = cacheCurrentAndPreviousWeek
            ),
            SettingsItem(
                id = SHOW_MISMATCHED_SUBGROUP_LESSONS_ID,
                title = "Показывать пары другой подгруппы",
                checked = showMismatchedSubgroupLessons
            ),
            notificationsSetting
        )
    }
}

internal fun String.toStudentBootstrapAccountKey(): String? {
    return trim().lowercase().takeIf { it.isNotBlank() }
}
