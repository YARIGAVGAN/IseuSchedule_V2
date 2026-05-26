package com.example.scheduleiseu.data.repository.core

import com.example.scheduleiseu.domain.core.model.WeekInfo

internal object ScheduleCacheKeys {
    const val roleStudent = "student"
    const val roleTeacher = "teacher"

    fun buildStudentOwnerId(
        facultyId: String,
        departmentId: String,
        courseId: String,
        groupId: String
    ): String = listOf(facultyId, departmentId, courseId, groupId).joinToString(separator = ":")

    fun buildCacheKey(role: String, ownerId: String, week: WeekInfo): String {
        return listOf(role, ownerId, week.value).joinToString(separator = "|")
    }
}
