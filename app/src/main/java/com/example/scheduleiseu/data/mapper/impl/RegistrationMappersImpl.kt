package com.example.scheduleiseu.data.mapper.impl

import com.example.scheduleiseu.data.mapper.contract.StudentRegistrationContextToStudentProfileMapper
import com.example.scheduleiseu.data.mapper.contract.StudentRegistrationContextToUiStateMapper
import com.example.scheduleiseu.data.mapper.contract.TimeTableDataToStudentRegistrationContextMapper
import com.example.scheduleiseu.data.remote.model.SelectOption
import com.example.scheduleiseu.data.remote.model.TimeTableData
import com.example.scheduleiseu.domain.core.model.StudentProfile
import com.example.scheduleiseu.domain.core.model.StudentRegistrationContext
import com.example.scheduleiseu.domain.core.model.StudentRegistrationOption
import com.example.scheduleiseu.feature.auth.studentregistration.StudentRegistrationUiState

class TimeTableDataToStudentRegistrationContextMapperImpl : TimeTableDataToStudentRegistrationContextMapper {
    override fun map(input: TimeTableData): StudentRegistrationContext {
        return StudentRegistrationContext(
            faculties = input.faculties.map { it.toDomainOption() },
            departments = input.departments.map { it.toDomainOption() },
            courses = input.courses.map { it.toDomainOption() },
            groups = input.groups.map { it.toDomainOption() },
            selectedFacultyId = input.faculties.firstOrNull { it.isSelected }?.value,
            selectedDepartmentId = input.departments.firstOrNull { it.isSelected }?.value,
            selectedCourseId = input.courses.firstOrNull { it.isSelected }?.value,
            selectedGroupId = input.groups.firstOrNull { it.isSelected }?.value
        )
    }
}

class StudentRegistrationContextToUiStateMapperImpl : StudentRegistrationContextToUiStateMapper {
    override fun map(
        context: StudentRegistrationContext,
        previousState: StudentRegistrationUiState
    ): StudentRegistrationUiState {
        return previousState.copy(
            selectedFaculty = context.faculties.firstOrNull { it.id == context.selectedFacultyId }?.title,
            selectedStudyForm = context.departments.firstOrNull { it.id == context.selectedDepartmentId }?.title,
            selectedCourse = context.courses.firstOrNull { it.id == context.selectedCourseId }?.title,
            selectedGroup = context.groups.firstOrNull { it.id == context.selectedGroupId }?.title,
            facultyOptions = context.faculties.map { it.title },
            studyFormOptions = context.departments.map { it.title },
            courseOptions = context.courses.map { it.title },
            groupOptions = context.groups.map { it.title }
        )
    }
}

class StudentRegistrationContextToStudentProfileMapperImpl : StudentRegistrationContextToStudentProfileMapper {
    override fun map(fullName: String, subgroup: String, context: StudentRegistrationContext): StudentProfile {
        val faculty = context.faculties.firstOrNull { it.id == context.selectedFacultyId }
        val department = context.departments.firstOrNull { it.id == context.selectedDepartmentId }
        val course = context.courses.firstOrNull { it.id == context.selectedCourseId }
        val group = context.groups.firstOrNull { it.id == context.selectedGroupId }

        return StudentProfile(
            fullName = fullName,
            facultyId = faculty?.id,
            faculty = faculty?.title,
            departmentId = department?.id,
            department = department?.title,
            courseId = course?.id,
            course = course?.title,
            groupId = group?.id,
            group = group?.title,
            subgroup = subgroup
        )
    }
}

private fun SelectOption.toDomainOption(): StudentRegistrationOption {
    return StudentRegistrationOption(
        id = value,
        title = text,
        selected = isSelected
    )
}
