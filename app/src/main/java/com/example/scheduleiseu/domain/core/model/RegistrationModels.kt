package com.example.scheduleiseu.domain.core.model

data class StudentRegistrationOption(
    val id: String,
    val title: String,
    val selected: Boolean = false
)

data class StudentRegistrationContext(
    val faculties: List<StudentRegistrationOption> = emptyList(),
    val departments: List<StudentRegistrationOption> = emptyList(),
    val courses: List<StudentRegistrationOption> = emptyList(),
    val groups: List<StudentRegistrationOption> = emptyList(),
    val selectedFacultyId: String? = null,
    val selectedDepartmentId: String? = null,
    val selectedCourseId: String? = null,
    val selectedGroupId: String? = null
)


data class TeacherRegistrationOption(
    val id: String,
    val title: String,
    val selected: Boolean = false
)

data class TeacherRegistrationContext(
    val teachers: List<TeacherRegistrationOption> = emptyList(),
    val selectedTeacherId: String? = null
)
