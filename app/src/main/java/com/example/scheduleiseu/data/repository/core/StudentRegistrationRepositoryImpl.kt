package com.example.scheduleiseu.data.repository.core

import com.example.scheduleiseu.data.local.preferences.AppPreferencesDataSource
import com.example.scheduleiseu.data.mapper.contract.StudentRegistrationContextToStudentProfileMapper
import com.example.scheduleiseu.data.mapper.contract.TimeTableDataToStudentRegistrationContextMapper
import com.example.scheduleiseu.data.remote.datasource.ScheduleRemoteDataSource
import com.example.scheduleiseu.data.remote.model.TimeTableData
import com.example.scheduleiseu.data.session.StudentRegistrationSessionStore
import com.example.scheduleiseu.domain.core.model.StudentProfile
import com.example.scheduleiseu.domain.core.model.StudentRegistrationContext
import com.example.scheduleiseu.domain.core.repository.StudentRegistrationRepository

class StudentRegistrationRepositoryImpl(
    private val scheduleRemoteDataSource: ScheduleRemoteDataSource,
    private val sessionStore: StudentRegistrationSessionStore,
    private val contextMapper: TimeTableDataToStudentRegistrationContextMapper,
    private val profileMapper: StudentRegistrationContextToStudentProfileMapper,
    private val preferencesDataSource: AppPreferencesDataSource? = null
) : StudentRegistrationRepository {

    override suspend fun loadInitialContext(): StudentRegistrationContext {
        val data = scheduleRemoteDataSource.getInitialStudentPage()
            ?: throw IllegalStateException("Не удалось загрузить данные регистрации студента")
        sessionStore.saveRawState(data)
        return contextMapper.map(data)
    }

    override suspend fun selectFaculty(facultyId: String): StudentRegistrationContext {
        val current = requireRawState()
        val updated = scheduleRemoteDataSource.updateOnFacultySelect(facultyId, current)
            ?: throw IllegalStateException("Не удалось обновить список после выбора факультета")
        sessionStore.saveRawState(updated)
        return contextMapper.map(updated)
    }

    override suspend fun selectDepartment(departmentId: String): StudentRegistrationContext {
        val current = requireRawState()
        val facultyId = current.faculties.firstOrNull { it.isSelected }?.value
            ?: throw IllegalStateException("Сначала выберите факультет")
        val updated = scheduleRemoteDataSource.updateOnDepartmentSelect(
            facultyId = facultyId,
            departmentId = departmentId,
            currentData = current
        ) ?: throw IllegalStateException("Не удалось обновить список после выбора формы обучения")
        sessionStore.saveRawState(updated)
        return contextMapper.map(updated)
    }

    override suspend fun selectCourse(courseId: String): StudentRegistrationContext {
        val current = requireRawState()
        val facultyId = current.faculties.firstOrNull { it.isSelected }?.value
            ?: throw IllegalStateException("Сначала выберите факультет")
        val departmentId = current.departments.firstOrNull { it.isSelected }?.value
            ?: throw IllegalStateException("Сначала выберите форму обучения")
        val updated = scheduleRemoteDataSource.updateOnCourseSelect(
            facultyId = facultyId,
            departmentId = departmentId,
            courseId = courseId,
            currentData = current
        ) ?: throw IllegalStateException("Не удалось обновить список после выбора курса")
        sessionStore.saveRawState(updated)
        return contextMapper.map(updated)
    }

    override suspend fun selectGroup(groupId: String): StudentRegistrationContext {
        val current = requireRawState()
        val facultyId = current.faculties.firstOrNull { it.isSelected }?.value
            ?: throw IllegalStateException("Сначала выберите факультет")
        val departmentId = current.departments.firstOrNull { it.isSelected }?.value
            ?: throw IllegalStateException("Сначала выберите форму обучения")
        val courseId = current.courses.firstOrNull { it.isSelected }?.value
            ?: throw IllegalStateException("Сначала выберите курс")
        val updated = scheduleRemoteDataSource.updateOnGroupSelect(
            facultyId = facultyId,
            departmentId = departmentId,
            courseId = courseId,
            groupId = groupId,
            currentData = current
        ) ?: throw IllegalStateException("Не удалось обновить список после выбора группы")
        sessionStore.saveRawState(updated)
        return contextMapper.map(updated)
    }

    override suspend fun saveStudentProfile(fullName: String, subgroup: String): StudentProfile {
        val normalizedName = fullName.trim()
        val normalizedSubgroup = subgroup.trim()
        require(normalizedName.isNotBlank()) { "Введите имя" }
        require(normalizedSubgroup.isNotBlank()) { "Выберите подгруппу" }

        val current = requireRawState()
        val context = contextMapper.map(current)

        if (context.selectedFacultyId == null || context.selectedDepartmentId == null ||
            context.selectedCourseId == null || context.selectedGroupId == null
        ) {
            throw IllegalStateException("Выберите факультет, форму обучения, курс и группу")
        }

        val profile = profileMapper.map(normalizedName, normalizedSubgroup, context)
        sessionStore.saveProfile(profile)
        preferencesDataSource?.saveStudentProfile(profile)
        return profile
    }

    override fun getSavedStudentProfile(): StudentProfile? =
        sessionStore.getSavedProfile() ?: preferencesDataSource?.getStudentProfileBlocking()

    private fun requireRawState(): TimeTableData {
        return sessionStore.getRawState()
            ?: throw IllegalStateException("Состояние регистрации не инициализировано")
    }
}
