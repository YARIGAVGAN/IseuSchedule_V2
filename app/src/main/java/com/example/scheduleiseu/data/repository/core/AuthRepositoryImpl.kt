package com.example.scheduleiseu.data.repository.core

import com.example.scheduleiseu.data.local.preferences.AppPreferencesDataSource
import com.example.scheduleiseu.data.mapper.contract.LoginPageDataToAuthSessionMapper
import com.example.scheduleiseu.data.remote.datasource.AuthRemoteDataSource
import com.example.scheduleiseu.data.remote.datasource.ProfileRemoteDataSource
import com.example.scheduleiseu.data.remote.model.ProfileData
import com.example.scheduleiseu.data.remote.parser.BsuParser
import com.example.scheduleiseu.data.session.AuthSessionStore
import com.example.scheduleiseu.domain.core.model.AuthSession
import com.example.scheduleiseu.domain.core.model.UserRole
import com.example.scheduleiseu.domain.core.repository.AuthRepository

class AuthRepositoryImpl(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val profileRemoteDataSource: ProfileRemoteDataSource,
    private val sessionStore: AuthSessionStore,
    private val loginPageSessionMapper: LoginPageDataToAuthSessionMapper,
    private val preferencesDataSource: AppPreferencesDataSource? = null
) : AuthRepository {

    override suspend fun clearActiveSession() {
        sessionStore.clearAll()
        BsuCabinetDataComponent.cookieJar.clear()
        preferencesDataSource?.clearAuthFlags()
    }

    override suspend fun prepareSession(userRole: UserRole): AuthSession {
        require(userRole == UserRole.STUDENT) {
            "На текущем этапе поддержан только student login flow"
        }

        val loginPageData = authRemoteDataSource.loadLoginPage()
        sessionStore.saveLoginPageData(loginPageData)

        val session = loginPageSessionMapper.map(loginPageData)
        sessionStore.saveSession(session)
        preferencesDataSource?.setUserRole(userRole)
        preferencesDataSource?.setAuthFlags(
            isAuthenticated = session.isAuthenticated,
            loginPagePrepared = session.loginPagePrepared,
            captchaRequired = session.captchaRequired
        )
        return session
    }

    override suspend fun loadCaptcha(session: AuthSession): ByteArray {
        val preparedLoginPage = sessionStore.getLoginPageData()
            ?: throw IllegalStateException("Сессия логина не подготовлена")

        ensureSessionMatches(session)
        return authRemoteDataSource.loadCaptcha(preparedLoginPage)
    }

    override suspend fun signIn(
        session: AuthSession,
        login: String,
        password: String,
        captcha: String
    ): AuthSession {
        ensureSessionMatches(session)

        val preparedLoginPage = sessionStore.getLoginPageData()
            ?: throw IllegalStateException("Сессия логина не подготовлена")

        val loginResult = authRemoteDataSource.login(
            loginPageData = preparedLoginPage,
            username = login,
            password = password,
            captcha = captcha
        )

        if (!loginResult.success) {
            val message = BsuParser().extractLoginError(loginResult.html)
                ?: "Не удалось выполнить вход. Проверьте логин, пароль и captcha."
            throw IllegalStateException(message)
        }

        val profile = profileRemoteDataSource.getProfile()
        val displayName = validateAuthenticatedCabinet(profile)

        val authenticatedSession = session.copy(
            isAuthenticated = true,
            captchaRequired = false,
            loginPagePrepared = true,
            displayName = displayName
        )

        sessionStore.saveSession(authenticatedSession)
        preferencesDataSource?.setUserRole(authenticatedSession.userRole)
        preferencesDataSource?.setAuthFlags(
            isAuthenticated = authenticatedSession.isAuthenticated,
            loginPagePrepared = authenticatedSession.loginPagePrepared,
            captchaRequired = authenticatedSession.captchaRequired
        )
        return authenticatedSession
    }

    override suspend fun logout() {
        val logoutConfirmed = authRemoteDataSource.logout()
        if (!logoutConfirmed) {
            throw IllegalStateException("Не удалось подтвердить выход из личного кабинета")
        }
        sessionStore.clearAll()
        preferencesDataSource?.clearSessionFlagsForLogout()
    }

    private fun ensureSessionMatches(session: AuthSession) {
        val stored = sessionStore.getSession()
            ?: throw IllegalStateException("Сессия логина не подготовлена")

        if (stored.sessionKey != session.sessionKey) {
            throw IllegalStateException("Сессия логина устарела. Обновите captcha и попробуйте снова")
        }
    }
    private fun validateAuthenticatedCabinet(profile: ProfileData): String {
        return profile.fullName!!.trim()
    }
}
