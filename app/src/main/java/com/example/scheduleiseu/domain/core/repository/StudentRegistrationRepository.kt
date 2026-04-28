package com.example.scheduleiseu.domain.core.repository

import com.example.scheduleiseu.domain.core.model.StudentProfile
import com.example.scheduleiseu.domain.core.model.StudentRegistrationContext

interface StudentRegistrationRepository {
    suspend fun loadInitialContext(): StudentRegistrationContext
    suspend fun selectFaculty(facultyId: String): StudentRegistrationContext
    suspend fun selectDepartment(departmentId: String): StudentRegistrationContext
    suspend fun selectCourse(courseId: String): StudentRegistrationContext
    suspend fun selectGroup(groupId: String): StudentRegistrationContext
    suspend fun saveStudentProfile(fullName: String, subgroup: String): StudentProfile
    fun getSavedStudentProfile(): StudentProfile?
}
