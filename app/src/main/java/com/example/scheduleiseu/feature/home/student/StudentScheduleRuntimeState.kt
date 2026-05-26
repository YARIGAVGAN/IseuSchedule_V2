package com.example.scheduleiseu.feature.home

import com.example.scheduleiseu.domain.core.model.ScheduleContext
import com.example.scheduleiseu.domain.core.model.ScheduleDay
import com.example.scheduleiseu.domain.core.model.ScheduleWeek
import com.example.scheduleiseu.domain.core.model.WeekInfo

internal class StudentScheduleRuntimeState(
    registeredSubgroup: String?
) {
    var ownScheduleContext: ScheduleContext? = null
    var teacherScheduleContext: ScheduleContext? = null

    var ownCurrentWeek: WeekInfo? = null
    var ownCurrentDayDate: String? = null
    var registeredSubgroup: String? = registeredSubgroup
    var cachedOwnWeekValues: Set<String> = emptySet()
    val manuallySelectedDayByWeekValue = mutableMapOf<String, String>()
    var showMismatchedSubgroupLessons = true
    var cacheWeeksEnabled = false
    var settingsInitialized = false

    var lastWeekSource: ScheduleWeek? = null
    var lastContextSource: ScheduleContext? = null
    var lastSelectedWeekOverride: WeekInfo? = null
    var lastSelectedDayOverride: ScheduleDay? = null
    var lastIsTemporaryContext = false
    var lastSelectedGroupTitle: String? = null
    var lastSelectedTeacherName: String? = null

    var lastPrimaryWeekSource: ScheduleWeek? = null
    var lastPrimaryContextSource: ScheduleContext? = null
    var lastPrimarySelectedWeekOverride: WeekInfo? = null
    var lastPrimarySelectedDayOverride: ScheduleDay? = null

    fun rememberAppliedWeek(
        week: ScheduleWeek,
        context: ScheduleContext?,
        selectedWeekOverride: WeekInfo,
        selectedDayOverride: ScheduleDay?,
        isTemporaryContext: Boolean,
        selectedGroupTitle: String?,
        selectedTeacherName: String?
    ) {
        lastWeekSource = week
        lastContextSource = context
        lastSelectedWeekOverride = selectedWeekOverride
        lastSelectedDayOverride = selectedDayOverride
        lastIsTemporaryContext = isTemporaryContext
        lastSelectedGroupTitle = selectedGroupTitle
        lastSelectedTeacherName = selectedTeacherName

        if (!isTemporaryContext) {
            lastPrimaryWeekSource = week
            lastPrimaryContextSource = context
            lastPrimarySelectedWeekOverride = selectedWeekOverride
            lastPrimarySelectedDayOverride = selectedDayOverride
        }
    }
}
