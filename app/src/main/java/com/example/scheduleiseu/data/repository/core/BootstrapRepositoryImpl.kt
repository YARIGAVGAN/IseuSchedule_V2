package com.example.scheduleiseu.data.repository.core

import com.example.scheduleiseu.data.local.preferences.AppPreferencesDataSource
import com.example.scheduleiseu.domain.core.repository.BootstrapRepository

class BootstrapRepositoryImpl(
    private val preferencesDataSource: AppPreferencesDataSource
) : BootstrapRepository {

    override suspend fun isStudentBootstrapCompleted(accountKey: String): Boolean {
        return preferencesDataSource.isStudentBootstrapCompleted(accountKey)
    }

    override suspend fun markStudentBootstrapCompleted(accountKey: String) {
        preferencesDataSource.markStudentBootstrapCompleted(accountKey)
    }

    override suspend fun resetStudentBootstrap() {
        preferencesDataSource.resetStudentBootstrap()
    }
}