package com.example.scheduleiseu.data.mapper.contract

import com.example.scheduleiseu.data.remote.model.TimeTableData
import com.example.scheduleiseu.domain.core.model.StudentProfile
import com.example.scheduleiseu.domain.core.model.StudentRegistrationContext
import com.example.scheduleiseu.feature.auth.studentregistration.StudentRegistrationUiState

fun interface TimeTableDataToStudentRegistrationContextMapper : DataToDomainMapper<TimeTableData, StudentRegistrationContext>

fun interface StudentRegistrationContextToUiStateMapper {
    fun map(
        context: StudentRegistrationContext,
        previousState: StudentRegistrationUiState
    ): StudentRegistrationUiState
}

fun interface StudentRegistrationContextToStudentProfileMapper {
    fun map(
        fullName: String,
        subgroup: String,
        context: StudentRegistrationContext
    ): StudentProfile
}
