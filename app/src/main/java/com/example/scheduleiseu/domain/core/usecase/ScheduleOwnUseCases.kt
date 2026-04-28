package com.example.scheduleiseu.domain.core.usecase

import com.example.scheduleiseu.domain.core.model.ScheduleWeek
import com.example.scheduleiseu.domain.core.model.WeekInfo
import com.example.scheduleiseu.domain.core.repository.ScheduleRepository
import com.example.scheduleiseu.domain.core.repository.StudentRegistrationRepository
import com.example.scheduleiseu.domain.core.repository.TeacherRegistrationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class ObserveOwnStudentScheduleUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val studentRegistrationRepository: StudentRegistrationRepository
) {
    operator fun invoke(): Flow<ScheduleWeek?> = flow {
        val profile = studentRegistrationRepository.getSavedStudentProfile()
            ?: throw IllegalStateException("Профиль студента не сохранен")

        val facultyId = profile.facultyId
            ?: throw IllegalStateException("Не выбран факультет")
        val departmentId = profile.departmentId
            ?: throw IllegalStateException("Не выбрана форма обучения")
        val courseId = profile.courseId
            ?: throw IllegalStateException("Не выбран курс")
        val groupId = profile.groupId
            ?: throw IllegalStateException("Не выбрана группа")

        emitAll(
            scheduleRepository.observeCachedStudentSchedule(
                facultyId = facultyId,
                departmentId = departmentId,
                courseId = courseId,
                groupId = groupId
            )
        )
    }
}

class ObserveOwnStudentScheduleForWeekUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val studentRegistrationRepository: StudentRegistrationRepository
) {
    operator fun invoke(week: WeekInfo): Flow<ScheduleWeek?> = flow {
        val profile = studentRegistrationRepository.getSavedStudentProfile()
            ?: throw IllegalStateException("Профиль студента не сохранен")

        val facultyId = profile.facultyId
            ?: throw IllegalStateException("Не выбран факультет")
        val departmentId = profile.departmentId
            ?: throw IllegalStateException("Не выбрана форма обучения")
        val courseId = profile.courseId
            ?: throw IllegalStateException("Не выбран курс")
        val groupId = profile.groupId
            ?: throw IllegalStateException("Не выбрана группа")

        emitAll(
            scheduleRepository.observeCachedStudentSchedule(
                facultyId = facultyId,
                departmentId = departmentId,
                courseId = courseId,
                groupId = groupId,
                week = week
            )
        )
    }
}

class ObserveOwnStudentCachedWeekValuesUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val studentRegistrationRepository: StudentRegistrationRepository
) {
    operator fun invoke(): Flow<Set<String>> = flow {
        val profile = studentRegistrationRepository.getSavedStudentProfile()
            ?: throw IllegalStateException("Профиль студента не сохранен")

        val facultyId = profile.facultyId
            ?: throw IllegalStateException("Не выбран факультет")
        val departmentId = profile.departmentId
            ?: throw IllegalStateException("Не выбрана форма обучения")
        val courseId = profile.courseId
            ?: throw IllegalStateException("Не выбран курс")
        val groupId = profile.groupId
            ?: throw IllegalStateException("Не выбрана группа")

        emitAll(
            scheduleRepository.observeCachedStudentWeekValues(
                facultyId = facultyId,
                departmentId = departmentId,
                courseId = courseId,
                groupId = groupId
            )
        )
    }
}

class ObserveOwnStudentCachedWeeksUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val studentRegistrationRepository: StudentRegistrationRepository
) {
    operator fun invoke(): Flow<List<WeekInfo>> = flow {
        val profile = studentRegistrationRepository.getSavedStudentProfile()
            ?: throw IllegalStateException("Профиль студента не сохранен")

        val facultyId = profile.facultyId
            ?: throw IllegalStateException("Не выбран факультет")
        val departmentId = profile.departmentId
            ?: throw IllegalStateException("Не выбрана форма обучения")
        val courseId = profile.courseId
            ?: throw IllegalStateException("Не выбран курс")
        val groupId = profile.groupId
            ?: throw IllegalStateException("Не выбрана группа")

        emitAll(
            scheduleRepository.observeCachedStudentWeeks(
                facultyId = facultyId,
                departmentId = departmentId,
                courseId = courseId,
                groupId = groupId
            )
        )
    }
}

class RefreshOwnStudentScheduleUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val studentRegistrationRepository: StudentRegistrationRepository,
    private val onOwnScheduleRefreshed: suspend () -> Unit = {}
) {
    suspend operator fun invoke(): ScheduleWeek {
        val profile = studentRegistrationRepository.getSavedStudentProfile()
            ?: throw IllegalStateException("Профиль студента не сохранен")

        val facultyId = profile.facultyId
            ?: throw IllegalStateException("Не выбран факультет")
        val departmentId = profile.departmentId
            ?: throw IllegalStateException("Не выбрана форма обучения")
        val courseId = profile.courseId
            ?: throw IllegalStateException("Не выбран курс")
        val groupId = profile.groupId
            ?: throw IllegalStateException("Не выбрана группа")

        val context = scheduleRepository.refreshStudentScheduleContext()
        val currentWeek = context.currentWeek
            ?: context.weeks.firstOrNull { it.isCurrent }
            ?: scheduleRepository.refreshStudentCurrentWeek()
            ?: context.selectedWeek
            ?: context.weeks.firstOrNull()
            ?: throw IllegalStateException("Не удалось определить текущую неделю")

        val week = scheduleRepository.refreshStudentSchedule(
            context = context,
            facultyId = facultyId,
            departmentId = departmentId,
            courseId = courseId,
            groupId = groupId,
            week = currentWeek
        )
        onOwnScheduleRefreshed()
        return week
    }
}

class RefreshOwnStudentScheduleForWeekUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val studentRegistrationRepository: StudentRegistrationRepository,
    private val onOwnScheduleRefreshed: suspend () -> Unit = {}
) {
    suspend operator fun invoke(week: WeekInfo): ScheduleWeek {
        val profile = studentRegistrationRepository.getSavedStudentProfile()
            ?: throw IllegalStateException("Профиль студента не сохранен")

        val facultyId = profile.facultyId
            ?: throw IllegalStateException("Не выбран факультет")
        val departmentId = profile.departmentId
            ?: throw IllegalStateException("Не выбрана форма обучения")
        val courseId = profile.courseId
            ?: throw IllegalStateException("Не выбран курс")
        val groupId = profile.groupId
            ?: throw IllegalStateException("Не выбрана группа")

        val context = scheduleRepository.refreshStudentScheduleContext()
        val scheduleWeek = scheduleRepository.refreshStudentSchedule(
            context = context,
            facultyId = facultyId,
            departmentId = departmentId,
            courseId = courseId,
            groupId = groupId,
            week = week
        )
        onOwnScheduleRefreshed()
        return scheduleWeek
    }
}

class ObserveOwnTeacherScheduleUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val teacherRegistrationRepository: TeacherRegistrationRepository
) {
    operator fun invoke(): Flow<ScheduleWeek?> = flow {
        val profile = teacherRegistrationRepository.getSavedTeacherProfile()
            ?: throw IllegalStateException("Профиль преподавателя не сохранен")
        val teacherId = profile.teacherId
            ?: throw IllegalStateException("Не выбран преподаватель")

        emitAll(scheduleRepository.observeCachedTeacherSchedule(teacherId = teacherId))
    }
}

class ObserveOwnTeacherScheduleForWeekUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val teacherRegistrationRepository: TeacherRegistrationRepository
) {
    operator fun invoke(week: WeekInfo): Flow<ScheduleWeek?> = flow {
        val profile = teacherRegistrationRepository.getSavedTeacherProfile()
            ?: throw IllegalStateException("Профиль преподавателя не сохранен")
        val teacherId = profile.teacherId
            ?: throw IllegalStateException("Не выбран преподаватель")

        emitAll(scheduleRepository.observeCachedTeacherSchedule(teacherId = teacherId, week = week))
    }
}

class ObserveOwnTeacherCachedWeekValuesUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val teacherRegistrationRepository: TeacherRegistrationRepository
) {
    operator fun invoke(): Flow<Set<String>> = flow {
        val profile = teacherRegistrationRepository.getSavedTeacherProfile()
            ?: throw IllegalStateException("Профиль преподавателя не сохранен")
        val teacherId = profile.teacherId
            ?: throw IllegalStateException("Не выбран преподаватель")

        emitAll(scheduleRepository.observeCachedTeacherWeekValues(teacherId = teacherId))
    }
}

class ObserveOwnTeacherCachedWeeksUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val teacherRegistrationRepository: TeacherRegistrationRepository
) {
    operator fun invoke(): Flow<List<WeekInfo>> = flow {
        val profile = teacherRegistrationRepository.getSavedTeacherProfile()
            ?: throw IllegalStateException("Профиль преподавателя не сохранен")
        val teacherId = profile.teacherId
            ?: throw IllegalStateException("Не выбран преподаватель")

        emitAll(scheduleRepository.observeCachedTeacherWeeks(teacherId = teacherId))
    }
}

class RefreshOwnTeacherScheduleUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val teacherRegistrationRepository: TeacherRegistrationRepository,
    private val onOwnScheduleRefreshed: suspend () -> Unit = {}
) {
    suspend operator fun invoke(): ScheduleWeek {
        val profile = teacherRegistrationRepository.getSavedTeacherProfile()
            ?: throw IllegalStateException("Профиль преподавателя не сохранен")
        val teacherId = profile.teacherId
            ?: throw IllegalStateException("Не выбран преподаватель")

        val context = scheduleRepository.refreshTeacherScheduleContext()
        val currentWeek = context.currentWeek
            ?: context.weeks.firstOrNull { it.isCurrent }
            ?: scheduleRepository.refreshTeacherCurrentWeek()
            ?: context.selectedWeek
            ?: context.weeks.firstOrNull()
            ?: throw IllegalStateException("Не удалось определить текущую неделю")

        val week = scheduleRepository.refreshTeacherSchedule(
            context = context,
            teacherId = teacherId,
            week = currentWeek
        )
        onOwnScheduleRefreshed()
        return week
    }
}

class RefreshOwnTeacherScheduleForWeekUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val teacherRegistrationRepository: TeacherRegistrationRepository,
    private val onOwnScheduleRefreshed: suspend () -> Unit = {}
) {
    suspend operator fun invoke(week: WeekInfo): ScheduleWeek {
        val profile = teacherRegistrationRepository.getSavedTeacherProfile()
            ?: throw IllegalStateException("Профиль преподавателя не сохранен")
        val teacherId = profile.teacherId
            ?: throw IllegalStateException("Не выбран преподаватель")

        val context = scheduleRepository.refreshTeacherScheduleContext()
        val scheduleWeek = scheduleRepository.refreshTeacherSchedule(
            context = context,
            teacherId = teacherId,
            week = week
        )
        onOwnScheduleRefreshed()
        return scheduleWeek
    }
}
