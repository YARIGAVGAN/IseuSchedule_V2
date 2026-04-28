package com.example.scheduleiseu.domain.core.repository

import com.example.scheduleiseu.domain.core.model.AuthSession

interface ActiveSessionRepository {
    fun getActiveSession(): AuthSession?
}
