package com.example.scheduleiseu.notification

import com.example.scheduleiseu.domain.core.model.Lesson
import com.example.scheduleiseu.domain.core.model.ScheduleDay
import com.example.scheduleiseu.domain.core.model.ScheduleWeek
import com.example.scheduleiseu.domain.core.model.UserRole
import com.example.scheduleiseu.domain.core.model.WeekInfo
import com.example.scheduleiseu.domain.core.usecase.ScheduleLessonVisibilityFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.ZoneId

class LessonNotificationsFilteringTest {

    private val visibilityFilter = ScheduleLessonVisibilityFilter()

    @Test
    fun `student notification filter matches visible lessons for subgroup`() {
        val week = scheduleWeek(
            lessons = listOf(
                lesson(
                    id = "subgroup-1",
                    title = "Видимая пара",
                    subgroup = "1 подгр.",
                    startTime = "10:00",
                    endTime = "11:20"
                ),
                lesson(
                    id = "subgroup-2",
                    title = "Скрытая пара",
                    subgroup = "2 подгр.",
                    startTime = "12:00",
                    endTime = "13:20"
                )
            )
        )

        val screenWeek = visibilityFilter.filterForStudentSubgroup(
            week = week,
            registeredSubgroup = "1",
            showMismatchedSubgroupLessons = false
        )
        val notificationWeek = visibilityFilter.filterForNotifications(
            week = week,
            userRole = UserRole.STUDENT,
            registeredSubgroup = "1",
            showMismatchedSubgroupLessons = false
        )

        assertEquals(screenWeek.days.first().lessons, notificationWeek.days.first().lessons)
        assertEquals(listOf("Видимая пара"), notificationWeek.days.first().lessons.map { it.title })
    }

    @Test
    fun `planner picks next visible lesson after subgroup filtering`() {
        val planner = LessonNotificationPlanner(zoneId = ZoneId.of("UTC"))
        val week = scheduleWeek(
            lessons = listOf(
                lesson(
                    id = "subgroup-2",
                    title = "Чужая подгруппа",
                    subgroup = "2 подгр.",
                    startTime = "10:00",
                    endTime = "11:20"
                ),
                lesson(
                    id = "subgroup-1",
                    title = "Своя подгруппа",
                    subgroup = "1 подгр.",
                    startTime = "12:00",
                    endTime = "13:20"
                )
            )
        )

        val filteredWeek = visibilityFilter.filterForNotifications(
            week = week,
            userRole = UserRole.STUDENT,
            registeredSubgroup = "1",
            showMismatchedSubgroupLessons = false
        )

        val event = planner.findNextEvent(
            week = filteredWeek,
            nowMillis = 0L
        )

        assertNotNull(event)
        assertEquals("Своя подгруппа", event?.lessonTitle)
    }

    private fun scheduleWeek(lessons: List<Lesson>): ScheduleWeek {
        val day = ScheduleDay(
            title = "Понедельник",
            date = "01.01.2026",
            lessons = lessons
        )
        return ScheduleWeek(
            week = WeekInfo(value = "01.01.2026", title = "01.01.2026"),
            days = listOf(day),
            selectedDay = day
        )
    }

    private fun lesson(
        id: String,
        title: String,
        subgroup: String,
        startTime: String,
        endTime: String
    ): Lesson {
        return Lesson(
            id = id,
            title = title,
            type = "практ. зан.",
            startTime = startTime,
            endTime = endTime,
            subgroup = subgroup,
            date = "01.01.2026",
            rawTimeRange = "$startTime-$endTime"
        )
    }
}
