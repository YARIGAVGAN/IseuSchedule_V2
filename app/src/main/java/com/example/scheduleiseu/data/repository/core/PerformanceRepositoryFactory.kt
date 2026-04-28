package com.example.scheduleiseu.data.repository.core

import com.example.scheduleiseu.data.mapper.impl.ProgressTableResultToSemesterPerformanceMapperImpl
import com.example.scheduleiseu.data.mapper.impl.SemesterLinkToSemesterReferenceMapperImpl
import com.example.scheduleiseu.data.remote.datasource.PerformanceRemoteDataSource
import com.example.scheduleiseu.data.remote.datasource.impl.PerformanceRemoteDataSourceImpl
import com.example.scheduleiseu.domain.core.repository.ActiveSessionRepository
import com.example.scheduleiseu.domain.core.repository.PerformanceRepository

object PerformanceRepositoryFactory {

    fun create(): PerformanceRepository {
        val remoteDataSource: PerformanceRemoteDataSource = PerformanceRemoteDataSourceImpl(BsuCabinetDataComponent.parser)
        return PerformanceRepositoryImpl(
            remoteDataSource = remoteDataSource,
            authSessionStore = BsuCabinetDataComponent.authSessionStore,
            semesterMapper = SemesterLinkToSemesterReferenceMapperImpl(),
            performanceMapper = ProgressTableResultToSemesterPerformanceMapperImpl(),
            performanceCacheDao = BsuCabinetDataComponent.database.performanceCacheDao()
        )
    }

    fun createActiveSessionRepository(): ActiveSessionRepository {
        return ActiveSessionRepositoryImpl(BsuCabinetDataComponent.authSessionStore)
    }
}
