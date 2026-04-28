package com.example.scheduleiseu.domain.core.model

data class WeekInfo(
    val value: String,
    val title: String,
    val isCurrent: Boolean = false,
    val isCached: Boolean = false
)

data class Lesson(
    val id: String,
    val title: String,
    val type: String? = null,
    val teacherName: String? = null,
    val classroom: String? = null,
    val startTime: String,
    val endTime: String? = null,
    val subgroup: String? = null,
    val note: String? = null,
    val dayTitle: String? = null,
    val date: String? = null,
    val topic: String? = null,
    val rawTimeRange: String? = null
)

data class ScheduleDay(
    val title: String,
    val date: String,
    val lessons: List<Lesson>,
    val isCurrentDay: Boolean = false
)

data class ScheduleWeek(
    val week: WeekInfo,
    val days: List<ScheduleDay>,
    val caption: String? = null,
    val contextTitle: String? = null,
    val currentDay: ScheduleDay? = null,
    val selectedDay: ScheduleDay? = null
)

data class ScheduleOption(
    val id: String,
    val title: String,
    val selected: Boolean = false
)

data class ScheduleContext(
    val userRole: UserRole,
    val faculties: List<ScheduleOption> = emptyList(),
    val departments: List<ScheduleOption> = emptyList(),
    val courses: List<ScheduleOption> = emptyList(),
    val groups: List<ScheduleOption> = emptyList(),
    val teachers: List<ScheduleOption> = emptyList(),
    val weeks: List<WeekInfo> = emptyList(),
    val selectedFacultyId: String? = null,
    val selectedDepartmentId: String? = null,
    val selectedCourseId: String? = null,
    val selectedGroupId: String? = null,
    val selectedTeacherId: String? = null,
    val selectedWeek: WeekInfo? = null,
    val currentWeek: WeekInfo? = null
)
