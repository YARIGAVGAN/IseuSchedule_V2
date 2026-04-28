package com.example.scheduleiseu.data.session

import com.example.scheduleiseu.data.remote.model.LoginPageData
import com.example.scheduleiseu.domain.core.model.AuthSession
import java.util.concurrent.atomic.AtomicReference

class AuthSessionStore {
    private val sessionRef = AtomicReference<AuthSession?>(null)
    private val loginPageRef = AtomicReference<LoginPageData?>(null)

    fun getSession(): AuthSession? = sessionRef.get()

    fun saveSession(session: AuthSession) {
        sessionRef.set(session)
    }

    fun clearSession() {
        sessionRef.set(null)
    }

    fun getLoginPageData(): LoginPageData? = loginPageRef.get()

    fun saveLoginPageData(loginPageData: LoginPageData) {
        loginPageRef.set(loginPageData)
    }

    fun clearLoginPageData() {
        loginPageRef.set(null)
    }

    fun clearAll() {
        clearSession()
        clearLoginPageData()
    }
}
