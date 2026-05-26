package com.example.scheduleiseu.feature.home

import com.example.scheduleiseu.domain.model.TeacherSearchItem

internal fun filterTeachers(query: String, teachers: List<TeacherSearchItem>): List<TeacherSearchItem> {
    val normalized = query.trim()
    if (normalized.isBlank()) return teachers
    return teachers.filter { teacher ->
        teacher.fullName.contains(normalized, ignoreCase = true) ||
            teacher.subtitle?.contains(normalized, ignoreCase = true) == true
    }
}

internal fun String.isTeacherPlaceholder(): Boolean {
    val normalized = trim().lowercase()
    return normalized == "выберите фамилию преподавателя" || normalized.startsWith("выберите ")
}
