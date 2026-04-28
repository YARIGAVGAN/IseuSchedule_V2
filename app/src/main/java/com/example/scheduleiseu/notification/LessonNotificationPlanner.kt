package com.example.scheduleiseu.notification

import com.example.scheduleiseu.domain.core.model.Lesson
import com.example.scheduleiseu.domain.core.model.ScheduleWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class LessonNotificationPlanner(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    fun findNextEvent(week: ScheduleWeek, nowMillis: Long): LessonNotificationEvent? {
        val events = week.days.flatMap { day ->
            val timedLessons = day.lessons
                .mapNotNull { lesson -> lesson.toTimedLesson(day.date) }
                .sortedBy { it.startMillis }
                .collapseForeignLanguageDuplicates()

            buildEventsForDay(timedLessons)
        }

        return events
            .asSequence()
            .filter { it.triggerAtMillis > nowMillis }
            .minByOrNull { it.triggerAtMillis }
    }

    private fun buildEventsForDay(lessons: List<TimedLesson>): List<LessonNotificationEvent> {
        if (lessons.isEmpty()) return emptyList()

        val events = mutableListOf<LessonNotificationEvent>()
        val firstLesson = lessons.first()
        events += LessonNotificationEvent(
            type = LessonNotificationType.FIRST_LESSON_SOON,
            triggerAtMillis = firstLesson.startMillis - FIRST_LESSON_OFFSET_MILLIS,
            lessonTitle = firstLesson.lesson.title,
            classroom = firstLesson.lesson.classroom
        )

        lessons.zipWithNext().forEach { (current, next) ->
            val currentEnd = current.endMillis ?: return@forEach
            val minutesUntilStart = Duration
                .ofMillis((next.startMillis - currentEnd).coerceAtLeast(0L))
                .toMinutes()
                .toInt()

            events += LessonNotificationEvent(
                type = LessonNotificationType.NEXT_LESSON,
                triggerAtMillis = currentEnd,
                lessonTitle = next.lesson.title,
                classroom = next.lesson.classroom,
                minutesUntilStart = minutesUntilStart
            )
        }

        lessons.last().endMillis?.let { lastEndMillis ->
            events += LessonNotificationEvent(
                type = LessonNotificationType.DAY_FINISHED,
                triggerAtMillis = lastEndMillis
            )
        }

        return events
    }

    private fun List<TimedLesson>.collapseForeignLanguageDuplicates(): List<TimedLesson> {
        if (size <= 1) return this

        val result = mutableListOf<TimedLesson>()
        val seenForeignLanguageSlots = mutableSetOf<ForeignLanguageSlotKey>()

        for (timedLesson in this) {
            if (!timedLesson.lesson.isForeignLanguageLesson()) {
                result += timedLesson
                continue
            }

            val key = ForeignLanguageSlotKey(
                startMillis = timedLesson.startMillis,
                endMillis = timedLesson.endMillis
            )

            if (seenForeignLanguageSlots.add(key)) {
                result += timedLesson.copy(
                    lesson = timedLesson.lesson.copy(classroom = null)
                )
            }
        }

        return result
    }

    private fun Lesson.isForeignLanguageLesson(): Boolean {
        return title.contains(FOREIGN_LANGUAGE_MARKER, ignoreCase = true)
    }

    private fun Lesson.toTimedLesson(dayDate: String): TimedLesson? {
        val date = (date?.takeIf { it.isNotBlank() } ?: dayDate).toLocalDateOrNull() ?: return null
        val rawRange = rawTimeRange.orEmpty()
        val parsedStart = startTime.toLocalTimeOrNull() ?: rawRange.parseRawRangeStart()
        val parsedEnd = endTime?.toLocalTimeOrNull() ?: rawRange.parseRawRangeEnd()
        val start = parsedStart ?: return null

        return TimedLesson(
            lesson = this,
            startMillis = LocalDateTime.of(date, start).toMillis(),
            endMillis = parsedEnd?.let { LocalDateTime.of(date, it).toMillis() }
        )
    }

    private fun LocalDateTime.toMillis(): Long {
        return atZone(zoneId).toInstant().toEpochMilli()
    }

    private fun String.toLocalDateOrNull(): LocalDate? {
        val normalized = trim().substringBefore(' ')
        return DATE_FORMATTERS.firstNotNullOfOrNull { formatter ->
            runCatching { LocalDate.parse(normalized, formatter) }.getOrNull()
        }
    }

    private fun String.toLocalTimeOrNull(): LocalTime? {
        val normalized = normalizeTime()
        if (!TIME_PATTERN.matches(normalized)) return null
        return runCatching { LocalTime.parse(normalized, TIME_FORMATTER) }.getOrNull()
    }

    private fun String.parseRawRangeStart(): LocalTime? = parseRawRangePart(index = 0)

    private fun String.parseRawRangeEnd(): LocalTime? = parseRawRangePart(index = 1)


    private fun String.parseRawRangePart(index: Int): LocalTime? {
        val parts = replace('–', '-')
            .replace('—', '-')
            .replace('\u00A0', ' ')
            .split('-', limit = 2)
            .map { it.trim() }
        return parts.getOrNull(index)?.toLocalTimeOrNull()
    }

    private fun String.normalizeTime(): String {
        val parts = trim()
            .replace('.', ':')
            .replace('\u00A0', ' ')
            .split(':', limit = 2)
            .map { it.trim() }
        if (parts.size != 2) return trim()
        val hour = parts[0].toIntOrNull() ?: return trim()
        val minute = parts[1].toIntOrNull() ?: return trim()
        return "%d:%02d".format(Locale.US, hour, minute)
    }

    private data class TimedLesson(
        val lesson: Lesson,
        val startMillis: Long,
        val endMillis: Long?
    )

    private data class ForeignLanguageSlotKey(
        val startMillis: Long,
        val endMillis: Long?
    )

    private companion object {
        const val FIRST_LESSON_OFFSET_MILLIS = 15 * 60 * 1000L
        const val FOREIGN_LANGUAGE_MARKER = "иностранный язык"
        val DATE_FORMATTERS: List<DateTimeFormatter> = listOf(
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru")),
            DateTimeFormatter.ISO_LOCAL_DATE
        )
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm", Locale("ru"))
        val TIME_PATTERN: Regex = Regex("^\\d{1,2}:\\d{2}$")
    }
}
