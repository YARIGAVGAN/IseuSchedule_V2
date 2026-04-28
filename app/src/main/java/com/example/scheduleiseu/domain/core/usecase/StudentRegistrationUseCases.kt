package com.example.scheduleiseu.domain.core.usecase

import com.example.scheduleiseu.domain.core.model.StudentProfile
import com.example.scheduleiseu.domain.core.model.StudentRegistrationContext
import com.example.scheduleiseu.domain.core.repository.StudentRegistrationRepository

class LoadStudentRegistrationContextUseCase(
    private val repository: StudentRegistrationRepository
) {
    suspend operator fun invoke(): StudentRegistrationContext = repository.loadInitialContext()
}

class SelectStudentFacultyUseCase(
    private val repository: StudentRegistrationRepository
) {
    suspend operator fun invoke(facultyId: String): StudentRegistrationContext = repository.selectFaculty(facultyId)
}

class SelectStudentDepartmentUseCase(
    private val repository: StudentRegistrationRepository
) {
    suspend operator fun invoke(departmentId: String): StudentRegistrationContext = repository.selectDepartment(departmentId)
}

class SelectStudentCourseUseCase(
    private val repository: StudentRegistrationRepository
) {
    suspend operator fun invoke(courseId: String): StudentRegistrationContext = repository.selectCourse(courseId)
}

class SelectStudentGroupUseCase(
    private val repository: StudentRegistrationRepository
) {
    suspend operator fun invoke(groupId: String): StudentRegistrationContext = repository.selectGroup(groupId)
}

class SaveStudentProfileUseCase(
    private val repository: StudentRegistrationRepository
) {
    suspend operator fun invoke(fullName: String, subgroup: String): StudentProfile =
        repository.saveStudentProfile(fullName, subgroup)
}
