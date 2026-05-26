package com.example.scheduleiseu.domain.core.model

fun String?.toFormattedLessonTypeLabel(): String? {
    val raw = this?.trim().orEmpty()
    if (raw.isBlank()) return null

    val normalized = raw.lowercase()

    return when {
        normalized.contains("диф") && (
            normalized.contains("зач") ||
                normalized.contains("зачет") ||
                normalized.contains("зачёт")
            ) -> "диф. зачет"

        normalized.contains("зач") ||
            normalized.contains("зачет") ||
            normalized.contains("зачёт") -> "зачет"

        normalized.contains("конс") -> "консультация"
        normalized.contains("экз") -> "экзамен"
        normalized.contains("лаб") -> "лабораторная работа"
        normalized.contains("практ") -> "практическое занятие"
        normalized.contains("лек") -> "лекция"
        else -> raw
    }
}

fun String?.shouldUseMidGreenLessonTypeBadge(): Boolean {
    return when (toFormattedLessonTypeLabel()) {
        "диф. зачет", "консультация", "экзамен", "зачет" -> true
        else -> false
    }
}
