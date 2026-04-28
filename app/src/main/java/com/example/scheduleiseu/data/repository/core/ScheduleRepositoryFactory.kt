package com.example.scheduleiseu.data.repository.core

import com.example.scheduleiseu.data.mapper.impl.CurrentWeekInfoToWeekInfoMapperImpl
import com.example.scheduleiseu.data.mapper.impl.StudentTimeTableDataToScheduleContextMapperImpl
import com.example.scheduleiseu.data.mapper.impl.StudentTimeTableDataToScheduleWeekMapperImpl
import com.example.scheduleiseu.data.mapper.impl.TeacherTimeTableDataToScheduleContextMapperImpl
import com.example.scheduleiseu.data.mapper.impl.TeacherTimeTableDataToScheduleWeekMapperImpl
import com.example.scheduleiseu.data.remote.datasource.ScheduleRemoteDataSource
import com.example.scheduleiseu.data.remote.datasource.impl.ScheduleRemoteDataSourceImpl
import com.example.scheduleiseu.data.remote.parser.TimeTableParser
import com.example.scheduleiseu.data.session.ScheduleSessionStore
import com.example.scheduleiseu.domain.core.repository.ScheduleRepository

object ScheduleRepositoryFactory {
    private val parser: TimeTableParser by lazy { TimeTableParser() }
    private val remoteDataSource: ScheduleRemoteDataSource by lazy { ScheduleRemoteDataSourceImpl(parser) }
    private val sessionStore: ScheduleSessionStore by lazy { ScheduleSessionStore() }

    private val repository: ScheduleRepository by lazy {
        ScheduleRepositoryImpl(
            scheduleRemoteDataSource = remoteDataSource,
            sessionStore = sessionStore,
            studentContextMapper = StudentTimeTableDataToScheduleContextMapperImpl(),
            teacherContextMapper = TeacherTimeTableDataToScheduleContextMapperImpl(),
            studentWeekMapper = StudentTimeTableDataToScheduleWeekMapperImpl(),
            teacherWeekMapper = TeacherTimeTableDataToScheduleWeekMapperImpl(),
            weekInfoMapper = CurrentWeekInfoToWeekInfoMapperImpl(),
            scheduleCacheDao = BsuCabinetDataComponent.database.scheduleCacheDao(),
            preferencesDataSource = BsuCabinetDataComponent.preferences
        )
    }

    fun create(): ScheduleRepository = repository
}
