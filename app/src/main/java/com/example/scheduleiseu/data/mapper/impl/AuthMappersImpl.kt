package com.example.scheduleiseu.data.mapper.impl

import com.example.scheduleiseu.data.mapper.contract.LoginPageDataToAuthSessionMapper
import com.example.scheduleiseu.data.remote.model.LoginPageData
import com.example.scheduleiseu.domain.core.model.AuthSession
import com.example.scheduleiseu.domain.core.model.UserRole
import java.util.UUID

class LoginPageDataToAuthSessionMapperImpl(
    private val userRole: UserRole
) : LoginPageDataToAuthSessionMapper {
    override fun map(input: LoginPageData): AuthSession {
        return AuthSession(
            userRole = userRole,
            isAuthenticated = false,
            captchaRequired = true,
            loginPagePrepared = true,
            displayName = null,
            sessionKey = UUID.randomUUID().toString()
        )
    }
}
