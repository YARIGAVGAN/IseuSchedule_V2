package com.example.scheduleiseu.feature.home

import com.example.scheduleiseu.domain.core.model.ScheduleContext
import com.example.scheduleiseu.domain.core.model.ScheduleDay
import com.example.scheduleiseu.domain.core.model.ScheduleWeek
import com.example.scheduleiseu.domain.core.model.WeekInfo

internal fun String.removeCachedWeekMarker(): String {
    return removeSuffix(" +").trim()
}

internal fun findAdjacentWeek(
    weeks: List<WeekInfo>,
    selectedWeekValue: String?,
    step: Int
): WeekInfo? {
    if (weeks.isEmpty() || selectedWeekValue == null) return null
    val currentIndex = weeks.indexOfFirst { it.value == selectedWeekValue }
    if (currentIndex == -1) return null
    return weeks.getOrNull(currentIndex + step)
}

internal fun WeekInfo.withCachedFlag(cachedWeekValues: Set<String>): WeekInfo {
    return copy(isCached = isCached || value in cachedWeekValues)
}

internal fun markCachedWeeks(
    weeks: List<WeekInfo>,
    cachedWeekValues: Set<String>
): List<WeekInfo> {
    return weeks.map { it.withCachedFlag(cachedWeekValues) }
}

internal fun ScheduleContext.markCachedWeeks(cachedWeekValues: Set<String>): ScheduleContext {
    return copy(
        currentWeek = currentWeek?.withCachedFlag(cachedWeekValues),
        selectedWeek = selectedWeek?.withCachedFlag(cachedWeekValues),
        weeks = markCachedWeeks(weeks, cachedWeekValues)
    )
}

internal fun mergeWithCachedWeek(week: WeekInfo, cachedWeeks: List<WeekInfo>): WeekInfo {
    val cached = cachedWeeks.firstOrNull { it.value == week.value } ?: return week
    return week.copy(
        title = if (week.title.isBlank()) cached.title else week.title,
        isCurrent = week.isCurrent || cached.isCurrent,
        isCached = true
    )
}

internal fun resolveStudentSelectedDayForApply(
    week: ScheduleWeek,
    selectedWeek: WeekInfo,
    defaultSelectedDay: ScheduleDay?,
    manuallySelectedDayByWeekValue: Map<String, String>
): ScheduleDay? {
    val manuallySelectedDate = manuallySelectedDayByWeekValue[selectedWeek.value]
    val days = week.days

    return manuallySelectedDate
        ?.let { date -> days.firstOrNull { it.date == date } }
        ?: defaultSelectedDay
            ?.date
            ?.let { date -> days.firstOrNull { it.date == date } }
        ?: week.selectedDay
            ?.date
            ?.let { date -> days.firstOrNull { it.date == date } }
        ?: week.currentDay
            ?.date
            ?.let { date -> days.firstOrNull { it.date == date } }
        ?: days.firstOrNull { it.isCurrentDay }
        ?: days.firstOrNull { it.lessons.isNotEmpty() }
        ?: days.firstOrNull()
}

internal fun resolveStudentInitialSelectedDay(
    week: ScheduleWeek,
    ownCurrentDayDate: String?
): ScheduleDay? {
    val days = week.days

    return week.currentDay
        ?.date
        ?.let { date -> days.firstOrNull { it.date == date } }
        ?: if (week.week.isCurrent) {
            ownCurrentDayDate?.let { expected -> days.firstOrNull { it.date == expected } }
        } else {
            null
        }
        ?: week.selectedDay
            ?.date
            ?.let { date -> days.firstOrNull { it.date == date } }
        ?: days.firstOrNull { it.isCurrentDay }
        ?: days.firstOrNull { it.lessons.isNotEmpty() }
        ?: days.firstOrNull()
}

internal fun resolveTeacherSelectedDayForApply(
    days: List<ScheduleDay>,
    selectedWeek: WeekInfo,
    defaultSelectedDay: ScheduleDay?,
    manuallySelectedDayByWeekValue: Map<String, String>
): ScheduleDay? {
    val manuallySelectedDate = manuallySelectedDayByWeekValue[selectedWeek.value]
    return manuallySelectedDate?.let { date -> days.firstOrNull { it.date == date } }
        ?: defaultSelectedDay
}

internal fun resolveTeacherInitialSelectedDay(week: ScheduleWeek): ScheduleDay? {
    return week.currentDay
        ?: week.selectedDay
        ?: week.days.firstOrNull { it.lessons.isNotEmpty() }
        ?: week.days.firstOrNull()
}
