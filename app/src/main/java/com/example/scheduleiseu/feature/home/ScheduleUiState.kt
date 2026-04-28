package com.example.scheduleiseu.feature.home

import com.example.scheduleiseu.domain.core.model.Lesson
import com.example.scheduleiseu.domain.core.model.ScheduleContext
import com.example.scheduleiseu.domain.core.model.ScheduleDay
import com.example.scheduleiseu.domain.core.model.WeekInfo
import com.example.scheduleiseu.domain.model.TeacherSearchItem

enum class ScheduleLoadingStage {
    Initial,
    BackgroundRefresh,
    Selection,
    RetryAfterNetworkRestore
}

data class ScheduleUiState(
    val currentWeek: WeekInfo? = null,
    val selectedWeek: WeekInfo? = null,
    val days: List<ScheduleDay> = emptyList(),
    val selectedDay: ScheduleDay? = null,
    val lessonsForSelectedDay: List<Lesson> = emptyList(),
    val scheduleContext: ScheduleContext? = null,
    val availableWeeks: List<WeekInfo> = emptyList(),
    val availableGroups: List<String> = emptyList(),
    val selectedGroupTitle: String? = null,
    val teacherQuery: String = "",
    val availableTeachers: List<TeacherSearchItem> = emptyList(),
    val filteredTeachers: List<TeacherSearchItem> = emptyList(),
    val selectedTeacherName: String? = null,
    val isTemporaryContext: Boolean = false,
    val isLoading: Boolean = false,
    val loadingStage: ScheduleLoadingStage? = null,
    val isOfflineMode: Boolean = false,
    val offlineMessage: String? = null,
    val errorMessage: String? = null
)
