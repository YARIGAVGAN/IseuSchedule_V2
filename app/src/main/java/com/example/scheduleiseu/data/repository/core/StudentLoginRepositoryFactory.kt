package com.example.scheduleiseu.data.repository.core

import com.example.scheduleiseu.data.mapper.impl.LoginPageDataToAuthSessionMapperImpl
import com.example.scheduleiseu.data.remote.datasource.AuthRemoteDataSource
import com.example.scheduleiseu.data.remote.datasource.ProfileRemoteDataSource
import com.example.scheduleiseu.data.remote.datasource.impl.AuthRemoteDataSourceImpl
import com.example.scheduleiseu.data.remote.datasource.impl.ProfileRemoteDataSourceImpl
import com.example.scheduleiseu.domain.core.model.UserRole
import com.example.scheduleiseu.domain.core.repository.AuthRepository

object StudentLoginRepositoryFactory {

    fun create(): AuthRepository {
        val parser = BsuCabinetDataComponent.parser
        val authRemoteDataSource: AuthRemoteDataSource = AuthRemoteDataSourceImpl(parser)
        val profileRemoteDataSource: ProfileRemoteDataSource = ProfileRemoteDataSourceImpl(parser)
        val sessionStore = BsuCabinetDataComponent.authSessionStore

        return AuthRepositoryImpl(
            authRemoteDataSource = authRemoteDataSource,
            profileRemoteDataSource = profileRemoteDataSource,
            sessionStore = sessionStore,
            loginPageSessionMapper = LoginPageDataToAuthSessionMapperImpl(UserRole.STUDENT),
            preferencesDataSource = BsuCabinetDataComponent.preferences
        )
    }
}
