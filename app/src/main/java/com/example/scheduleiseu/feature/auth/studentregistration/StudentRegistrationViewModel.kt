package com.example.scheduleiseu.feature.auth.studentregistration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scheduleiseu.data.mapper.contract.StudentRegistrationContextToUiStateMapper
import com.example.scheduleiseu.data.mapper.impl.StudentRegistrationContextToUiStateMapperImpl
import com.example.scheduleiseu.domain.core.repository.StudentRegistrationRepository
import com.example.scheduleiseu.domain.core.usecase.LoadStudentRegistrationContextUseCase
import com.example.scheduleiseu.domain.core.usecase.SaveStudentProfileUseCase
import com.example.scheduleiseu.domain.core.usecase.SelectStudentCourseUseCase
import com.example.scheduleiseu.domain.core.usecase.SelectStudentDepartmentUseCase
import com.example.scheduleiseu.domain.core.usecase.SelectStudentFacultyUseCase
import com.example.scheduleiseu.domain.core.usecase.SelectStudentGroupUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudentRegistrationViewModel(
    private val repository: StudentRegistrationRepository,
    private val uiStateMapper: StudentRegistrationContextToUiStateMapper = StudentRegistrationContextToUiStateMapperImpl()
) : ViewModel() {

    private var currentContextSnapshot: com.example.scheduleiseu.domain.core.model.StudentRegistrationContext? = null

    private val loadContext = LoadStudentRegistrationContextUseCase(repository)
    private val selectFaculty = SelectStudentFacultyUseCase(repository)
    private val selectDepartment = SelectStudentDepartmentUseCase(repository)
    private val selectCourse = SelectStudentCourseUseCase(repository)
    private val selectGroup = SelectStudentGroupUseCase(repository)
    private val saveProfile = SaveStudentProfileUseCase(repository)

    private val _state = MutableStateFlow(StudentRegistrationUiState())
    val state: StateFlow<StudentRegistrationUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<StudentRegistrationUiEvent>()
    val events: SharedFlow<StudentRegistrationUiEvent> = _events.asSharedFlow()

    init {
        loadInitialContext()
    }

    fun onAction(action: StudentRegistrationAction) {
        when (action) {
            is StudentRegistrationAction.NameChanged -> _state.update {
                it.copy(name = action.value, errorMessage = null)
            }
            is StudentRegistrationAction.FacultySelected -> handleFacultySelected(action.value)
            is StudentRegistrationAction.StudyFormSelected -> handleStudyFormSelected(action.value)
            is StudentRegistrationAction.CourseSelected -> handleCourseSelected(action.value)
            is StudentRegistrationAction.GroupSelected -> handleGroupSelected(action.value)
            is StudentRegistrationAction.SubgroupSelected -> _state.update {
                it.copy(selectedSubgroup = action.value, errorMessage = null)
            }
            StudentRegistrationAction.CreateAccountClicked -> handleCreateAccount()
        }
    }

    private fun loadInitialContext() {
        viewModelScope.launch {
            setLoading(true)
            runCatching { loadContext() }
                .onSuccess { context ->
                    val resolvedContext = restoreSavedSelection(context)
                    currentContextSnapshot = resolvedContext
                    val savedSubgroup = repository.getSavedStudentProfile()
                        ?.subgroup
                        ?.takeIf { it in StudentRegistrationUiState().subgroupOptions }
                    _state.update { current ->
                        uiStateMapper.map(resolvedContext, current).copy(
                            selectedSubgroup = current.selectedSubgroup ?: savedSubgroup,
                            controlsEnabled = true,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            controlsEnabled = true,
                            isLoading = false,
                            errorMessage = throwable.message ?: "Не удалось загрузить данные регистрации"
                        )
                    }
                }
        }
    }

    private fun handleFacultySelected(title: String) {
        val selectedId = currentContextSnapshot?.faculties?.firstOrNull { it.title == title }?.id ?: run {
            _state.update { it.copy(errorMessage = "Не удалось определить факультет") }
            return
        }

        viewModelScope.launch {
            setLoading(true)
            runCatching { selectFaculty(selectedId) }
                .onSuccess { context ->
                    currentContextSnapshot = context
                    _state.update { current ->
                        uiStateMapper.map(context, current.copy(errorMessage = null)).copy(
                            controlsEnabled = true,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(controlsEnabled = true, isLoading = false, errorMessage = throwable.message ?: "Не удалось выбрать факультет")
                    }
                }
        }
    }

    private fun handleStudyFormSelected(title: String) {
        val selectedId = currentContextSnapshot?.departments?.firstOrNull { it.title == title }?.id ?: run {
            _state.update { it.copy(errorMessage = "Не удалось определить форму обучения") }
            return
        }

        viewModelScope.launch {
            setLoading(true)
            runCatching { selectDepartment(selectedId) }
                .onSuccess { context ->
                    currentContextSnapshot = context
                    _state.update { current ->
                        uiStateMapper.map(context, current.copy(errorMessage = null)).copy(
                            controlsEnabled = true,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(controlsEnabled = true, isLoading = false, errorMessage = throwable.message ?: "Не удалось выбрать форму обучения")
                    }
                }
        }
    }

    private fun handleCourseSelected(title: String) {
        val selectedId = currentContextSnapshot?.courses?.firstOrNull { it.title == title }?.id ?: run {
            _state.update { it.copy(errorMessage = "Не удалось определить курс") }
            return
        }

        viewModelScope.launch {
            setLoading(true)
            runCatching { selectCourse(selectedId) }
                .onSuccess { context ->
                    currentContextSnapshot = context
                    _state.update { current ->
                        uiStateMapper.map(context, current.copy(errorMessage = null)).copy(
                            controlsEnabled = true,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(controlsEnabled = true, isLoading = false, errorMessage = throwable.message ?: "Не удалось выбрать курс")
                    }
                }
        }
    }

    private fun handleGroupSelected(title: String) {
        val selectedId = currentContextSnapshot?.groups?.firstOrNull { it.title == title }?.id ?: run {
            _state.update { it.copy(errorMessage = "Не удалось определить группу") }
            return
        }

        viewModelScope.launch {
            setLoading(true)
            runCatching { selectGroup(selectedId) }
                .onSuccess { context ->
                    currentContextSnapshot = context
                    _state.update { current ->
                        uiStateMapper.map(context, current.copy(errorMessage = null)).copy(
                            controlsEnabled = true,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(controlsEnabled = true, isLoading = false, errorMessage = throwable.message ?: "Не удалось выбрать группу")
                    }
                }
        }
    }

    private fun handleCreateAccount() {
        val stateValue = _state.value
        viewModelScope.launch {
            setLoading(true)
            val subgroup = stateValue.selectedSubgroup
            if (subgroup.isNullOrBlank()) {
                _state.update {
                    it.copy(
                        controlsEnabled = true,
                        isLoading = false,
                        errorMessage = "Выберите подгруппу"
                    )
                }
                return@launch
            }
            runCatching { saveProfile(stateValue.name, subgroup) }
                .onSuccess { profile ->
                    _state.update {
                        it.copy(controlsEnabled = true, isLoading = false, errorMessage = null)
                    }
                    _events.emit(StudentRegistrationUiEvent.RegistrationCompleted(profile))
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            controlsEnabled = true,
                            isLoading = false,
                            errorMessage = throwable.message ?: "Не удалось сохранить профиль студента"
                        )
                    }
                }
        }
    }

    fun prefillNameIfBlank(name: String?) {
        val normalized = name?.trim().orEmpty()
        if (normalized.isBlank()) return
        _state.update { current ->
            if (current.name.isBlank()) current.copy(name = normalized, errorMessage = null) else current
        }
    }

    private fun setLoading(isLoading: Boolean) {
        _state.update {
            it.copy(
                isLoading = isLoading,
                controlsEnabled = !isLoading
            )
        }
    }

    private suspend fun restoreSavedSelection(
        initialContext: com.example.scheduleiseu.domain.core.model.StudentRegistrationContext
    ): com.example.scheduleiseu.domain.core.model.StudentRegistrationContext {
        val savedProfile = repository.getSavedStudentProfile() ?: return initialContext
        var context = initialContext

        savedProfile.facultyId
            ?.takeIf { savedId -> context.faculties.any { it.id == savedId } }
            ?.let { savedId -> context = selectFaculty(savedId) }

        savedProfile.departmentId
            ?.takeIf { savedId -> context.departments.any { it.id == savedId } }
            ?.let { savedId -> context = selectDepartment(savedId) }

        savedProfile.courseId
            ?.takeIf { savedId -> context.courses.any { it.id == savedId } }
            ?.let { savedId -> context = selectCourse(savedId) }

        savedProfile.groupId
            ?.takeIf { savedId -> context.groups.any { it.id == savedId } }
            ?.let { savedId -> context = selectGroup(savedId) }

        return context
    }
}
