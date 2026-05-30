package com.example.scheduleiseu.notification

import com.example.scheduleiseu.domain.core.model.Lesson
import com.example.scheduleiseu.domain.core.model.ScheduleDay
import com.example.scheduleiseu.domain.core.model.ScheduleWeek
import com.example.scheduleiseu.domain.core.model.WeekInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class LessonNotificationPlannerFormatterTest {

    private val zoneId = ZoneId.of("UTC")
    private val planner = LessonNotificationPlanner(zoneId)

    @Test
    fun `planner reads date and time variants from raw schedule range`() {
        val event = planner.findNextEvent(
            week = week(
                lesson(
                    title = "Физика",
                    startTime = "",
                    endTime = null,
                    rawTimeRange = "8.30\u00A0— 9.50",
                    date = "2026-01-02 00:00:00",
                ),
            ),
            nowMillis = millis(2026, 1, 2, 8, 0),
        )

        assertEquals(LessonNotificationType.FIRST_LESSON_SOON, event?.type)
        assertEquals(millis(2026, 1, 2, 8, 15), event?.triggerAtMillis)
    }

    @Test
    fun `planner collapses duplicate foreign language lessons in same slot`() {
        val event = planner.findNextEvent(
            week = week(
                lesson("Иностранный язык", "10:00", "11:20", classroom = "ауд. 1"),
                lesson("Иностранный язык", "10:00", "11:20", classroom = "ауд. 2"),
            ),
            nowMillis = millis(2026, 1, 2, 9, 0),
        )

        assertEquals(LessonNotificationType.FIRST_LESSON_SOON, event?.type)
        assertNull(event?.classroom)
    }

    @Test
    fun `formatter uses russian minute forms and omits blank classroom`() {
        val formatted = LessonNotificationFormatter.format(
            LessonNotificationEvent(
                type = LessonNotificationType.NEXT_LESSON,
                triggerAtMillis = 1L,
                lessonTitle = "Математика",
                lessonType = "практ.",
                classroom = " ",
                minutesUntilStart = 21,
            ),
        )

        assertEquals("Следующая пара: Математика", formatted.title)
        assertEquals("Тип занятия: практическое занятие\nНачало через 21 минуту", formatted.text)
    }

    private fun week(vararg lessons: Lesson): ScheduleWeek {
        return ScheduleWeek(
            week = WeekInfo(value = "02.01.2026", title = "02.01.2026"),
            days = listOf(ScheduleDay("Пятница", "02.01.2026", lessons.toList())),
        )
    }

    private fun lesson(
        title: String,
        startTime: String,
        endTime: String?,
        classroom: String? = null,
        rawTimeRange: String? = null,
        date: String? = "02.01.2026",
    ): Lesson {
        return Lesson(
            id = "$title-$startTime-$classroom",
            title = title,
            classroom = classroom,
            startTime = startTime,
            endTime = endTime,
            rawTimeRange = rawTimeRange,
            date = date,
        )
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(zoneId).toInstant().toEpochMilli()
    }
}
