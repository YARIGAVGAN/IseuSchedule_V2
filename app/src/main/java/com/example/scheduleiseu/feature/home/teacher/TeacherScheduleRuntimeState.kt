package com.example.scheduleiseu.feature.home

internal class TeacherScheduleRuntimeState {
    var cachedTeacherWeekValues: Set<String> = emptySet()
    val manuallySelectedDayByWeekValue = mutableMapOf<String, String>()
}
