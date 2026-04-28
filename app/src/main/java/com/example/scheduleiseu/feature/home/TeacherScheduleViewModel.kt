package com.example.scheduleiseu.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scheduleiseu.domain.core.model.ScheduleContext
import com.example.scheduleiseu.domain.core.model.ScheduleDay
import com.example.scheduleiseu.domain.core.model.ScheduleWeek
import com.example.scheduleiseu.domain.core.model.WeekInfo
import com.example.scheduleiseu.domain.core.network.NetworkMonitor
import com.example.scheduleiseu.domain.core.usecase.LoadTeacherScheduleContextUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveOwnTeacherCachedWeeksUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveOwnTeacherCachedWeekValuesUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveOwnTeacherScheduleForWeekUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveOwnTeacherScheduleUseCase
import com.example.scheduleiseu.domain.core.usecase.RefreshOwnTeacherScheduleForWeekUseCase
import com.example.scheduleiseu.domain.core.usecase.RefreshOwnTeacherScheduleUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeacherScheduleViewModel(
    private val observeOwnTeacherCachedWeeks: ObserveOwnTeacherCachedWeeksUseCase,
    private val observeOwnTeacherCachedWeekValues: ObserveOwnTeacherCachedWeekValuesUseCase,
    private val observeOwnTeacherSchedule: ObserveOwnTeacherScheduleUseCase,
    private val observeOwnTeacherScheduleForWeek: ObserveOwnTeacherScheduleForWeekUseCase,
    private val refreshOwnTeacherSchedule: RefreshOwnTeacherScheduleUseCase,
    private val refreshOwnTeacherScheduleForWeek: RefreshOwnTeacherScheduleForWeekUseCase,
    private val loadTeacherScheduleContext: LoadTeacherScheduleContextUseCase,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    private var cachedTeacherWeekValues: Set<String> = emptySet()
    private var selectedWeekObserverJob: Job? = null
    private var teacherScheduleContext: ScheduleContext? = null
    private val manuallySelectedDayByWeekValue = mutableMapOf<String, String>()

    init {
        observeNetwork()
        observeCachedWeeks()
        observeCachedWeekValues()
        observeCachedSchedule()
        loadInitialSchedule()
    }

    fun loadInitialSchedule() {
        loadInitialSchedule(ScheduleLoadingStage.Initial)
    }

    fun onWeekSelected(weekTitle: String) {
        val normalizedTitle = weekTitle.removeCachedWeekMarker()
        val week = _state.value.availableWeeks.firstOrNull { it.title == normalizedTitle } ?: return
        resetManualDaySelection(week)
        loadWeek(week)
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

    private fun loadInitialSchedule(stage: ScheduleLoadingStage) {
        selectedWeekObserverJob?.cancel()
        selectedWeekObserverJob = null
        viewModelScope.launch {
            val hasCachedSchedule = _state.value.days.isNotEmpty()
            if (!networkMonitor.isCurrentlyOnline()) {
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        loadingStage = null,
                        isOfflineMode = true,
                        offlineMessage = OFFLINE_MESSAGE,
                        errorMessage = if (hasCachedSchedule) null else "Нет интернета и сохраненного расписания преподавателя"
                    )
                }
                return@launch
            }

            _state.update {
                it.copy(
                    isLoading = true,
                    loadingStage = if (hasCachedSchedule && stage == ScheduleLoadingStage.Initial) {
                        ScheduleLoadingStage.BackgroundRefresh
                    } else {
                        stage
                    },
                    isOfflineMode = false,
                    offlineMessage = null,
                    errorMessage = null
                )
            }

            runCatching {
                val context = loadTeacherScheduleContext()
                val week = refreshOwnTeacherSchedule()
                context to week
            }.onSuccess { (context, week) ->
                teacherScheduleContext = context
                applyLoadedWeek(week = week, context = context)
            }.onFailure { throwable ->
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        loadingStage = null,
                        errorMessage = if (current.days.isEmpty()) {
                            throwable.message ?: "Не удалось загрузить расписание преподавателя"
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
                    loadInitialSchedule(ScheduleLoadingStage.RetryAfterNetworkRestore)
                }
            }
        }
    }

    private fun observeCachedSchedule() {
        viewModelScope.launch {
            runCatching {
                observeOwnTeacherSchedule().collect { week ->
                    if (week != null && selectedWeekObserverJob == null) {
                        applyLoadedWeek(week = week, context = teacherScheduleContext)
                    }
                }
            }.onFailure { throwable ->
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        loadingStage = null,
                        errorMessage = if (current.days.isEmpty()) {
                            throwable.message ?: "Не удалось прочитать кэш расписания преподавателя"
                        } else {
                            current.errorMessage
                        }
                    )
                }
            }
        }
    }

    private fun observeCachedWeekValues() {
        viewModelScope.launch {
            runCatching {
                observeOwnTeacherCachedWeekValues().collect { cachedValues ->
                    cachedTeacherWeekValues = cachedValues
                    _state.update { current ->
                        current.copy(
                            currentWeek = current.currentWeek?.withCachedFlag(),
                            selectedWeek = current.selectedWeek?.withCachedFlag(),
                            scheduleContext = current.scheduleContext?.markCachedWeeks(),
                            availableWeeks = markCachedWeeks(current.availableWeeks)
                        )
                    }
                }
            }
        }
    }

    private fun observeCachedWeeks() {
        viewModelScope.launch {
            runCatching {
                observeOwnTeacherCachedWeeks().collect { cachedWeeks ->
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

    private fun loadWeek(week: WeekInfo) {
        selectedWeekObserverJob?.cancel()
        selectedWeekObserverJob = viewModelScope.launch {
            runCatching {
                observeOwnTeacherScheduleForWeek(week).collect { cachedWeek ->
                    if (cachedWeek != null) {
                        applyLoadedWeek(
                            week = cachedWeek.copy(week = week.withCachedFlag()),
                            context = teacherScheduleContext,
                            selectedWeekOverride = week
                        )
                    }
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
                        errorMessage = if (week.value in cachedTeacherWeekValues || current.days.isNotEmpty()) {
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
                _state.update {
                    it.copy(
                        isLoading = true,
                        loadingStage = ScheduleLoadingStage.Selection,
                        isOfflineMode = false,
                        offlineMessage = null,
                        errorMessage = null
                    )
                }
            }

            runCatching { refreshOwnTeacherScheduleForWeek(week) }
                .onSuccess {
                    applyLoadedWeek(
                        week = it.copy(week = week.withCachedFlag()),
                        context = teacherScheduleContext,
                        selectedWeekOverride = week
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

    private fun applyLoadedWeek(
        week: ScheduleWeek,
        context: ScheduleContext?,
        selectedWeekOverride: WeekInfo = week.week
    ) {
        val selectedDay = resolveSelectedDayForApply(
            days = week.days,
            selectedWeek = selectedWeekOverride,
            defaultSelectedDay = resolveInitialSelectedDay(week)
        )
        val markedContext = context?.markCachedWeeks()
        val contextWeeks = markedContext?.weeks.orEmpty()
        val availableWeeksSource = contextWeeks.ifEmpty {
            _state.value.availableWeeks.ifEmpty { listOf(selectedWeekOverride) }
        }
        val markedWeeks = markCachedWeeks(availableWeeksSource).let { weeks ->
            if (weeks.none { it.value == selectedWeekOverride.value }) {
                markCachedWeeks(weeks + selectedWeekOverride)
            } else {
                weeks
            }
        }
        val isOnline = networkMonitor.isCurrentlyOnline()

        _state.value = _state.value.copy(
            currentWeek = (markedContext?.currentWeek ?: markedWeeks.firstOrNull { it.isCurrent } ?: week.week).withCachedFlag(),
            selectedWeek = selectedWeekOverride.withCachedFlag(),
            days = week.days,
            selectedDay = selectedDay,
            lessonsForSelectedDay = selectedDay?.lessons.orEmpty(),
            scheduleContext = markedContext,
            availableWeeks = markedWeeks,
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
        return copy(isCached = isCached || value in cachedTeacherWeekValues)
    }

    private fun mergeWithCachedWeek(week: WeekInfo, cachedWeeks: List<WeekInfo>): WeekInfo {
        val cached = cachedWeeks.firstOrNull { it.value == week.value } ?: return week
        return week.copy(
            title = if (week.title.isBlank()) cached.title else week.title,
            isCurrent = week.isCurrent || cached.isCurrent,
            isCached = true
        )
    }

    private fun String.removeCachedWeekMarker(): String {
        return removeSuffix(" +").trim()
    }
}
