package com.example.scheduleiseu.data.repository.core

import com.example.scheduleiseu.data.session.AuthSessionStore
import com.example.scheduleiseu.domain.core.model.AuthSession
import com.example.scheduleiseu.domain.core.repository.ActiveSessionRepository

class ActiveSessionRepositoryImpl(
    private val authSessionStore: AuthSessionStore
) : ActiveSessionRepository {
    override fun getActiveSession(): AuthSession? = authSessionStore.getSession()
}
