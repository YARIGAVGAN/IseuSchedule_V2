package com.example.scheduleiseu.data.repository.core

import com.example.scheduleiseu.data.local.preferences.AppPreferencesDataSource
import com.example.scheduleiseu.data.mapper.contract.TeacherRegistrationContextToTeacherProfileMapper
import com.example.scheduleiseu.data.mapper.contract.TeacherTimeTableDataToTeacherRegistrationContextMapper
import com.example.scheduleiseu.data.remote.datasource.ScheduleRemoteDataSource
import com.example.scheduleiseu.data.remote.model.TeacherTimeTableData
import com.example.scheduleiseu.data.session.TeacherRegistrationSessionStore
import com.example.scheduleiseu.domain.core.model.TeacherProfile
import com.example.scheduleiseu.domain.core.model.TeacherRegistrationContext
import com.example.scheduleiseu.domain.core.repository.TeacherRegistrationRepository

class TeacherRegistrationRepositoryImpl(
    private val scheduleRemoteDataSource: ScheduleRemoteDataSource,
    private val sessionStore: TeacherRegistrationSessionStore,
    private val contextMapper: TeacherTimeTableDataToTeacherRegistrationContextMapper,
    private val profileMapper: TeacherRegistrationContextToTeacherProfileMapper,
    private val preferencesDataSource: AppPreferencesDataSource? = null
) : TeacherRegistrationRepository {

    override suspend fun loadInitialContext(): TeacherRegistrationContext {
        val data = scheduleRemoteDataSource.getInitialTeacherPage()
            ?: throw IllegalStateException("Не удалось загрузить список преподавателей")
        sessionStore.saveRawState(data)
        return contextMapper.map(data)
    }

    override suspend fun saveTeacherProfile(teacherId: String): TeacherProfile {
        val normalizedTeacherId = teacherId.trim()
        require(normalizedTeacherId.isNotBlank()) { "Выберите преподавателя" }

        val context = contextMapper.map(requireRawState())
        if (context.teachers.none { it.id == normalizedTeacherId }) {
            throw IllegalStateException("Выбранный преподаватель не найден")
        }

        val profile = profileMapper.map(normalizedTeacherId, context)
        sessionStore.saveProfile(profile)
        preferencesDataSource?.saveTeacherProfile(profile)
        return profile
    }

    override fun getSavedTeacherProfile(): TeacherProfile? =
        sessionStore.getSavedProfile() ?: preferencesDataSource?.getTeacherProfileBlocking()

    override suspend fun clearSavedTeacherProfile() {
        sessionStore.clearAll()
        preferencesDataSource?.clearTeacherProfileForLogout()
    }

    private fun requireRawState(): TeacherTimeTableData {
        return sessionStore.getRawState()
            ?: throw IllegalStateException("Состояние регистрации преподавателя не инициализировано")
    }
}
