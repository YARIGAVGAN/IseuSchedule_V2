package com.example.scheduleiseu.data.remote.datasource

import com.example.scheduleiseu.data.remote.model.CurrentWeekInfo
import com.example.scheduleiseu.data.remote.model.TeacherTimeTableData
import com.example.scheduleiseu.data.remote.model.TimeTableData

interface ScheduleRemoteDataSource {
    suspend fun getInitialStudentPage(): TimeTableData?
    suspend fun getInitialTeacherPage(): TeacherTimeTableData?
    suspend fun getCurrentStudentWeek(): CurrentWeekInfo?
    suspend fun getCurrentTeacherWeek(): CurrentWeekInfo?

    suspend fun updateOnFacultySelect(
        facultyId: String,
        currentData: TimeTableData
    ): TimeTableData?

    suspend fun updateOnDepartmentSelect(
        facultyId: String,
        departmentId: String,
        currentData: TimeTableData
    ): TimeTableData?

    suspend fun updateOnCourseSelect(
        facultyId: String,
        departmentId: String,
        courseId: String,
        currentData: TimeTableData
    ): TimeTableData?

    suspend fun updateOnGroupSelect(
        facultyId: String,
        departmentId: String,
        courseId: String,
        groupId: String,
        currentData: TimeTableData
    ): TimeTableData?

    suspend fun getStudentTimeTable(
        facultyId: String,
        departmentId: String,
        courseId: String,
        groupId: String,
        weekDate: String,
        currentData: TimeTableData
    ): TimeTableData?

    suspend fun getTeacherTimeTable(
        teacherId: String,
        weekDate: String,
        currentData: TeacherTimeTableData
    ): TeacherTimeTableData?
}
