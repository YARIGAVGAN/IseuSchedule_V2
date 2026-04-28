package com.example.scheduleiseu.feature.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.scheduleiseu.data.local.preferences.AppPreferencesDataSource
import com.example.scheduleiseu.data.ocr.MlKitCaptchaRecognizer
import com.example.scheduleiseu.data.repository.core.BsuCabinetDataComponent
import com.example.scheduleiseu.data.repository.core.StudentLoginRepositoryFactory
import com.example.scheduleiseu.domain.core.repository.AuthRepository
import com.example.scheduleiseu.domain.core.service.CaptchaRecognizer

class LoginViewModelFactory(
    private val authRepository: AuthRepository = StudentLoginRepositoryFactory.create(),
    private val preferencesDataSource: AppPreferencesDataSource = BsuCabinetDataComponent.preferences,
    private val captchaRecognizer: CaptchaRecognizer = MlKitCaptchaRecognizer()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        return LoginViewModel(
            authRepository = authRepository,
            preferencesDataSource = preferencesDataSource,
            captchaRecognizer = captchaRecognizer
        ) as T
    }
}
