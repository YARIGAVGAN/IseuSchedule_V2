package com.example.scheduleiseu.domain.core.usecase

import com.example.scheduleiseu.domain.core.model.AuthSession
import com.example.scheduleiseu.domain.core.model.ScheduleContext
import com.example.scheduleiseu.domain.core.model.StudentProfile
import com.example.scheduleiseu.domain.core.model.TeacherProfile
import com.example.scheduleiseu.domain.core.model.UserRole
import com.example.scheduleiseu.domain.core.model.WeekInfo
import com.example.scheduleiseu.domain.core.repository.ActiveSessionRepository
import com.example.scheduleiseu.domain.core.repository.PerformanceRepository
import com.example.scheduleiseu.domain.core.repository.ProfileRepository
import com.example.scheduleiseu.domain.core.repository.ScheduleRepository
import com.example.scheduleiseu.domain.core.repository.StudentRegistrationRepository
import com.example.scheduleiseu.domain.core.repository.TeacherRegistrationRepository
import kotlinx.coroutines.sync.Mutex

sealed interface BackgroundRefreshResult {
    data object Success : BackgroundRefreshResult
    data object Skipped : BackgroundRefreshResult
    data object LoginRequired : BackgroundRefreshResult
}

class BackgroundRefreshUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val profileRepository: ProfileRepository,
    private val studentRegistrationRepository: StudentRegistrationRepository,
    private val teacherRegistrationRepository: TeacherRegistrationRepository,
    private val scheduleRepository: ScheduleRepository,
    private val performanceRepository: PerformanceRepository,
    private val onOwnScheduleRefreshed: suspend () -> Unit = {}
) {
    private val refreshMutex = Mutex()

    suspend operator fun invoke(): BackgroundRefreshResult {
        if (!refreshMutex.tryLock()) return BackgroundRefreshResult.Skipped
        return try {
            when (val session = activeSessionRepository.getActiveSession()) {
                null -> {
                    refreshStudentScheduleIfPossible()
                    refreshTeacherScheduleIfPossible()
                    BackgroundRefreshResult.LoginRequired
                }
                else -> refreshForSession(session)
            }
        } finally {
            refreshMutex.unlock()
        }
    }

    private suspend fun refreshForSession(session: AuthSession): BackgroundRefreshResult {
        if (!session.isAuthenticated) return BackgroundRefreshResult.LoginRequired

        return when (session.userRole) {
            UserRole.STUDENT -> refreshStudent(session)
            UserRole.TEACHER -> refreshTeacher(session)
        }
    }

    private suspend fun refreshStudent(session: AuthSession): BackgroundRefreshResult {
        var loginRequired = false
        val profile = runCatching {
            profileRepository.refreshStudentProfile(session)
        }.getOrElse {
            loginRequired = true
            studentRegistrationRepository.getSavedStudentProfile() ?: return BackgroundRefreshResult.LoginRequired
        }

        runCatching { refreshStudentSchedule(profile) }
            .onSuccess { onOwnScheduleRefreshed() }
        runCatching { refreshStudentPerformance(session) }.onFailure { loginRequired = true }
        runCatching { profileRepository.refreshUserPhoto(session) }

        return if (loginRequired) BackgroundRefreshResult.LoginRequired else BackgroundRefreshResult.Success
    }

    private suspend fun refreshTeacher(session: AuthSession): BackgroundRefreshResult {
        val profile = runCatching {
            profileRepository.refreshTeacherProfile(session)
        }.getOrElse {
            teacherRegistrationRepository.getSavedTeacherProfile() ?: return BackgroundRefreshResult.LoginRequired
        }

        runCatching { refreshTeacherSchedule(profile) }
            .onSuccess { onOwnScheduleRefreshed() }
        runCatching { profileRepository.refreshUserPhoto(session) }
        return BackgroundRefreshResult.Success
    }

    private suspend fun refreshStudentScheduleIfPossible() {
        val profile = studentRegistrationRepository.getSavedStudentProfile() ?: return
        runCatching { refreshStudentSchedule(profile) }
            .onSuccess { onOwnScheduleRefreshed() }
    }

    private suspend fun refreshTeacherScheduleIfPossible() {
        val profile = teacherRegistrationRepository.getSavedTeacherProfile() ?: return
        runCatching { refreshTeacherSchedule(profile) }
            .onSuccess { onOwnScheduleRefreshed() }
    }

    private suspend fun refreshStudentSchedule(profile: StudentProfile) {
        val facultyId = profile.facultyId.requiredField()
        val departmentId = profile.departmentId.requiredField()
        val courseId = profile.courseId.requiredField()
        val groupId = profile.groupId.requiredField()

        val context = scheduleRepository.refreshStudentScheduleContext()
        val currentWeek = context.resolveCurrentWeek()
            ?: scheduleRepository.refreshStudentCurrentWeek()
            ?: return

        scheduleRepository.refreshStudentSchedule(
            context = context,
            facultyId = facultyId,
            departmentId = departmentId,
            courseId = courseId,
            groupId = groupId,
            week = currentWeek
        )
    }

    private suspend fun refreshTeacherSchedule(profile: TeacherProfile) {
        val teacherId = profile.teacherId.requiredField()
        val context = scheduleRepository.refreshTeacherScheduleContext()
        val currentWeek = context.resolveCurrentWeek()
            ?: scheduleRepository.refreshTeacherCurrentWeek()
            ?: return

        scheduleRepository.refreshTeacherSchedule(
            context = context,
            teacherId = teacherId,
            week = currentWeek
        )
    }

    private suspend fun refreshStudentPerformance(session: AuthSession) {
        val semesters = performanceRepository.refreshAvailableSemesters(session)
        val selectedOrLatest = semesters.lastOrNull() ?: run {
            performanceRepository.refreshLatestSemesterPerformance(session)
            return
        }

        performanceRepository.refreshSemesterPerformance(
            session = session,
            semesterId = selectedOrLatest.id,
            semesterTitle = selectedOrLatest.title
        )
    }

    private fun ScheduleContext.resolveCurrentWeek(): WeekInfo? {
        return currentWeek
            ?: weeks.firstOrNull { it.isCurrent }
            ?: selectedWeek
            ?: weeks.firstOrNull()
    }

    private fun String?.requiredField(): String {
        return takeUnless { it.isNullOrBlank() } ?: throw IllegalStateException("Не заполнены данные регистрации")
    }
}
