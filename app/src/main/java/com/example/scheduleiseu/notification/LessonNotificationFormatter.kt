package com.example.scheduleiseu.notification

import com.example.scheduleiseu.domain.core.model.toFormattedLessonTypeLabel

data class FormattedLessonNotification(
    val title: String,
    val text: String
)

object LessonNotificationFormatter {
    fun format(event: LessonNotificationEvent): FormattedLessonNotification {
        val lines = when (event.type) {
            LessonNotificationType.FIRST_LESSON_SOON -> buildList {
                add("Первая пара: ${event.lessonTitle.orEmpty()}")
                event.lessonType.toFormattedLessonTypeLabel()?.let { add("Тип занятия: $it") }
                event.classroom?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
                add("Начало через 15 минут")
            }

            LessonNotificationType.NEXT_LESSON -> buildList {
                add("Следующая пара: ${event.lessonTitle.orEmpty()}")
                event.lessonType.toFormattedLessonTypeLabel()?.let { add("Тип занятия: $it") }
                event.classroom?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
                val minutes = event.minutesUntilStart?.coerceAtLeast(0) ?: 0
                add(if (minutes == 0) "Начало сейчас" else "Начало через $minutes ${minutes.minutesWord()}")
            }

            LessonNotificationType.DAY_FINISHED -> listOf(
                "Учебный день закончен",
                "Хорошего отдыха!"
            )
        }

        return FormattedLessonNotification(
            title = lines.first(),
            text = lines.drop(1).joinToString(separator = "\n").ifBlank { lines.first() }
        )
    }

    private fun Int.minutesWord(): String {
        val value = this % 100
        if (value in 11..14) return "минут"
        return when (this % 10) {
            1 -> "минуту"
            in 2..4 -> "минуты"
            else -> "минут"
        }
    }
}
