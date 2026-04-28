package com.example.scheduleiseu.data.repository.core

import com.example.scheduleiseu.data.mapper.impl.TeacherRegistrationContextToTeacherProfileMapperImpl
import com.example.scheduleiseu.data.mapper.impl.TeacherTimeTableDataToTeacherRegistrationContextMapperImpl
import com.example.scheduleiseu.data.remote.datasource.ScheduleRemoteDataSource
import com.example.scheduleiseu.data.remote.datasource.impl.ScheduleRemoteDataSourceImpl
import com.example.scheduleiseu.data.remote.parser.TimeTableParser
import com.example.scheduleiseu.data.session.TeacherRegistrationSessionStore
import com.example.scheduleiseu.domain.core.repository.TeacherRegistrationRepository

object TeacherRegistrationRepositoryFactory {
    private val parser: TimeTableParser by lazy { TimeTableParser() }
    private val remoteDataSource: ScheduleRemoteDataSource by lazy { ScheduleRemoteDataSourceImpl(parser) }
    private val sessionStore: TeacherRegistrationSessionStore by lazy { TeacherRegistrationSessionStore() }

    private val repository: TeacherRegistrationRepository by lazy {
        TeacherRegistrationRepositoryImpl(
            scheduleRemoteDataSource = remoteDataSource,
            sessionStore = sessionStore,
            contextMapper = TeacherTimeTableDataToTeacherRegistrationContextMapperImpl(),
            profileMapper = TeacherRegistrationContextToTeacherProfileMapperImpl(),
            preferencesDataSource = BsuCabinetDataComponent.preferences
        )
    }

    fun create(): TeacherRegistrationRepository = repository
}
