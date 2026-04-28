package com.example.scheduleiseu.data.mapper.contract

import com.example.scheduleiseu.data.remote.model.TeacherTimeTableData
import com.example.scheduleiseu.domain.core.model.TeacherProfile
import com.example.scheduleiseu.domain.core.model.TeacherRegistrationContext

fun interface TeacherTimeTableDataToTeacherRegistrationContextMapper : DataToDomainMapper<TeacherTimeTableData, TeacherRegistrationContext>

fun interface TeacherRegistrationContextToTeacherProfileMapper {
    fun map(
        teacherId: String,
        context: TeacherRegistrationContext
    ): TeacherProfile
}
