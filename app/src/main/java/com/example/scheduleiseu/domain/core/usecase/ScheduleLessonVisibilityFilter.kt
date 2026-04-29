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
        val normalizedRegisteredSubgroup = registeredSubgroup.normalizeSubgroup()
        if (showMismatchedSubgroupLessons || normalizedRegisteredSubgroup == null) {
            return week
        }

        val filteredDays = week.days.map { day ->
            day.copy(
                lessons = day.lessons.filter { lesson ->
                    val lessonSubgroup = lesson.subgroup.normalizeSubgroup()
                    lessonSubgroup == null || lessonSubgroup == normalizedRegisteredSubgroup
                }
            )
        }

        return week.copy(
            days = filteredDays,
            currentDay = week.currentDay?.date?.let { currentDate ->
                filteredDays.firstOrNull { it.date == currentDate }
            },
            selectedDay = week.selectedDay?.date?.let { selectedDate ->
                filteredDays.firstOrNull { it.date == selectedDate }
            }
        )
    }

    private fun String?.normalizeSubgroup(): String? {
        val normalized = this
            ?.replace('\u00A0', ' ')
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()

        if (normalized.isBlank()) return null
        if (normalized == "1" || normalized == "2") return normalized

        explicitSubgroupPatterns.forEach { pattern ->
            val match = pattern.matchEntire(normalized) ?: return@forEach
            return match.groupValues.getOrNull(1)?.takeIf { it == "1" || it == "2" }
        }

        return null
    }

    private companion object {
        val explicitSubgroupPatterns = listOf(
            Regex("""(?i)([12])\s*(?:п\s*/\s*гр|п\.?\s*гр|подгр\.?|подгруппа)"""),
            Regex("""(?i)(?:п\s*/\s*гр|п\.?\s*гр|подгр\.?|подгруппа)\s*([12])""")
        )
    }
}
