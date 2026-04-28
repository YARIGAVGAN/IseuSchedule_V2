package com.example.scheduleiseu.feature.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scheduleiseu.domain.core.model.SemesterPerformance
import com.example.scheduleiseu.domain.core.model.SemesterReference
import com.example.scheduleiseu.domain.core.model.SubjectPerformance
import com.example.scheduleiseu.domain.core.usecase.ObserveAllPerformanceUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveLatestPerformanceUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveSemesterPerformanceUseCase
import com.example.scheduleiseu.domain.core.usecase.RefreshLatestPerformanceUseCase
import com.example.scheduleiseu.domain.core.usecase.RefreshSemesterPerformanceUseCase
import com.example.scheduleiseu.domain.model.Course
import com.example.scheduleiseu.domain.model.Session
import com.example.scheduleiseu.domain.model.Subject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PerformanceViewModel(
    private val observeLatestPerformance: ObserveLatestPerformanceUseCase,
    private val observeAllPerformance: ObserveAllPerformanceUseCase,
    private val observeSemesterPerformance: ObserveSemesterPerformanceUseCase,
    private val refreshLatestPerformance: RefreshLatestPerformanceUseCase,
    private val refreshSemesterPerformance: RefreshSemesterPerformanceUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(PerformanceUiState())
    val state: StateFlow<PerformanceUiState> = _state.asStateFlow()

    private var selectedSemesterObserverJob: Job? = null
    private var cachedPerformancesById: Map<String, SemesterPerformance> = emptyMap()
    private var latestSemesterId: String? = null

    init {
        observeAllCachedPerformance()
        observeLatestCachedPerformance()
        refreshLatestSemester()
    }

    fun retry() {
        val selectedSemester = _state.value.course
            ?.sessions
            ?.firstOrNull { it.id == _state.value.selectedSemesterId }

        if (selectedSemester == null) {
            refreshLatestSemester()
        } else {
            observeAndRefreshSemester(selectedSemester.id, selectedSemester.title)
        }
    }

    fun onSemesterSelected(semesterId: String, semesterTitle: String) {
        if (_state.value.loadingSemesterId == semesterId) return
        observeAndRefreshSemester(semesterId, semesterTitle)
    }

    private fun observeAllCachedPerformance() {
        viewModelScope.launch {
            observeAllPerformance().collect { performances ->
                if (performances.isEmpty()) return@collect

                cachedPerformancesById = performances.associateBy { it.stableSemesterId() }
                val selectedId = _state.value.selectedSemesterId
                    ?: performances.lastOrNull()?.stableSemesterId()
                val sourcePerformance = selectedId
                    ?.let { cachedPerformancesById[it] }
                    ?: performances.last()

                latestSemesterId = performances.lastOrNull()?.stableSemesterId()
                applyPerformance(sourcePerformance)
            }
        }
    }

    private fun observeLatestCachedPerformance() {
        viewModelScope.launch {
            observeLatestPerformance().collect { performance ->
                if (performance != null && _state.value.course == null) {
                    latestSemesterId = performance.stableSemesterId()
                    applyPerformance(performance)
                }
            }
        }
    }

    private fun refreshLatestSemester() {
        viewModelScope.launch {
            val hasCachedCourse = _state.value.course != null
            _state.update {
                it.copy(
                    isLoading = !hasCachedCourse,
                    errorMessage = null,
                    loadingSemesterId = null
                )
            }

            runCatching { refreshLatestPerformance() }
                .onSuccess { performance ->
                    latestSemesterId = performance.stableSemesterId()
                    applyPerformance(performance)
                }
                .onFailure { throwable ->
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            loadingSemesterId = null,
                            errorMessage = if (current.course == null) {
                                throwable.message ?: "Не удалось загрузить успеваемость"
                            } else {
                                null
                            }
                        )
                    }
                }
        }
    }

    private fun observeAndRefreshSemester(semesterId: String, semesterTitle: String) {
        selectedSemesterObserverJob?.cancel()
        selectedSemesterObserverJob = viewModelScope.launch {
            observeSemesterPerformance(semesterId, semesterTitle).collect { performance ->
                if (performance != null) {
                    applyPerformance(performance)
                }
            }
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    errorMessage = null,
                    loadingSemesterId = semesterId,
                    selectedSemesterId = semesterId
                )
            }

            runCatching { refreshSemesterPerformance(semesterId, semesterTitle) }
                .onSuccess { performance -> applyPerformance(performance) }
                .onFailure { throwable ->
                    _state.update { current ->
                        current.copy(
                            loadingSemesterId = null,
                            isLoading = false,
                            errorMessage = if (current.course == null) {
                                throwable.message ?: "Не удалось открыть выбранный семестр"
                            } else {
                                null
                            }
                        )
                    }
                }
        }
    }

    private fun applyPerformance(performance: SemesterPerformance) {
        val normalizedPerformance = performance.withCachedSemesters(cachedPerformancesById)
        val selectedId = normalizedPerformance.stableSemesterId()
        _state.value = _state.value.copy(
            course = normalizedPerformance.toCourse(_state.value.course),
            selectedSemesterId = selectedId,
            loadingSemesterId = null,
            isLoading = false,
            errorMessage = null
        )
    }

    private fun SemesterPerformance.toCourse(previousCourse: Course?): Course {
        val selectedId = stableSemesterId()
        val previousSessionsById = previousCourse?.sessions.orEmpty().associateBy { it.id }
        val semesterReferences = availableSemesters.ifEmpty {
            cachedPerformancesById.values
                .map { SemesterReference(it.stableSemesterId(), it.semesterTitle) }
                .ifEmpty { listOf(SemesterReference(selectedId, semesterTitle)) }
        }
        val resolvedLatestSemesterId = latestSemesterId ?: semesterReferences.lastOrNull()?.id ?: selectedId

        val semesterSessions = semesterReferences.map { semester ->
            val previous = previousSessionsById[semester.id]
            val cached = cachedPerformancesById[semester.id]
            val isSelected = semester.id == selectedId
            Session(
                id = semester.id,
                title = semester.title,
                subjects = when {
                    isSelected -> subjects.mapIndexed { index, subject -> subject.toUiSubject(index) }
                    cached != null -> cached.subjects.mapIndexed { index, subject -> subject.toUiSubject(index) }
                    else -> previous?.subjects.orEmpty()
                },
                averageScore = when {
                    isSelected -> averageScore
                    cached != null -> cached.averageScore
                    else -> previous?.averageScore
                }
            )
        }

        val latestAverage = semesterSessions.firstOrNull { it.id == resolvedLatestSemesterId }?.averageScore
            ?: if (selectedId == resolvedLatestSemesterId) averageScore else null
            ?: previousCourse?.averageScore
            ?: "нет оценок"

        return Course(
            id = "student-performance",
            title = "Успеваемость",
            averageScore = latestAverage,
            sessions = semesterSessions
        )
    }

    private fun SemesterPerformance.withCachedSemesters(
        cachedById: Map<String, SemesterPerformance>
    ): SemesterPerformance {
        if (availableSemesters.isNotEmpty()) return this
        val cachedReferences = cachedById.values.map { cached ->
            SemesterReference(
                id = cached.stableSemesterId(),
                title = cached.semesterTitle
            )
        }
        return copy(availableSemesters = cachedReferences)
    }

    private fun SemesterPerformance.stableSemesterId(): String {
        return semesterId?.takeIf { it.isNotBlank() } ?: semesterTitle
    }

    private fun SubjectPerformance.toUiSubject(index: Int): Subject {
        return Subject(
            id = "${subjectName}_${controlType}_$index",
            title = subjectName,
            typeLabel = controlType,
            grade = result
        )
    }
}
