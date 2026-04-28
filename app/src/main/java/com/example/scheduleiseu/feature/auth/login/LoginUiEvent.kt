package com.example.scheduleiseu.feature.auth.login

import com.example.scheduleiseu.domain.core.model.AuthSession

sealed interface LoginUiEvent {
    data class LoginSucceeded(
        val session: AuthSession,
        val accountLogin: String
    ) : LoginUiEvent

    data object ContinueWithoutRegistration : LoginUiEvent
}
