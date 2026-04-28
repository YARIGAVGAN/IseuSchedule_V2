package com.example.scheduleiseu.domain.core.model

data class AuthSession(
    val userRole: UserRole,
    val isAuthenticated: Boolean,
    val captchaRequired: Boolean = true,
    val loginPagePrepared: Boolean = false,
    val displayName: String? = null,
    val sessionKey: String? = null
)
