package com.example.scheduleiseu.domain.core.usecase

import com.example.scheduleiseu.domain.core.model.ScheduleContext
import com.example.scheduleiseu.domain.core.model.ScheduleWeek
import com.example.scheduleiseu.domain.core.model.WeekInfo
import com.example.scheduleiseu.domain.core.repository.ScheduleRepository
import com.example.scheduleiseu.domain.core.repository.StudentRegistrationRepository
import com.example.scheduleiseu.domain.core.repository.TeacherRegistrationRepository

class LoadStudentScheduleContextUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val studentRegistrationRepository: StudentRegistrationRepository
) {
    suspend operator fun invoke(): ScheduleContext {
        val profile = studentRegistrationRepository.getSavedStudentProfile()
            ?: throw IllegalStateException("Профиль студента не сохранен")

        val facultyId = profile.facultyId
            ?: throw IllegalStateException("Не выбран факультет")
        val departmentId = profile.departmentId
            ?: throw IllegalStateException("Не выбрана форма обучения")
        val courseId = profile.courseId
            ?: throw IllegalStateException("Не выбран курс")

        var context = scheduleRepository.refreshStudentScheduleContext()
        context = scheduleRepository.updateStudentContextByFaculty(context, facultyId)
        context = scheduleRepository.updateStudentContextByDepartment(context, facultyId, departmentId)
        context = scheduleRepository.updateStudentContextByCourse(context, facultyId, departmentId, courseId)
        return context
    }
}

class LoadTeacherScheduleContextUseCase(
    private val scheduleRepository: ScheduleRepository
) {
    suspend operator fun invoke(): ScheduleContext = scheduleRepository.refreshTeacherScheduleContext()
}

class LoadExternalStudentGroupScheduleUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val studentRegistrationRepository: StudentRegistrationRepository
) {
    suspend operator fun invoke(groupId: String, week: WeekInfo): ScheduleWeek {
        val profile = studentRegistrationRepository.getSavedStudentProfile()
            ?: throw IllegalStateException("Профиль студента не сохранен")

        val facultyId = profile.facultyId
            ?: throw IllegalStateException("Не выбран факультет")
        val departmentId = profile.departmentId
            ?: throw IllegalStateException("Не выбрана форма обучения")
        val courseId = profile.courseId
            ?: throw IllegalStateException("Не выбран курс")

        val context = scheduleRepository.refreshStudentScheduleContext()
        return scheduleRepository.refreshStudentSchedule(
            context = context,
            facultyId = facultyId,
            departmentId = departmentId,
            courseId = courseId,
            groupId = groupId,
            week = week
        )
    }
}

class LoadExternalTeacherScheduleUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val teacherRegistrationRepository: TeacherRegistrationRepository
) {
    suspend operator fun invoke(teacherId: String, week: WeekInfo): ScheduleWeek {
        teacherRegistrationRepository.getSavedTeacherProfile()
        val context = scheduleRepository.refreshTeacherScheduleContext()
        return scheduleRepository.refreshTeacherSchedule(
            context = context,
            teacherId = teacherId,
            week = week
        )
    }
}
