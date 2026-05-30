package com.example.scheduleiseu.domain.core.usecase

import com.example.scheduleiseu.domain.core.model.Lesson
import com.example.scheduleiseu.domain.core.model.ScheduleDay
import com.example.scheduleiseu.domain.core.model.ScheduleWeek
import com.example.scheduleiseu.domain.core.model.WeekInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ScheduleLessonFilterRulesTest {

    @Test
    fun `normalize subgroup supports values returned by schedule sources`() {
        assertEquals("1", ScheduleLessonFilterRules.normalizeSubgroup("1"))
        assertEquals("1", ScheduleLessonFilterRules.normalizeSubgroup("1 п/гр"))
        assertEquals("2", ScheduleLessonFilterRules.normalizeSubgroup("подгр. 2"))
        assertEquals("2", ScheduleLessonFilterRules.normalizeSubgroup("  2\u00A0подгруппа "))
        assertNull(ScheduleLessonFilterRules.normalizeSubgroup("ФЭМ В51МД1/1"))
        assertNull(ScheduleLessonFilterRules.normalizeSubgroup(null))
    }

    @Test
    fun `student filter keeps shared lessons and own subgroup`() {
        val week = weekWithSubgroups(null, "1 подгр.", "2 подгр.")

        val filtered = ScheduleLessonVisibilityFilter().filterForStudentSubgroup(
            week = week,
            registeredSubgroup = "1",
            showMismatchedSubgroupLessons = false,
        )

        assertEquals(listOf("shared", "1 подгр."), filtered.days.single().lessons.map { it.id })
    }

    @Test
    fun `student filter returns original week when filtering is disabled or subgroup is unknown`() {
        val week = weekWithSubgroups("1 подгр.", "2 подгр.")
        val filter = ScheduleLessonVisibilityFilter()

        assertSame(week, filter.filterForStudentSubgroup(week, "1", true))
        assertSame(week, filter.filterForStudentSubgroup(week, null, false))
    }

    private fun weekWithSubgroups(vararg subgroups: String?): ScheduleWeek {
        val lessons = subgroups.mapIndexed { index, subgroup ->
            Lesson(
                id = subgroup ?: "shared",
                title = "Пара $index",
                startTime = "10:00",
                subgroup = subgroup,
            )
        }
        return ScheduleWeek(
            week = WeekInfo(value = "week", title = "Неделя"),
            days = listOf(ScheduleDay(title = "Понедельник", date = "01.01.2026", lessons = lessons)),
        )
    }
}
