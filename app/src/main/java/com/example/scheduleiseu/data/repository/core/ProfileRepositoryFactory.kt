package com.example.scheduleiseu.data.repository.core

import com.example.scheduleiseu.data.mapper.impl.ProfileDataToStudentProfileMapperImpl
import com.example.scheduleiseu.data.mapper.impl.ProfileDataToTeacherProfileMapperImpl
import com.example.scheduleiseu.data.mapper.impl.ProfileDataToUserPhotoMapperImpl
import com.example.scheduleiseu.data.remote.datasource.ProfileRemoteDataSource
import com.example.scheduleiseu.data.remote.datasource.impl.ProfileRemoteDataSourceImpl
import com.example.scheduleiseu.domain.core.repository.ActiveSessionRepository
import com.example.scheduleiseu.domain.core.repository.ProfileRepository

object ProfileRepositoryFactory {

    fun create(): ProfileRepository {
        val remoteDataSource: ProfileRemoteDataSource = ProfileRemoteDataSourceImpl(BsuCabinetDataComponent.parser)
        val photoMapper = ProfileDataToUserPhotoMapperImpl()

        return ProfileRepositoryImpl(
            remoteDataSource = remoteDataSource,
            studentProfileMapper = ProfileDataToStudentProfileMapperImpl(photoMapper),
            teacherProfileMapper = ProfileDataToTeacherProfileMapperImpl(photoMapper),
            photoMapper = photoMapper,
            preferencesDataSource = BsuCabinetDataComponent.preferences,
            photoCacheDataSource = BsuCabinetDataComponent.profilePhotoCache
        )
    }

    fun createActiveSessionRepository(): ActiveSessionRepository {
        return ActiveSessionRepositoryImpl(BsuCabinetDataComponent.authSessionStore)
    }
}
