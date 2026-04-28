package com.example.scheduleiseu.data.remote.datasource.impl

import com.example.scheduleiseu.data.remote.datasource.ScheduleRemoteDataSource
import com.example.scheduleiseu.data.remote.model.CurrentWeekInfo
import com.example.scheduleiseu.data.remote.model.TeacherTimeTableData
import com.example.scheduleiseu.data.remote.model.TimeTableData
import com.example.scheduleiseu.data.remote.parser.TimeTableParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduleRemoteDataSourceImpl(
    private val parser: TimeTableParser
) : ScheduleRemoteDataSource {

    override suspend fun getInitialStudentPage(): TimeTableData? = withContext(Dispatchers.IO) {
        parser.getInitialStudentPage()
    }

    override suspend fun getInitialTeacherPage(): TeacherTimeTableData? = withContext(Dispatchers.IO) {
        parser.getInitialTeacherPage()
    }

    override suspend fun getCurrentStudentWeek(): CurrentWeekInfo? = withContext(Dispatchers.IO) {
        parser.getCurrentStudentWeek()
    }

    override suspend fun getCurrentTeacherWeek(): CurrentWeekInfo? = withContext(Dispatchers.IO) {
        parser.getCurrentTeacherWeek()
    }

    override suspend fun updateOnFacultySelect(
        facultyId: String,
        currentData: TimeTableData
    ): TimeTableData? = withContext(Dispatchers.IO) {
        parser.updateOnFacultySelect(facultyId, currentData)
    }

    override suspend fun updateOnDepartmentSelect(
        facultyId: String,
        departmentId: String,
        currentData: TimeTableData
    ): TimeTableData? = withContext(Dispatchers.IO) {
        parser.updateOnDepartmentSelect(facultyId, departmentId, currentData)
    }

    override suspend fun updateOnCourseSelect(
        facultyId: String,
        departmentId: String,
        courseId: String,
        currentData: TimeTableData
    ): TimeTableData? = withContext(Dispatchers.IO) {
        parser.updateOnCourseSelect(facultyId, departmentId, courseId, currentData)
    }

    override suspend fun updateOnGroupSelect(
        facultyId: String,
        departmentId: String,
        courseId: String,
        groupId: String,
        currentData: TimeTableData
    ): TimeTableData? = withContext(Dispatchers.IO) {
        parser.updateOnGroupSelect(facultyId, departmentId, courseId, groupId, currentData)
    }

    override suspend fun getStudentTimeTable(
        facultyId: String,
        departmentId: String,
        courseId: String,
        groupId: String,
        weekDate: String,
        currentData: TimeTableData
    ): TimeTableData? = withContext(Dispatchers.IO) {
        parser.getStudentTimeTable(
            facultyId = facultyId,
            departmentId = departmentId,
            courseId = courseId,
            groupId = groupId,
            weekDate = weekDate,
            currentData = currentData
        )
    }

    override suspend fun getTeacherTimeTable(
        teacherId: String,
        weekDate: String,
        currentData: TeacherTimeTableData
    ): TeacherTimeTableData? = withContext(Dispatchers.IO) {
        parser.getTeacherTimeTable(
            teacherId = teacherId,
            weekDate = weekDate,
            currentData = currentData
        )
    }
}
