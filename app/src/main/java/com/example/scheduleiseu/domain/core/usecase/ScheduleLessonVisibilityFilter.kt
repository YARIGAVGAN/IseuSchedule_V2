package com.example.scheduleiseu.domain.core.usecase

import com.example.scheduleiseu.domain.core.model.ScheduleWeek

class ScheduleLessonVisibilityFilter {

    fun filterForNotifications(
        week: ScheduleWeek,
        userRole: com.example.scheduleiseu.domain.core.model.UserRole,
        registeredSubgroup: String?,
        showMismatchedSubgroupLessons: Boolean
    ): ScheduleWeek {
        return when (userRole) {
            com.example.scheduleiseu.domain.core.model.UserRole.TEACHER -> week
            com.example.scheduleiseu.domain.core.model.UserRole.STUDENT -> filterForStudentSubgroup(
                week = week,
                registeredSubgroup = registeredSubgroup,
                showMismatchedSubgroupLessons = showMismatchedSubgroupLessons
            )
        }
    }

    fun filterForStudentSubgroup(
        week: ScheduleWeek,
        registeredSubgroup: String?,
        showMismatchedSubgroupLessons: Boolean
    ): ScheduleWeek {
        if (showMismatchedSubgroupLessons) return week
        val normalizedRegisteredSubgroup = ScheduleLessonFilterRules.normalizeSubgroup(registeredSubgroup)
            ?: return week

        val filteredDays = week.days.map { day ->
            day.copy(
                lessons = day.lessons.filter { lesson ->
                    val lessonSubgroup = ScheduleLessonFilterRules.normalizeSubgroup(lesson.subgroup)

                    lessonSubgroup == null || lessonSubgroup == normalizedRegisteredSubgroup
                }
            )
        }

        return week.copy(days = filteredDays)
    }
}
