package com.example.scheduleiseu.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scheduleiseu.data.local.preferences.AppPreferencesDataSource
import com.example.scheduleiseu.domain.core.model.ScheduleContext
import com.example.scheduleiseu.domain.core.model.ScheduleDay
import com.example.scheduleiseu.domain.core.model.ScheduleWeek
import com.example.scheduleiseu.domain.core.model.UserRole
import com.example.scheduleiseu.domain.core.model.WeekInfo
import com.example.scheduleiseu.domain.core.network.NetworkMonitor
import com.example.scheduleiseu.domain.core.usecase.LoadExternalStudentGroupScheduleUseCase
import com.example.scheduleiseu.domain.core.usecase.LoadExternalTeacherScheduleUseCase
import com.example.scheduleiseu.domain.core.usecase.LoadStudentScheduleContextUseCase
import com.example.scheduleiseu.domain.core.usecase.LoadTeacherScheduleContextUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveOwnStudentCachedWeeksUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveOwnStudentCachedWeekValuesUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveOwnStudentScheduleForWeekUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveOwnStudentScheduleUseCase
import com.example.scheduleiseu.domain.core.usecase.RefreshOwnStudentScheduleForWeekUseCase
import com.example.scheduleiseu.domain.core.usecase.RefreshOwnStudentScheduleUseCase
import com.example.scheduleiseu.domain.core.usecase.ScheduleLessonVisibilityFilter
import com.example.scheduleiseu.domain.model.TeacherSearchItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScheduleViewModel(
    private val ownGroupTitle: String?,
    private val registeredSubgroup: String?,
    private val preferencesDataSource: AppPreferencesDataSource,
    private val observeOwnStudentCachedWeeks: ObserveOwnStudentCachedWeeksUseCase,
    private val observeOwnStudentCachedWeekValues: ObserveOwnStudentCachedWeekValuesUseCase,
    private val observeOwnStudentSchedule: ObserveOwnStudentScheduleUseCase,
    private val observeOwnStudentScheduleForWeek: ObserveOwnStudentScheduleForWeekUseCase,
    private val refreshOwnStudentSchedule: RefreshOwnStudentScheduleUseCase,
    private val refreshOwnStudentScheduleForWeek: RefreshOwnStudentScheduleForWeekUseCase,
    private val loadStudentScheduleContext: LoadStudentScheduleContextUseCase,
    private val loadTeacherScheduleContext: LoadTeacherScheduleContextUseCase,
    private val loadExternalStudentGroupSchedule: LoadExternalStudentGroupScheduleUseCase,
    private val loadExternalTeacherSchedule: LoadExternalTeacherScheduleUseCase,
    private val networkMonitor: NetworkMonitor,
    private val visibilityFilter: ScheduleLessonVisibilityFilter = ScheduleLessonVisibilityFilter()
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    private var ownScheduleContext: ScheduleContext? = null
    private var teacherScheduleContext: ScheduleContext? = null

    private var ownCurrentWeek: WeekInfo? = null
    private var ownCurrentDayDate: String? = null
    private var cachedOwnWeekValues: Set<String> = emptySet()
    private var selectedOwnWeekObserverJob: Job? = null
    private val manuallySelectedDayByWeekValue = mutableMapOf<String, String>()
    private var showMismatchedSubgroupLessons = true
    private var cacheWeeksEnabled = false
    private var settingsInitialized = false
    private var lastWeekSource: ScheduleWeek? = null
    private var lastContextSource: ScheduleContext? = null
    private var lastSelectedWeekOverride: WeekInfo? = null
    private var lastSelectedDayOverride: ScheduleDay? = null
    private var lastIsTemporaryContext = false
    private var lastSelectedGroupTitle: String? = null
    private var lastSelectedTeacherName: String? = null

    init {
        observeNetwork()
        observeSettings()
        observeCachedOwnSchedule()
        observeCachedOwnWeeks()
        observeCachedOwnWeekValues()
        loadInitialSchedule()
    }

    fun loadInitialSchedule() {
        loadInitialSchedule(ScheduleLoadingStage.Initial)
    }

    fun onWeekSelected(weekTitle: String) {
        val normalizedTitle = weekTitle.removeCachedWeekMarker()
        val week = _state.value.availableWeeks.firstOrNull { it.title == normalizedTitle } ?: return
        resetManualDaySelection(week)
        if (_state.value.isTemporaryContext) {
            loadTemporaryWeek(week)
        } else {
            loadOwnWeekAsTemporary(week)
        }
    }

    fun onDayClick(dayDate: String) {
        val day = _state.value.days.firstOrNull { it.date == dayDate } ?: return
        _state.value.selectedWeek?.value?.let { weekValue -> manuallySelectedDayByWeekValue[weekValue] = day.date }
        _state.update { current ->
            current.copy(
                selectedDay = day,
                lessonsForSelectedDay = day.lessons,
                errorMessage = null
            )
        }
    }

    fun onTeacherQueryChanged(query: String) {
        _state.update { current ->
            current.copy(
                teacherQuery = query,
                filteredTeachers = filterTeachers(query, current.availableTeachers)
            )
        }
    }

    fun onTeacherSearchCleared() {
        _state.update { current ->
            current.copy(
                teacherQuery = "",
                filteredTeachers = filterTeachers("", current.availableTeachers)
            )
        }
    }

    fun onExternalTeacherSelected(item: TeacherSearchItem) {
        if (!networkMonitor.isCurrentlyOnline()) {
            setOfflineSelectionState("Расписание преподавателя недоступно без интернета")
            return
        }
        val week = _state.value.selectedWeek ?: _state.value.currentWeek ?: return
        viewModelScope.launch {
            setLoading(true, ScheduleLoadingStage.Selection)
            runCatching { loadExternalTeacherSchedule(item.id, week) }
                .onSuccess { weekData ->
                    applyLoadedWeek(
                        week = weekData,
                        context = teacherScheduleContext,
                        selectedWeekOverride = week,
                        selectedDayOverride = resolveInitialSelectedDay(weekData),
                        isTemporaryContext = true,
                        selectedGroupTitle = null,
                        selectedTeacherName = item.fullName
                    )
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            loadingStage = null,
                            errorMessage = throwable.message ?: "Не удалось загрузить расписание преподавателя"
                        )
                    }
                }
        }
    }

    fun onExternalGroupSelected(groupTitle: String) {
        if (!networkMonitor.isCurrentlyOnline()) {
            setOfflineSelectionState("Расписание другой группы недоступно без интернета")
            return
        }
        val context = ownScheduleContext ?: return
        val normalizedTitle = groupTitle.removeCachedWeekMarker()
        val groupId = context.groups.firstOrNull { it.title == normalizedTitle }?.id ?: return
        val week = _state.value.selectedWeek ?: _state.value.currentWeek ?: return

        viewModelScope.launch {
            setLoading(true, ScheduleLoadingStage.Selection)
            runCatching { loadExternalStudentGroupSchedule(groupId, week) }
                .onSuccess { weekData ->
                    applyLoadedWeek(
                        week = weekData,
                        context = ownScheduleContext,
                        selectedWeekOverride = week,
                        selectedDayOverride = resolveInitialSelectedDay(weekData),
                        isTemporaryContext = true,
                        selectedGroupTitle = normalizedTitle,
                        selectedTeacherName = null
                    )
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            loadingStage = null,
                            errorMessage = throwable.message ?: "Не удалось загрузить расписание группы"
                        )
                    }
                }
        }
    }

    fun resetTemporaryContext() {
        loadInitialSchedule()
    }

    private fun loadInitialSchedule(stage: ScheduleLoadingStage) {
        selectedOwnWeekObserverJob?.cancel()
        viewModelScope.launch {
            val hasCachedSchedule = _state.value.days.isNotEmpty()
            if (!networkMonitor.isCurrentlyOnline()) {
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        loadingStage = null,
                        isOfflineMode = true,
                        offlineMessage = OFFLINE_MESSAGE,
                        errorMessage = if (hasCachedSchedule) null else "Нет интернета и сохраненного расписания"
                    )
                }
                return@launch
            }

            if (hasCachedSchedule) {
                _state.update {
                    it.copy(
                        isLoading = true,
                        loadingStage = if (stage == ScheduleLoadingStage.RetryAfterNetworkRestore) stage else ScheduleLoadingStage.BackgroundRefresh,
                        isOfflineMode = false,
                        offlineMessage = null,
                        errorMessage = null
                    )
                }
            } else {
                setLoading(true, stage)
            }

            runCatching {
                val ownContext = loadStudentScheduleContext()
                val ownWeek = refreshOwnStudentSchedule()
                ownContext to ownWeek
            }.onSuccess { (context, week) ->
                ownScheduleContext = context
                ownCurrentWeek = week.week
                ownCurrentDayDate = resolveInitialSelectedDay(week)?.date
                applyLoadedWeek(
                    week = week,
                    context = context,
                    selectedWeekOverride = week.week,
                    selectedDayOverride = resolveInitialSelectedDay(week),
                    isTemporaryContext = false,
                    selectedGroupTitle = null,
                    selectedTeacherName = null
                )
                preloadTeacherOptions()
            }.onFailure { throwable ->
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        loadingStage = null,
                        errorMessage = if (current.days.isEmpty()) {
                            throwable.message ?: "Не удалось загрузить расписание"
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                if (!isOnline) {
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            loadingStage = null,
                            isOfflineMode = true,
                            offlineMessage = OFFLINE_MESSAGE,
                            errorMessage = if (current.days.isEmpty()) current.errorMessage else null
                        )
                    }
                } else if (_state.value.isOfflineMode) {
                    _state.update { current ->
                        current.copy(
                            isLoading = current.days.isNotEmpty(),
                            loadingStage = ScheduleLoadingStage.RetryAfterNetworkRestore,
                            isOfflineMode = false,
                            offlineMessage = null,
                            errorMessage = null
                        )
                    }
                    loadInitialSchedule(ScheduleLoadingStage.RetryAfterNetworkRestore)
                }
            }
        }
    }

    private fun observeCachedOwnSchedule() {
        viewModelScope.launch {
            runCatching {
                observeOwnStudentSchedule().collect { cachedWeek ->
                    if (cachedWeek != null && !_state.value.isTemporaryContext && selectedOwnWeekObserverJob == null) {
                        ownCurrentWeek = ownCurrentWeek ?: cachedWeek.week
                        val selectedDay = resolveInitialSelectedDay(cachedWeek)
                        ownCurrentDayDate = ownCurrentDayDate ?: selectedDay?.date
                        applyLoadedWeek(
                            week = cachedWeek,
                            context = ownScheduleContext,
                            selectedWeekOverride = cachedWeek.week,
                            selectedDayOverride = selectedDay,
                            isTemporaryContext = false,
                            selectedGroupTitle = null,
                            selectedTeacherName = null
                        )
                    }
                }
            }.onFailure { throwable ->
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        loadingStage = null,
                        errorMessage = if (current.days.isEmpty()) {
                            throwable.message ?: "Не удалось прочитать кэш расписания"
                        } else {
                            current.errorMessage
                        }
                    )
                }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                preferencesDataSource.observeCacheCurrentAndPreviousWeekEnabled(),
                preferencesDataSource.observeShowMismatchedSubgroupLessonsEnabled()
            ) { cacheEnabled, showMismatched ->
                cacheEnabled to showMismatched
            }.collect { (cacheEnabled, showMismatched) ->
                val cacheChanged = settingsInitialized && cacheWeeksEnabled != cacheEnabled
                val subgroupChanged = settingsInitialized && showMismatchedSubgroupLessons != showMismatched
                cacheWeeksEnabled = cacheEnabled
                showMismatchedSubgroupLessons = showMismatched
                if (!settingsInitialized) {
                    settingsInitialized = true
                    return@collect
                }
                if (subgroupChanged || cacheChanged) {
                    refreshCurrentScheduleAfterSettingsChanged(subgroupChanged = subgroupChanged)
                }
            }
        }
    }

    private fun observeCachedOwnWeekValues() {
        viewModelScope.launch {
            runCatching {
                observeOwnStudentCachedWeekValues().collect { cachedValues ->
                    cachedOwnWeekValues = cachedValues
                    _state.update { current ->
                        current.copy(
                            currentWeek = current.currentWeek?.withCachedFlag(),
                            selectedWeek = current.selectedWeek?.withCachedFlag(),
                            availableWeeks = markCachedWeeks(current.availableWeeks)
                        )
                    }
                }
            }
        }
    }

    private fun observeCachedOwnWeeks() {
        viewModelScope.launch {
            runCatching {
                observeOwnStudentCachedWeeks().collect { cachedWeeks ->
                    _state.update { current ->
                        val availableWeeks = when {
                            current.scheduleContext != null -> markCachedWeeks(current.scheduleContext.weeks)
                            cachedWeeks.isNotEmpty() -> cachedWeeks
                            else -> emptyList()
                        }
                        current.copy(
                            currentWeek = current.currentWeek?.let { mergeWithCachedWeek(it, cachedWeeks) },
                            selectedWeek = current.selectedWeek?.let { mergeWithCachedWeek(it, cachedWeeks) },
                            availableWeeks = availableWeeks
                        )
                    }
                }
            }
        }
    }

    private fun loadOwnWeekAsTemporary(week: WeekInfo) {
        selectedOwnWeekObserverJob?.cancel()
        selectedOwnWeekObserverJob = viewModelScope.launch {
            runCatching {
                observeOwnStudentScheduleForWeek(week).collect { cachedWeek ->
                    if (cachedWeek != null) {
                        applyLoadedWeek(
                            week = cachedWeek,
                            context = ownScheduleContext,
                            selectedWeekOverride = week,
                            selectedDayOverride = resolveInitialSelectedDay(cachedWeek),
                            isTemporaryContext = true,
                            selectedGroupTitle = null,
                            selectedTeacherName = null
                        )
                    }
                }
            }.onFailure { throwable ->
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        loadingStage = null,
                        errorMessage = if (current.days.isEmpty()) {
                            throwable.message ?: "Не удалось прочитать выбранную неделю из кэша"
                        } else {
                            current.errorMessage
                        }
                    )
                }
            }
        }

        viewModelScope.launch {
            val isCachedWeekAlreadyVisible = _state.value.selectedWeek?.value == week.value &&
                _state.value.days.isNotEmpty()
            if (!networkMonitor.isCurrentlyOnline()) {
                _state.update { current ->
                    current.copy(
                        selectedWeek = week.withCachedFlag(),
                        isLoading = false,
                        loadingStage = null,
                        isOfflineMode = true,
                        offlineMessage = OFFLINE_MESSAGE,
                        errorMessage = if (week.value in cachedOwnWeekValues || current.days.isNotEmpty()) {
                            null
                        } else {
                            "Эта неделя не сохранена для офлайн-режима"
                        }
                    )
                }
                return@launch
            }

            if (isCachedWeekAlreadyVisible) {
                _state.update { it.copy(errorMessage = null) }
            } else {
                setLoading(true, ScheduleLoadingStage.Selection)
            }

            runCatching { refreshOwnStudentScheduleForWeek(week) }
                .onSuccess { scheduleWeek ->
                    applyLoadedWeek(
                        week = scheduleWeek,
                        context = ownScheduleContext,
                        selectedWeekOverride = week,
                        selectedDayOverride = resolveInitialSelectedDay(scheduleWeek),
                        isTemporaryContext = true,
                        selectedGroupTitle = null,
                        selectedTeacherName = null
                    )
                }
                .onFailure { throwable ->
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            loadingStage = null,
                            errorMessage = if (current.days.isEmpty()) {
                                throwable.message ?: "Не удалось загрузить выбранную неделю"
                            } else {
                                null
                            }
                        )
                    }
                }
        }
    }

    private fun loadTemporaryWeek(week: WeekInfo) {
        val selectedTeacher = _state.value.selectedTeacherName
        val selectedGroup = _state.value.selectedGroupTitle
        when {
            selectedTeacher != null -> {
                val teacher = _state.value.availableTeachers.firstOrNull { it.fullName == selectedTeacher } ?: return
                onExternalTeacherSelectedForWeek(teacher, week)
            }
            selectedGroup != null -> onExternalGroupSelectedForWeek(selectedGroup, week)
            else -> loadOwnWeekAsTemporary(week)
        }
    }

    private fun onExternalTeacherSelectedForWeek(item: TeacherSearchItem, week: WeekInfo) {
        if (!networkMonitor.isCurrentlyOnline()) {
            setOfflineSelectionState("Выбранная неделя преподавателя недоступна без интернета")
            return
        }
        viewModelScope.launch {
            setLoading(true, ScheduleLoadingStage.Selection)
            runCatching { loadExternalTeacherSchedule(item.id, week) }
                .onSuccess { weekData ->
                    applyLoadedWeek(
                        week = weekData,
                        context = teacherScheduleContext,
                        selectedWeekOverride = week,
                        selectedDayOverride = resolveInitialSelectedDay(weekData),
                        isTemporaryContext = true,
                        selectedGroupTitle = null,
                        selectedTeacherName = item.fullName
                    )
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            loadingStage = null,
                            errorMessage = throwable.message ?: "Не удалось загрузить расписание преподавателя"
                        )
                    }
                }
        }
    }

    private fun onExternalGroupSelectedForWeek(groupTitle: String, week: WeekInfo) {
        if (!networkMonitor.isCurrentlyOnline()) {
            setOfflineSelectionState("Выбранная неделя группы недоступна без интернета")
            return
        }
        val context = ownScheduleContext ?: return
        val groupId = context.groups.firstOrNull { it.title == groupTitle }?.id ?: return

        viewModelScope.launch {
            setLoading(true, ScheduleLoadingStage.Selection)
            runCatching { loadExternalStudentGroupSchedule(groupId, week) }
                .onSuccess { weekData ->
                    applyLoadedWeek(
                        week = weekData,
                        context = ownScheduleContext,
                        selectedWeekOverride = week,
                        selectedDayOverride = resolveInitialSelectedDay(weekData),
                        isTemporaryContext = true,
                        selectedGroupTitle = groupTitle,
                        selectedTeacherName = null
                    )
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            loadingStage = null,
                            errorMessage = throwable.message ?: "Не удалось загрузить расписание группы"
                        )
                    }
                }
        }
    }

    private fun preloadTeacherOptions() {
        if (!networkMonitor.isCurrentlyOnline()) return
        viewModelScope.launch {
            runCatching { loadTeacherScheduleContext() }
                .onSuccess { context ->
                    teacherScheduleContext = context
                    val teachers = context.teachers
                        .filter { it.id.isNotBlank() && !it.title.isTeacherPlaceholder() }
                        .map { TeacherSearchItem(id = it.id, fullName = it.title) }
                    _state.update {
                        it.copy(
                            availableTeachers = teachers,
                            filteredTeachers = filterTeachers(it.teacherQuery, teachers)
                        )
                    }
                }
        }
    }

    private fun filterTeachers(query: String, teachers: List<TeacherSearchItem>): List<TeacherSearchItem> {
        val normalized = query.trim()
        if (normalized.isBlank()) return teachers
        return teachers.filter { teacher ->
            teacher.fullName.contains(normalized, ignoreCase = true) ||
                teacher.subtitle?.contains(normalized, ignoreCase = true) == true
        }
    }

    private fun setLoading(isLoading: Boolean, stage: ScheduleLoadingStage) {
        _state.update {
            it.copy(
                isLoading = isLoading,
                loadingStage = if (isLoading) stage else null,
                isOfflineMode = if (networkMonitor.isCurrentlyOnline()) false else it.isOfflineMode,
                offlineMessage = if (networkMonitor.isCurrentlyOnline()) null else it.offlineMessage,
                errorMessage = null
            )
        }
    }

    private fun setOfflineSelectionState(message: String) {
        _state.update { current ->
            current.copy(
                isLoading = false,
                loadingStage = null,
                isOfflineMode = true,
                offlineMessage = OFFLINE_MESSAGE,
                errorMessage = if (current.days.isEmpty()) message else null
            )
        }
    }

    private fun applyLoadedWeek(
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

        val filteredWeek = applyLessonFilters(week, context, selectedTeacherName)
        val markedContext = context?.markCachedWeeks()
        val selectedWeek = selectedWeekOverride.withCachedFlag()
        val selectedDay = resolveSelectedDayForApply(
            days = filteredWeek.days,
            selectedWeek = selectedWeekOverride,
            defaultSelectedDay = selectedDayOverride
        )
        val isOnline = networkMonitor.isCurrentlyOnline()
        _state.value = _state.value.copy(
            currentWeek = (ownCurrentWeek ?: week.week).withCachedFlag(),
            selectedWeek = selectedWeek,
            days = filteredWeek.days,
            selectedDay = selectedDay,
            lessonsForSelectedDay = selectedDay?.lessons.orEmpty(),
            scheduleContext = markedContext,
            availableWeeks = markedContext?.weeks ?: markCachedWeeks(_state.value.availableWeeks.ifEmpty { listOf(filteredWeek.week) }),
            availableGroups = context?.let(::buildExternalGroupTitles) ?: _state.value.availableGroups,
            selectedGroupTitle = selectedGroupTitle,
            selectedTeacherName = selectedTeacherName,
            isTemporaryContext = isTemporaryContext,
            isLoading = false,
            loadingStage = null,
            isOfflineMode = !isOnline,
            offlineMessage = if (isOnline) null else OFFLINE_MESSAGE,
            errorMessage = null
        )
    }

    private fun resetManualDaySelection(week: WeekInfo) {
        manuallySelectedDayByWeekValue.remove(week.value)
    }

    private fun resolveSelectedDayForApply(
        days: List<ScheduleDay>,
        selectedWeek: WeekInfo,
        defaultSelectedDay: ScheduleDay?
    ): ScheduleDay? {
        val manuallySelectedDate = manuallySelectedDayByWeekValue[selectedWeek.value]
        return manuallySelectedDate?.let { date -> days.firstOrNull { it.date == date } }
            ?: defaultSelectedDay
    }

    private fun resolveInitialSelectedDay(week: ScheduleWeek): ScheduleDay? {
        return week.currentDay
            ?: if (week.week.isCurrent) ownCurrentDayDate?.let { expected -> week.days.firstOrNull { it.date == expected } } else null
            ?: week.selectedDay
            ?: week.days.firstOrNull { it.lessons.isNotEmpty() }
            ?: week.days.firstOrNull()
    }

    private fun ScheduleContext.markCachedWeeks(): ScheduleContext {
        return copy(
            currentWeek = currentWeek?.withCachedFlag(),
            selectedWeek = selectedWeek?.withCachedFlag(),
            weeks = markCachedWeeks(weeks)
        )
    }

    private fun markCachedWeeks(weeks: List<WeekInfo>): List<WeekInfo> {
        return weeks.map { it.withCachedFlag() }
    }

    private fun WeekInfo.withCachedFlag(): WeekInfo {
        return copy(isCached = isCached || value in cachedOwnWeekValues)
    }

    private fun applyLessonFilters(
        week: ScheduleWeek,
        context: ScheduleContext?,
        selectedTeacherName: String?
    ): ScheduleWeek {
        val selectedGroupTitle = _state.value.selectedGroupTitle
        if (
            context?.userRole == UserRole.TEACHER ||
            !selectedTeacherName.isNullOrBlank() ||
            !selectedGroupTitle.isNullOrBlank()
        ) {
            return week
        }

        return visibilityFilter.filterForStudentSubgroup(
            week = week,
            registeredSubgroup = registeredSubgroup,
            showMismatchedSubgroupLessons = showMismatchedSubgroupLessons
        )
    }

    private fun mergeWithCachedWeek(week: WeekInfo, cachedWeeks: List<WeekInfo>): WeekInfo {
        val cached = cachedWeeks.firstOrNull { it.value == week.value } ?: return week
        return week.copy(
            title = if (week.title.isBlank()) cached.title else week.title,
            isCurrent = week.isCurrent || cached.isCurrent,
            isCached = true
        )
    }

    private fun buildExternalGroupTitles(context: ScheduleContext): List<String> {
        return context.groups
            .filterNot { option ->
                option.id == context.selectedGroupId ||
                    option.title == ownGroupTitle
            }
            .map { it.title }
    }

    private fun reapplyLastWeek() {
        val week = lastWeekSource ?: return
        applyLoadedWeek(
            week = week,
            context = lastContextSource,
            selectedWeekOverride = lastSelectedWeekOverride ?: week.week,
            selectedDayOverride = lastSelectedDayOverride,
            isTemporaryContext = lastIsTemporaryContext,
            selectedGroupTitle = lastSelectedGroupTitle,
            selectedTeacherName = lastSelectedTeacherName
        )
    }

    private fun refreshCurrentScheduleAfterSettingsChanged(subgroupChanged: Boolean) {
        if (!networkMonitor.isCurrentlyOnline()) {
            if (subgroupChanged) {
                reapplyLastWeek()
            }
            return
        }

        val selectedWeek = _state.value.selectedWeek ?: _state.value.currentWeek
        when {
            selectedWeek == null -> loadInitialSchedule(ScheduleLoadingStage.BackgroundRefresh)
            !_state.value.selectedTeacherName.isNullOrBlank() || !_state.value.selectedGroupTitle.isNullOrBlank() -> {
                refreshTemporarySelectedWeekAfterSettingsChanged(selectedWeek)
            }
            else -> refreshOwnSelectedWeekAfterSettingsChanged(selectedWeek)
        }
    }

    private fun refreshTemporarySelectedWeekAfterSettingsChanged(week: WeekInfo) {
        val selectedTeacher = _state.value.selectedTeacherName
        val selectedGroup = _state.value.selectedGroupTitle
        when {
            !selectedTeacher.isNullOrBlank() -> {
                val teacher = _state.value.availableTeachers.firstOrNull { it.fullName == selectedTeacher } ?: return
                viewModelScope.launch {
                    setLoading(true, ScheduleLoadingStage.BackgroundRefresh)
                    runCatching { loadExternalTeacherSchedule(teacher.id, week) }
                        .onSuccess { weekData ->
                            applyLoadedWeek(
                                week = weekData,
                                context = teacherScheduleContext,
                                selectedWeekOverride = week,
                                selectedDayOverride = resolveSelectedDayAfterRefresh(weekData),
                                isTemporaryContext = true,
                                selectedGroupTitle = null,
                                selectedTeacherName = teacher.fullName
                            )
                        }
                        .onFailure { throwable ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    loadingStage = null,
                                    errorMessage = throwable.message ?: "Не удалось обновить расписание преподавателя"
                                )
                            }
                        }
                }
            }
            !selectedGroup.isNullOrBlank() -> {
                val context = ownScheduleContext ?: return
                val groupId = context.groups.firstOrNull { it.title == selectedGroup }?.id ?: return
                viewModelScope.launch {
                    setLoading(true, ScheduleLoadingStage.BackgroundRefresh)
                    runCatching { loadExternalStudentGroupSchedule(groupId, week) }
                        .onSuccess { weekData ->
                            applyLoadedWeek(
                                week = weekData,
                                context = ownScheduleContext,
                                selectedWeekOverride = week,
                                selectedDayOverride = resolveSelectedDayAfterRefresh(weekData),
                                isTemporaryContext = true,
                                selectedGroupTitle = selectedGroup,
                                selectedTeacherName = null
                            )
                        }
                        .onFailure { throwable ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    loadingStage = null,
                                    errorMessage = throwable.message ?: "Не удалось обновить расписание группы"
                                )
                            }
                        }
                }
            }
            else -> refreshOwnSelectedWeekAfterSettingsChanged(week)
        }
    }

    private fun refreshOwnSelectedWeekAfterSettingsChanged(week: WeekInfo) {
        selectedOwnWeekObserverJob?.cancel()
        selectedOwnWeekObserverJob = viewModelScope.launch {
            runCatching {
                observeOwnStudentScheduleForWeek(week).collect { cachedWeek ->
                    if (cachedWeek != null && _state.value.selectedGroupTitle == null && _state.value.selectedTeacherName == null) {
                        applyLoadedWeek(
                            week = cachedWeek,
                            context = ownScheduleContext,
                            selectedWeekOverride = week,
                            selectedDayOverride = resolveSelectedDayAfterRefresh(cachedWeek),
                            isTemporaryContext = false,
                            selectedGroupTitle = null,
                            selectedTeacherName = null
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            setLoading(true, ScheduleLoadingStage.BackgroundRefresh)
            runCatching {
                val context = loadStudentScheduleContext()
                val weekData = refreshOwnStudentScheduleForWeek(week)
                context to weekData
            }.onSuccess { (context, weekData) ->
                ownScheduleContext = context
                applyLoadedWeek(
                    week = weekData,
                    context = context,
                    selectedWeekOverride = week,
                    selectedDayOverride = resolveSelectedDayAfterRefresh(weekData),
                    isTemporaryContext = false,
                    selectedGroupTitle = null,
                    selectedTeacherName = null
                )
            }.onFailure { throwable ->
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        loadingStage = null,
                        errorMessage = if (current.days.isEmpty()) {
                            throwable.message ?: "Не удалось обновить выбранную неделю"
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    private fun resolveSelectedDayAfterRefresh(week: ScheduleWeek): ScheduleDay? {
        val currentSelectedDate = _state.value.selectedDay?.date
        return currentSelectedDate?.let { date -> week.days.firstOrNull { it.date == date } }
            ?: resolveInitialSelectedDay(week)
    }

    private fun String.removeCachedWeekMarker(): String {
        return removeSuffix(" +").trim()
    }

    private fun String.isTeacherPlaceholder(): Boolean {
        val normalized = trim().lowercase()
        return normalized == "выберите фамилию преподавателя" || normalized.startsWith("выберите ")
    }
}
