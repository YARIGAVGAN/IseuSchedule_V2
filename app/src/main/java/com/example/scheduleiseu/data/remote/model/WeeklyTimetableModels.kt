package com.example.scheduleiseu.data.remote.model

data class WeeklyTimetableTableData(
    val caption: String,
    val weekStartDate: String?,
    val contextDescription: String?,
    val days: List<WeeklyTimetableDayData>
)

data class WeeklyTimetableDayData(
    val dayName: String,
    val date: String,
    val isCurrentDay: Boolean,
    val lessons: List<WeeklyTimetableLessonData>
)

data class WeeklyTimetableLessonData(
    val timeRange: String,
    val subgroup: String?,
    val disciplineRaw: String,
    val subject: String,
    val lessonType: String?,
    val topic: String?,
    val staff: String?,
    val auditory: String?
)
