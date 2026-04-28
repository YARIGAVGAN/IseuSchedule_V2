package com.example.scheduleiseu.domain.core.repository

import com.example.scheduleiseu.domain.core.model.TeacherProfile
import com.example.scheduleiseu.domain.core.model.TeacherRegistrationContext

interface TeacherRegistrationRepository {
    suspend fun loadInitialContext(): TeacherRegistrationContext
    suspend fun saveTeacherProfile(teacherId: String): TeacherProfile
    fun getSavedTeacherProfile(): TeacherProfile?
    suspend fun clearSavedTeacherProfile()
}
