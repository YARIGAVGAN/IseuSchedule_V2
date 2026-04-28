package com.example.scheduleiseu.domain.core.model

data class StudentProfile(
    val fullName: String,
    val login: String? = null,
    val facultyId: String? = null,
    val faculty: String? = null,
    val departmentId: String? = null,
    val department: String? = null,
    val courseId: String? = null,
    val course: String? = null,
    val groupId: String? = null,
    val group: String? = null,
    val subgroup: String? = null,
    val averageScore: String? = null,
    val photo: UserPhoto? = null
)

data class TeacherProfile(
    val fullName: String,
    val teacherId: String? = null,
    val login: String? = null,
    val faculty: String? = null,
    val department: String? = null,
    val photo: UserPhoto? = null
)
