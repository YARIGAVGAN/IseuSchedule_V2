package com.example.scheduleiseu.feature.navigation

import com.example.scheduleiseu.data.local.preferences.AppPreferencesDataSource
import com.example.scheduleiseu.domain.core.model.UserRole
import com.example.scheduleiseu.domain.core.repository.AuthRepository
import com.example.scheduleiseu.domain.core.repository.PerformanceRepository
import com.example.scheduleiseu.domain.core.repository.ProfileRepository
import com.example.scheduleiseu.domain.core.repository.ScheduleRepository
import com.example.scheduleiseu.domain.core.repository.StudentRegistrationRepository
import com.example.scheduleiseu.domain.core.repository.TeacherRegistrationRepository
import com.example.scheduleiseu.notification.LessonNotificationScheduler

internal class AppNavigationLogoutSupport(
    private val authRepository: AuthRepository,
    private val studentRegistrationRepository: StudentRegistrationRepository,
    private val teacherRegistrationRepository: TeacherRegistrationRepository,
    private val scheduleRepository: ScheduleRepository,
    private val performanceRepository: PerformanceRepository,
    private val profileRepository: ProfileRepository,
    private val preferencesDataSource: AppPreferencesDataSource,
    private val lessonNotificationScheduler: LessonNotificationScheduler
) {
    suspend fun logoutStudent(isStudentScheduleOnlyMode: Boolean): Result<Unit> = runCatching {
        if (isStudentScheduleOnlyMode) {
            authRepository.clearActiveSession()
        } else {
            runCatching { authRepository.logout() }
            authRepository.clearActiveSession()
        }
        clearLocalState()
    }

    suspend fun logoutTeacher(): Result<Unit> = runCatching {
        authRepository.clearActiveSession()
        clearLocalState()
    }

    private suspend fun clearLocalState() {
        studentRegistrationRepository.clearSavedStudentProfile()
        teacherRegistrationRepository.clearSavedTeacherProfile()
        scheduleRepository.clearTeacherSessionState()
        scheduleRepository.clearAllCachedScheduleWeeks()
        performanceRepository.clearCachedPerformance()
        profileRepository.clearCachedUserPhotos()
        preferencesDataSource.resetStudentBootstrap()
        preferencesDataSource.clearStudentCredentials()
        preferencesDataSource.clearSessionFlagsForLogout()
        preferencesDataSource.setLessonNotificationsEnabled(false)
        preferencesDataSource.setUserRole(UserRole.STUDENT)
        lessonNotificationScheduler.cancelAll()
    }
}
