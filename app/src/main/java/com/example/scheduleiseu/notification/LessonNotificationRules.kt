package com.example.scheduleiseu.notification

import java.time.format.DateTimeFormatter
import java.util.Locale

object LessonNotificationRules {
    const val firstLessonOffsetMillis = 15 * 60 * 1000L
    const val foreignLanguageMarker = "иностранный язык"
    val supportedDateFormatters: List<DateTimeFormatter> = listOf(
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru")),
        DateTimeFormatter.ISO_LOCAL_DATE
    )
    val supportedTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("H:mm", Locale("ru"))
    val supportedTimePattern: Regex = Regex("^\\d{1,2}:\\d{2}$")
}
