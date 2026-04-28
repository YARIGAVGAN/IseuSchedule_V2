package com.example.scheduleiseu.feature.menu

import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scheduleiseu.domain.core.model.StudentProfile
import com.example.scheduleiseu.domain.core.model.TeacherProfile
import com.example.scheduleiseu.domain.core.model.UserPhoto
import com.example.scheduleiseu.domain.core.model.UserRole
import com.example.scheduleiseu.domain.core.usecase.GetActiveUserRoleUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveCachedUserPhotoUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveLatestPerformanceUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveStudentProfileUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveTeacherProfileUseCase
import com.example.scheduleiseu.domain.core.usecase.RefreshStudentProfileUseCase
import com.example.scheduleiseu.domain.core.usecase.RefreshTeacherProfileUseCase
import com.example.scheduleiseu.domain.core.usecase.RefreshUserPhotoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MenuProfileViewModel(
    private val getActiveUserRole: GetActiveUserRoleUseCase,
    private val observeStudentProfile: ObserveStudentProfileUseCase,
    private val observeTeacherProfile: ObserveTeacherProfileUseCase,
    private val observeCachedUserPhoto: ObserveCachedUserPhotoUseCase,
    private val observeLatestPerformance: ObserveLatestPerformanceUseCase,
    private val refreshStudentProfile: RefreshStudentProfileUseCase,
    private val refreshTeacherProfile: RefreshTeacherProfileUseCase,
    private val refreshUserPhoto: RefreshUserPhotoUseCase,
    private val isStudentScheduleOnlyModeProvider: () -> Boolean
) : ViewModel() {

    private val _state = MutableStateFlow(MenuProfileUiState())
    val state: StateFlow<MenuProfileUiState> = _state.asStateFlow()

    init {
        observeCachedProfile()
        observeCachedPhoto()
        refreshProfile()
        refreshPhoto()
    }

    fun retry() {
        refreshProfile()
        refreshPhoto()
    }

    private fun observeCachedProfile() {
        combine(
            observeStudentProfile(),
            observeTeacherProfile(),
            observeLatestPerformance()
        ) { studentProfile, teacherProfile, latestPerformance ->
            val scheduleOnly = isStudentScheduleOnlyModeProvider()
            when (getActiveUserRole()) {
                UserRole.STUDENT -> studentProfile?.toUiState(latestPerformance?.averageScore, scheduleOnly)
                UserRole.TEACHER -> teacherProfile?.toUiState()
                null -> studentProfile?.toUiState(latestPerformance?.averageScore, scheduleOnly) ?: teacherProfile?.toUiState()
            }
        }.onEach { cachedState ->
            if (cachedState != null) {
                applyProfileState(cachedState, isLoading = false, errorMessage = null)
            }
        }.launchIn(viewModelScope)
    }

    private fun observeCachedPhoto() {
        combine(
            observeCachedUserPhoto(UserRole.STUDENT),
            observeCachedUserPhoto(UserRole.TEACHER)
        ) { studentPhoto, teacherPhoto ->
            when (getActiveUserRole()) {
                UserRole.STUDENT -> if (isStudentScheduleOnlyModeProvider()) null else studentPhoto
                UserRole.TEACHER -> teacherPhoto
                null -> if (isStudentScheduleOnlyModeProvider()) null else studentPhoto ?: teacherPhoto
            }
        }.onEach { photo ->
            _state.update { current ->
                current.copy(
                    photoBitmap = if (current.isScheduleOnlyMode) null else photo?.toBitmapOrNull() ?: current.photoBitmap,
                    isPhotoLoading = false
                )
            }
        }.catch {
            _state.update { current -> current.copy(isPhotoLoading = false) }
        }.launchIn(viewModelScope)
    }

    private fun refreshProfile() {
        if (getActiveUserRole() == UserRole.STUDENT && isStudentScheduleOnlyModeProvider()) {
            _state.update { current ->
                current.copy(
                    isLoading = false,
                    errorMessage = null,
                    isScheduleOnlyMode = true,
                    isPhotoLoading = false,
                    showPhoto = true
                )
            }
            return
        }

        viewModelScope.launch {
            val hasCachedProfile = _state.value.fullName != "Профиль"

            _state.update { current ->
                current.copy(
                    isLoading = !hasCachedProfile,
                    errorMessage = null
                )
            }

            val result = runCatching {
                when (getActiveUserRole()) {
                    UserRole.STUDENT -> refreshStudentProfile().toUiState(_state.value.averageScore, isScheduleOnly = false)
                    UserRole.TEACHER -> refreshTeacherProfile().toUiState()
                    null -> null
                }
            }

            result.onSuccess { refreshedState ->
                if (refreshedState != null) {
                    applyProfileState(refreshedState, isLoading = false, errorMessage = null)
                } else {
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = if (current.fullName == "Профиль") "Нет активной сессии" else null
                        )
                    }
                }
            }.onFailure { throwable ->
                _state.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = if (current.fullName == "Профиль") {
                            throwable.message ?: "Не удалось загрузить профиль"
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    private fun refreshPhoto() {
        val role = getActiveUserRole()

        if (role == null || role == UserRole.TEACHER || isStudentScheduleOnlyModeProvider()) {
            _state.update { current -> current.copy(isPhotoLoading = false, photoBitmap = if (current.isScheduleOnlyMode) null else current.photoBitmap) }
            return
        }

        viewModelScope.launch {
            runCatching { refreshUserPhoto() }
                .onSuccess { photo ->
                    _state.update { current ->
                        current.copy(
                            photoBitmap = photo?.toBitmapOrNull() ?: current.photoBitmap,
                            isPhotoLoading = false
                        )
                    }
                }
                .onFailure {
                    _state.update { current -> current.copy(isPhotoLoading = false) }
                }
        }
    }

    private fun applyProfileState(
        profileState: MenuProfileUiState,
        isLoading: Boolean,
        errorMessage: String?
    ) {
        _state.update { current ->
            profileState.copy(
                isLoading = isLoading,
                errorMessage = errorMessage,
                photoBitmap = if (profileState.isScheduleOnlyMode) null else current.photoBitmap,
                isPhotoLoading = if (profileState.isScheduleOnlyMode) false else current.isPhotoLoading
            )
        }
    }

    private fun StudentProfile.toUiState(
        latestSemesterAverageScore: String?,
        isScheduleOnly: Boolean
    ): MenuProfileUiState {
        val info = listOfNotNull(course, group?.let { "группа $it" })
            .joinToString(", ")
            .ifBlank { group ?: faculty ?: "Студент" }

        return MenuProfileUiState(
            fullName = fullName,
            role = if (isScheduleOnly) "Студент" else "Студент",
            isTeacherMode = false,
            isScheduleOnlyMode = isScheduleOnly,
            groupOrPosition = faculty ?: "БГУ",
            details = if (isScheduleOnly) "$info • режим без регистрации" else info,
            averageScore = if (isScheduleOnly) null else latestSemesterAverageScore ?: averageScore,
            photoBitmap = null,
            isPhotoLoading = !isScheduleOnly,
            showPhoto = true
        )
    }

    private fun TeacherProfile.toUiState(): MenuProfileUiState {
        return MenuProfileUiState(
            fullName = fullName,
            role = "Преподаватель",
            isTeacherMode = true,
            isScheduleOnlyMode = false,
            groupOrPosition = department ?: faculty ?: "БГУ",
            details = listOfNotNull(faculty, department).distinct().joinToString(" • ").ifBlank { "Преподаватель" },
            averageScore = null,
            photoBitmap = null,
            isPhotoLoading = false,
            showPhoto = false
        )
    }

    private fun UserPhoto.toBitmapOrNull() = bytes.toBitmapOrNull()

    private fun ByteArray.toBitmapOrNull() = runCatching {
        BitmapFactory.decodeByteArray(this, 0, size)
    }.getOrNull()
}
