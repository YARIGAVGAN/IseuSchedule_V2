package com.example.scheduleiseu.domain.core.repository

import com.example.scheduleiseu.domain.core.model.AuthSession
import com.example.scheduleiseu.domain.core.model.UserRole

interface AuthRepository {
    suspend fun clearActiveSession()
    suspend fun prepareSession(userRole: UserRole): AuthSession
    suspend fun loadCaptcha(session: AuthSession): ByteArray
    suspend fun signIn(
        session: AuthSession,
        login: String,
        password: String,
        captcha: String
    ): AuthSession

    suspend fun logout()
}
