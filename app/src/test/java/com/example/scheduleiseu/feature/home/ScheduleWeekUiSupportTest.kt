package com.example.scheduleiseu.feature.home

import com.example.scheduleiseu.domain.core.model.Lesson
import com.example.scheduleiseu.domain.core.model.ScheduleDay
import com.example.scheduleiseu.domain.core.model.ScheduleWeek
import com.example.scheduleiseu.domain.core.model.WeekInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleWeekUiSupportTest {

    @Test
    fun `adjacent week navigation stops at list boundaries`() {
        val weeks = listOf(week("1"), week("2"), week("3"))

        assertEquals("3", findAdjacentWeek(weeks, "2", 1)?.value)
        assertEquals("1", findAdjacentWeek(weeks, "2", -1)?.value)
        assertNull(findAdjacentWeek(weeks, "3", 1))
        assertNull(findAdjacentWeek(weeks, "missing", 1))
    }

    @Test
    fun `cached marker utilities preserve server data and enrich cached state`() {
        assertEquals("10.03 - 16.03", "10.03 - 16.03 +".removeCachedWeekMarker())

        val merged = mergeWithCachedWeek(
            week = WeekInfo(value = "1", title = ""),
            cachedWeeks = listOf(WeekInfo(value = "1", title = "Неделя 1", isCurrent = true)),
        )

        assertEquals("Неделя 1", merged.title)
        assertEquals(true, merged.isCurrent)
        assertEquals(true, merged.isCached)
    }

    @Test
    fun `student apply selection prefers manual choice then first day with lessons`() {
        val emptyDay = day("01.01.2026")
        val lessonDay = day("02.01.2026", hasLesson = true)
        val week = ScheduleWeek(week("1"), listOf(emptyDay, lessonDay))

        assertEquals(
            lessonDay,
            resolveStudentSelectedDayForApply(
                week = week,
                selectedWeek = week.week,
                defaultSelectedDay = null,
                manuallySelectedDayByWeekValue = emptyMap(),
            ),
        )
        assertEquals(
            emptyDay,
            resolveStudentSelectedDayForApply(
                week = week,
                selectedWeek = week.week,
                defaultSelectedDay = lessonDay,
                manuallySelectedDayByWeekValue = mapOf("1" to emptyDay.date),
            ),
        )
    }

    private fun week(value: String) = WeekInfo(value = value, title = "Неделя $value")

    private fun day(date: String, hasLesson: Boolean = false): ScheduleDay {
        return ScheduleDay(
            title = date,
            date = date,
            lessons = if (hasLesson) {
                listOf(Lesson(id = date, title = "Пара", startTime = "10:00"))
            } else {
                emptyList()
            },
        )
    }
}
