package com.example.scheduleiseu.data.repository.core

import com.example.scheduleiseu.data.mapper.impl.StudentRegistrationContextToStudentProfileMapperImpl
import com.example.scheduleiseu.data.mapper.impl.TimeTableDataToStudentRegistrationContextMapperImpl
import com.example.scheduleiseu.data.remote.datasource.ScheduleRemoteDataSource
import com.example.scheduleiseu.data.remote.datasource.impl.ScheduleRemoteDataSourceImpl
import com.example.scheduleiseu.data.remote.parser.TimeTableParser
import com.example.scheduleiseu.data.session.StudentRegistrationSessionStore
import com.example.scheduleiseu.domain.core.repository.StudentRegistrationRepository

object StudentRegistrationRepositoryFactory {
    private val parser: TimeTableParser by lazy { TimeTableParser() }
    private val remoteDataSource: ScheduleRemoteDataSource by lazy { ScheduleRemoteDataSourceImpl(parser) }
    private val sessionStore: StudentRegistrationSessionStore by lazy { StudentRegistrationSessionStore() }

    private val repository: StudentRegistrationRepository by lazy {
        StudentRegistrationRepositoryImpl(
            scheduleRemoteDataSource = remoteDataSource,
            sessionStore = sessionStore,
            contextMapper = TimeTableDataToStudentRegistrationContextMapperImpl(),
            profileMapper = StudentRegistrationContextToStudentProfileMapperImpl(),
            preferencesDataSource = BsuCabinetDataComponent.preferences
        )
    }

    fun create(): StudentRegistrationRepository = repository
}
