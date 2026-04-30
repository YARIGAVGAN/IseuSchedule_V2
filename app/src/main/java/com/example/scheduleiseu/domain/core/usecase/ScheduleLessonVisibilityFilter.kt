package com.example.scheduleiseu.domain.core.usecase

import android.util.Log
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
        Log.d(
            "SUBGROUP_FILTER",
            "registeredSubgroup=$registeredSubgroup"
        )
        val normalizedRegisteredSubgroup = registeredSubgroup.normalizeSubgroup()
            ?: return week

        val filteredDays = week.days.map { day ->
            day.copy(
                lessons = day.lessons.filter { lesson ->
                    val lessonSubgroup = lesson.subgroup.normalizeSubgroup()

                    lessonSubgroup == null || lessonSubgroup == normalizedRegisteredSubgroup
                }
            )
        }

        return week.copy(days = filteredDays)
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
