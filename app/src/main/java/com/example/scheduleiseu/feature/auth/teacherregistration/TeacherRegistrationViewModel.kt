package com.example.scheduleiseu.feature.auth.teacherregistration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scheduleiseu.domain.core.model.TeacherRegistrationContext
import com.example.scheduleiseu.domain.core.usecase.LoadTeacherRegistrationContextUseCase
import com.example.scheduleiseu.domain.core.usecase.SaveTeacherProfileUseCase
import com.example.scheduleiseu.domain.model.TeacherSearchItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeacherRegistrationViewModel(
    private val loadContext: LoadTeacherRegistrationContextUseCase,
    private val saveProfile: SaveTeacherProfileUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherRegistrationUiState())
    val state: StateFlow<TeacherRegistrationUiState> = _state.asStateFlow()

    private val _searchState = MutableStateFlow(TeacherSearchUiState())
    val searchState: StateFlow<TeacherSearchUiState> = _searchState.asStateFlow()

    private val _events = MutableSharedFlow<TeacherRegistrationUiEvent>()
    val events: SharedFlow<TeacherRegistrationUiEvent> = _events.asSharedFlow()

    private var currentContext: TeacherRegistrationContext? = null
    private var selectedTeacherId: String? = null

    init {
        ensureContextLoaded(showSheetOnSuccess = false)
    }

    fun onAction(action: TeacherRegistrationAction) {
        when (action) {
            TeacherRegistrationAction.SearchTeacherClicked -> ensureContextLoaded(showSheetOnSuccess = true)
            TeacherRegistrationAction.CreateAccountClicked -> createAccount()
            TeacherRegistrationAction.ContinueWithoutRegistrationClicked -> Unit
        }
    }

    fun onSearchQueryChange(query: String) {
        val context = currentContext ?: return
        _searchState.update { current ->
            current.copy(
                query = query,
                results = filterTeachers(context, query)
            )
        }
    }

    fun onClearSearchQueryClick() {
        val context = currentContext
        _searchState.update { current ->
            current.copy(
                query = "",
                results = if (context == null) emptyList() else emptyList()
            )
        }
    }

    fun onTeacherSelected(item: TeacherSearchItem) {
        selectedTeacherId = item.id
        _state.update {
            it.copy(
                selectedTeacherName = item.fullName,
                errorMessage = null,
                controlsEnabled = true,
                isLoading = false
            )
        }
        _searchState.update {
            it.copy(
                isVisible = false,
                query = "",
                results = emptyList(),
                isLoading = false,
                errorMessage = null
            )
        }
    }

    fun onSearchDismiss() {
        _searchState.update {
            it.copy(
                isVisible = false,
                query = "",
                results = emptyList(),
                isLoading = false,
                errorMessage = null
            )
        }
    }

    private fun ensureContextLoaded(showSheetOnSuccess: Boolean) {
        val existingContext = currentContext
        if (existingContext != null) {
            if (showSheetOnSuccess) {
                _searchState.update {
                    it.copy(
                        isVisible = true,
                        query = "",
                        results = emptyList(),
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(isLoading = true, controlsEnabled = false, errorMessage = null)
            }
            _searchState.update {
                it.copy(isVisible = showSheetOnSuccess, isLoading = true, errorMessage = null)
            }

            runCatching { loadContext() }
                .onSuccess { context ->
                    currentContext = context
                    _state.update {
                        it.copy(isLoading = false, controlsEnabled = true, errorMessage = null)
                    }
                    _searchState.update {
                        it.copy(
                            isVisible = showSheetOnSuccess,
                            query = "",
                            results = emptyList(),
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    val message = throwable.message ?: "Не удалось загрузить список преподавателей"
                    _state.update {
                        it.copy(isLoading = false, controlsEnabled = true, errorMessage = message)
                    }
                    _searchState.update {
                        it.copy(
                            isVisible = false,
                            isLoading = false,
                            errorMessage = message,
                            results = emptyList()
                        )
                    }
                }
        }
    }

    private fun createAccount() {
        val teacherId = selectedTeacherId
        if (teacherId.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "Выберите преподавателя") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, controlsEnabled = false, errorMessage = null) }
            runCatching { saveProfile(teacherId) }
                .onSuccess { profile ->
                    _state.update { it.copy(isLoading = false, controlsEnabled = true, errorMessage = null) }
                    _events.emit(TeacherRegistrationUiEvent.RegistrationCompleted(profile))
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            controlsEnabled = true,
                            errorMessage = throwable.message ?: "Не удалось сохранить профиль преподавателя"
                        )
                    }
                }
        }
    }

    private fun filterTeachers(
        context: TeacherRegistrationContext,
        query: String
    ): List<TeacherSearchItem> {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) return emptyList()

        return context.teachers
            .asSequence()
            .filter { option -> option.title.lowercase().contains(normalizedQuery) }
            .map { option -> TeacherSearchItem(id = option.id, fullName = option.title) }
            .take(50)
            .toList()
    }
}

data class TeacherSearchUiState(
    val isVisible: Boolean = false,
    val query: String = "",
    val results: List<TeacherSearchItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
