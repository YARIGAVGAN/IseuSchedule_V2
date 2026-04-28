package com.example.scheduleiseu.feature.auth.login

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scheduleiseu.data.local.preferences.AppPreferencesDataSource
import com.example.scheduleiseu.domain.core.model.AuthSession
import com.example.scheduleiseu.domain.core.model.UserRole
import com.example.scheduleiseu.domain.core.repository.AuthRepository
import com.example.scheduleiseu.domain.core.service.CaptchaRecognizer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val preferencesDataSource: AppPreferencesDataSource,
    private val captchaRecognizer: CaptchaRecognizer
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LoginUiEvent>()
    val events: SharedFlow<LoginUiEvent> = _events.asSharedFlow()

    private var currentSession: AuthSession? = null
    private var automaticLoginAllowed = false
    private var automaticLoginAttempted = false

    init {
        loadSavedCredentialsAndRefreshCaptcha()
    }

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.LoginChanged -> _state.update {
                it.copy(login = action.value, errorMessage = null)
            }

            is LoginAction.PasswordChanged -> _state.update {
                it.copy(password = action.value, errorMessage = null)
            }

            is LoginAction.CaptchaChanged -> _state.update {
                it.copy(captcha = action.value, errorMessage = null)
            }

            is LoginAction.AutoLoginChanged -> _state.update {
                it.copy(autoLogin = action.value)
            }

            LoginAction.RefreshCaptchaClicked -> refreshCaptcha()
            LoginAction.ContinueWithoutRegistrationClicked -> continueWithoutRegistration()
            LoginAction.LoginClicked -> submitLogin()
        }
    }

    private fun loadSavedCredentialsAndRefreshCaptcha() {
        viewModelScope.launch {
            val savedLogin = preferencesDataSource.getSavedStudentLogin().orEmpty()
            val savedPassword = preferencesDataSource.getSavedStudentPassword().orEmpty()
            val hasSavedCredentials = savedLogin.isNotBlank() && savedPassword.isNotBlank()

            automaticLoginAllowed = hasSavedCredentials
            automaticLoginAttempted = false

            _state.update {
                it.copy(
                    login = savedLogin,
                    password = savedPassword,
                    autoLogin = hasSavedCredentials
                )
            }
            refreshCaptcha(allowAutomaticLogin = hasSavedCredentials)
        }
    }

    private fun refreshCaptcha(
        preserveCredentials: Boolean = true,
        errorMessage: String? = null,
        allowAutomaticLogin: Boolean = false
    ) {
        viewModelScope.launch {
            val previous = _state.value
            _state.update {
                it.copy(
                    controlsEnabled = false,
                    isLoading = true,
                    errorMessage = errorMessage,
                    captcha = if (preserveCredentials) it.captcha else ""
                )
            }

            runCatching {
                val session = authRepository.prepareSession(UserRole.STUDENT)
                val captchaBytes = authRepository.loadCaptcha(session)
                val captchaBitmap = BitmapFactory.decodeByteArray(captchaBytes, 0, captchaBytes.size)
                    ?: throw IllegalStateException("Не удалось декодировать captcha")
                val recognizedCaptcha = runCatching {
                    captchaRecognizer.recognize(captchaBytes).orEmpty()
                }.getOrDefault("").trim()

                CaptchaLoadResult(
                    session = session,
                    bitmap = captchaBitmap,
                    recognizedCaptcha = recognizedCaptcha
                )
            }.onSuccess { result ->
                currentSession = result.session
                _state.update {
                    it.copy(
                        login = if (preserveCredentials) previous.login else "",
                        password = if (preserveCredentials) previous.password else "",
                        captcha = result.recognizedCaptcha.takeIf { captcha -> captcha.isNotBlank() }.orEmpty(),
                        captchaBitmap = result.bitmap,
                        controlsEnabled = true,
                        isLoading = false,
                        errorMessage = errorMessage
                    )
                }

                if (allowAutomaticLogin) {
                    submitAutomaticLoginIfReady()
                }
            }.onFailure { throwable ->
                currentSession = null
                _state.update {
                    it.copy(
                        controlsEnabled = true,
                        isLoading = false,
                        errorMessage = throwable.message ?: "Не удалось загрузить captcha"
                    )
                }
            }
        }
    }

    private fun continueWithoutRegistration() {
        viewModelScope.launch {
            preferencesDataSource.clearStudentCredentials()
            preferencesDataSource.clearAuthFlags()
            preferencesDataSource.setStudentScheduleOnlyModeEnabled(true)
            _events.emit(LoginUiEvent.ContinueWithoutRegistration)
        }
    }

    private fun submitLogin(isAutomatic: Boolean = false) {
        val session = currentSession ?: run {
            if (isAutomatic) {
                _state.update { it.copy(errorMessage = "Автовход не выполнен. Нажмите, чтобы войти вручную") }
            } else {
                refreshCaptcha(errorMessage = "Сессия логина устарела. Обновите captcha")
            }
            return
        }

        val stateValue = _state.value
        if (stateValue.login.isBlank() || stateValue.password.isBlank() || stateValue.captcha.isBlank()) {
            _state.update { it.copy(errorMessage = "Введите логин, пароль и captcha") }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    controlsEnabled = false,
                    isLoading = true,
                    errorMessage = null
                )
            }

            runCatching {
                authRepository.signIn(
                    session = session,
                    login = stateValue.login,
                    password = stateValue.password,
                    captcha = stateValue.captcha
                )
            }.onSuccess { authenticatedSession ->
                currentSession = authenticatedSession
                preferencesDataSource.setStudentScheduleOnlyModeEnabled(false)
                if (stateValue.autoLogin) {
                    preferencesDataSource.saveStudentCredentials(
                        login = stateValue.login,
                        password = stateValue.password
                    )
                } else {
                    preferencesDataSource.clearStudentCredentials()
                }
                _state.update {
                    it.copy(
                        controlsEnabled = true,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                _events.emit(
                    LoginUiEvent.LoginSucceeded(
                        session = authenticatedSession,
                        accountLogin = stateValue.login
                    )
                )
            }.onFailure { throwable ->
                val message = throwable.message ?: "Не удалось выполнить вход"
                if (isAutomatic) {
                    _state.update {
                        it.copy(
                            controlsEnabled = true,
                            isLoading = false,
                            errorMessage = message
                        )
                    }
                } else {
                    refreshCaptcha(
                        preserveCredentials = true,
                        errorMessage = message
                    )
                }
            }
        }
    }

    private fun submitAutomaticLoginIfReady() {
        if (!automaticLoginAllowed || automaticLoginAttempted) return

        val stateValue = _state.value
        val canSubmit = stateValue.autoLogin &&
            stateValue.login.isNotBlank() &&
            stateValue.password.isNotBlank() &&
            stateValue.captcha.isNotBlank() &&
            currentSession != null

        if (!canSubmit) return

        automaticLoginAttempted = true
        submitLogin(isAutomatic = true)
    }

    private data class CaptchaLoadResult(
        val session: AuthSession,
        val bitmap: Bitmap,
        val recognizedCaptcha: String
    )
}
