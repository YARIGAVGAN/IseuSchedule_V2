package com.example.scheduleiseu.domain.core.usecase

import com.example.scheduleiseu.domain.core.model.AuthSession
import com.example.scheduleiseu.domain.core.model.ScheduleContext
import com.example.scheduleiseu.domain.core.model.StudentProfile
import com.example.scheduleiseu.domain.core.model.UserRole
import com.example.scheduleiseu.domain.core.repository.ActiveSessionRepository
import com.example.scheduleiseu.domain.core.repository.BootstrapRepository
import com.example.scheduleiseu.domain.core.repository.PerformanceRepository
import com.example.scheduleiseu.domain.core.repository.ProfileRepository
import com.example.scheduleiseu.domain.core.repository.ScheduleRepository
import com.example.scheduleiseu.domain.core.repository.StudentRegistrationRepository

class FirstEntryBootstrapUseCase(
    private val activeSessionRepository: ActiveSessionRepository,
    private val bootstrapRepository: BootstrapRepository,
    private val profileRepository: ProfileRepository,
    private val studentRegistrationRepository: StudentRegistrationRepository,
    private val scheduleRepository: ScheduleRepository,
    private val performanceRepository: PerformanceRepository,
    private val onOwnScheduleRefreshed: suspend () -> Unit = {}
) {
    suspend fun isCompleted(accountKey: String): Boolean {
        return bootstrapRepository.isStudentBootstrapCompleted(accountKey.normalizedAccountKey())
    }

    suspend operator fun invoke(accountKey: String) {
        val normalizedAccountKey = accountKey.normalizedAccountKey()
        if (bootstrapRepository.isStudentBootstrapCompleted(normalizedAccountKey)) return

        val session = activeSessionRepository.getActiveSession()
            ?: throw IllegalStateException("Сессия личного кабинета не активна")
        validateStudentSession(session)

        val remoteProfile = profileRepository.refreshStudentProfile(session)
        val profile = studentRegistrationRepository.getSavedStudentProfile()
            ?.mergeRemoteStudentInfo(remoteProfile)
            ?: remoteProfile

        validateRegistrationProfile(profile)
        loadUserPhoto(session)
        loadStudentSchedule(profile)
        onOwnScheduleRefreshed()
        loadPerformance(session)

        bootstrapRepository.markStudentBootstrapCompleted(normalizedAccountKey)
    }

    private suspend fun loadUserPhoto(session: AuthSession) {
        profileRepository.refreshUserPhoto(session)
    }

    private suspend fun loadStudentSchedule(profile: StudentProfile) {
        val facultyId = profile.facultyId.requiredField("Не выбран факультет")
        val departmentId = profile.departmentId.requiredField("Не выбрана форма обучения")
        val courseId = profile.courseId.requiredField("Не выбран курс")
        val groupId = profile.groupId.requiredField("Не выбрана группа")

        val context = scheduleRepository.refreshStudentScheduleContext()
        val currentWeek = context.resolvePrimaryWeek()
            ?: scheduleRepository.refreshStudentCurrentWeek()
            ?: throw IllegalStateException("Не удалось определить текущую неделю")

        scheduleRepository.refreshStudentSchedule(
            context = context,
            facultyId = facultyId,
            departmentId = departmentId,
            courseId = courseId,
            groupId = groupId,
            week = currentWeek
        )
    }

    private suspend fun loadPerformance(session: AuthSession) {
        val semesters = performanceRepository.refreshAvailableSemesters(session)
        if (semesters.isEmpty()) {
            performanceRepository.refreshLatestSemesterPerformance(session)
            return
        }

        semesters.forEach { semester ->
            performanceRepository.refreshSemesterPerformance(
                session = session,
                semesterId = semester.id,
                semesterTitle = semester.title
            )
        }
    }

    private fun validateStudentSession(session: AuthSession) {
        if (!session.isAuthenticated) {
            throw IllegalStateException("Пользователь не авторизован")
        }
        if (session.userRole != UserRole.STUDENT) {
            throw IllegalStateException("Первичная загрузка доступна только для студента")
        }
    }

    private fun validateRegistrationProfile(profile: StudentProfile) {
        profile.facultyId.requiredField("Для первого входа нужно выбрать факультет")
        profile.departmentId.requiredField("Для первого входа нужно выбрать форму обучения")
        profile.courseId.requiredField("Для первого входа нужно выбрать курс")
        profile.groupId.requiredField("Для первого входа нужно выбрать группу")
    }

    private fun ScheduleContext.resolvePrimaryWeek() = currentWeek
        ?: weeks.firstOrNull { it.isCurrent }
        ?: selectedWeek
        ?: weeks.firstOrNull()

    private fun String?.requiredField(message: String): String {
        return takeUnless { it.isNullOrBlank() } ?: throw IllegalStateException(message)
    }

    private fun StudentProfile.mergeRemoteStudentInfo(remoteProfile: StudentProfile): StudentProfile {
        return copy(
            fullName = remoteProfile.fullName.takeIf { it.isNotBlank() } ?: fullName,
            login = remoteProfile.login.takeUnless(String?::isNullOrBlank) ?: login,
            faculty = faculty.takeUnless(String?::isNullOrBlank) ?: remoteProfile.faculty,
            department = department.takeUnless(String?::isNullOrBlank) ?: remoteProfile.department,
            course = course.takeUnless(String?::isNullOrBlank) ?: remoteProfile.course,
            group = group.takeUnless(String?::isNullOrBlank) ?: remoteProfile.group,
            averageScore = remoteProfile.averageScore.takeUnless(String?::isNullOrBlank) ?: averageScore,
            photo = null
        )
    }

    private fun String.normalizedAccountKey(): String {
        val normalized = trim().lowercase()
        require(normalized.isNotBlank()) { "Не удалось определить аккаунт для первичной загрузки" }
        return normalized
    }
}
