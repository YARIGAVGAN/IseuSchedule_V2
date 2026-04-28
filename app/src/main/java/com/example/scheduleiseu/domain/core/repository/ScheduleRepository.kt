package com.example.scheduleiseu.domain.core.repository

import com.example.scheduleiseu.domain.core.model.ScheduleContext
import com.example.scheduleiseu.domain.core.model.ScheduleWeek
import com.example.scheduleiseu.domain.core.model.WeekInfo
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun observeCachedStudentSchedule(
        facultyId: String,
        departmentId: String,
        courseId: String,
        groupId: String,
        week: WeekInfo? = null
    ): Flow<ScheduleWeek?>

    fun observeCachedTeacherSchedule(
        teacherId: String,
        week: WeekInfo? = null
    ): Flow<ScheduleWeek?>

    fun clearTeacherSessionState()

    fun observeCachedStudentWeekValues(
        facultyId: String,
        departmentId: String,
        courseId: String,
        groupId: String
    ): Flow<Set<String>>

    fun observeCachedTeacherWeekValues(teacherId: String): Flow<Set<String>>

    fun observeCachedStudentWeeks(
        facultyId: String,
        departmentId: String,
        courseId: String,
        groupId: String
    ): Flow<List<WeekInfo>>

    fun observeCachedTeacherWeeks(teacherId: String): Flow<List<WeekInfo>>

    suspend fun refreshStudentScheduleContext(): ScheduleContext
    suspend fun refreshTeacherScheduleContext(): ScheduleContext
    suspend fun refreshStudentCurrentWeek(): WeekInfo?
    suspend fun refreshTeacherCurrentWeek(): WeekInfo?

    suspend fun updateStudentContextByFaculty(context: ScheduleContext, facultyId: String): ScheduleContext
    suspend fun updateStudentContextByDepartment(
        context: ScheduleContext,
        facultyId: String,
        departmentId: String
    ): ScheduleContext
    suspend fun updateStudentContextByCourse(
        context: ScheduleContext,
        facultyId: String,
        departmentId: String,
        courseId: String
    ): ScheduleContext
    suspend fun refreshStudentSchedule(
        context: ScheduleContext,
        facultyId: String,
        departmentId: String,
        courseId: String,
        groupId: String,
        week: WeekInfo
    ): ScheduleWeek

    suspend fun refreshTeacherSchedule(
        context: ScheduleContext,
        teacherId: String,
        week: WeekInfo
    ): ScheduleWeek

    suspend fun clearAllCachedScheduleWeeks()
}
