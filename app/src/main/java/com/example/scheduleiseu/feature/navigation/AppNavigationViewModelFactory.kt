package com.example.scheduleiseu.feature.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.scheduleiseu.data.ocr.MlKitCaptchaRecognizer
import com.example.scheduleiseu.data.repository.core.BackgroundRefreshUseCaseFactory
import com.example.scheduleiseu.data.repository.core.BsuCabinetDataComponent
import com.example.scheduleiseu.data.repository.core.FirstEntryBootstrapUseCaseFactory
import com.example.scheduleiseu.data.repository.core.PerformanceRepositoryFactory
import com.example.scheduleiseu.data.repository.core.ProfileRepositoryFactory
import com.example.scheduleiseu.data.repository.core.ScheduleRepositoryFactory
import com.example.scheduleiseu.data.repository.core.StudentRegistrationRepositoryFactory
import com.example.scheduleiseu.data.repository.core.StudentLoginRepositoryFactory
import com.example.scheduleiseu.data.repository.core.TeacherRegistrationRepositoryFactory
import com.example.scheduleiseu.domain.core.network.NetworkMonitor
import com.example.scheduleiseu.domain.core.repository.AuthRepository
import com.example.scheduleiseu.domain.core.repository.PerformanceRepository
import com.example.scheduleiseu.domain.core.repository.ProfileRepository
import com.example.scheduleiseu.domain.core.repository.ScheduleRepository
import com.example.scheduleiseu.domain.core.repository.StudentRegistrationRepository
import com.example.scheduleiseu.domain.core.repository.TeacherRegistrationRepository
import com.example.scheduleiseu.domain.core.service.CaptchaRecognizer
import com.example.scheduleiseu.domain.core.usecase.BackgroundRefreshUseCase
import com.example.scheduleiseu.domain.core.usecase.FirstEntryBootstrapUseCase

class AppNavigationViewModelFactory(
    private val authRepository: AuthRepository = StudentLoginRepositoryFactory.create(),
    private val studentRegistrationRepository: StudentRegistrationRepository = StudentRegistrationRepositoryFactory.create(),
    private val teacherRegistrationRepository: TeacherRegistrationRepository = TeacherRegistrationRepositoryFactory.create(),
    private val scheduleRepository: ScheduleRepository = ScheduleRepositoryFactory.create(),
    private val performanceRepository: PerformanceRepository = PerformanceRepositoryFactory.create(),
    private val profileRepository: ProfileRepository = ProfileRepositoryFactory.create(),
    private val firstEntryBootstrapUseCase: FirstEntryBootstrapUseCase = FirstEntryBootstrapUseCaseFactory.create(),
    private val backgroundRefreshUseCase: BackgroundRefreshUseCase = BackgroundRefreshUseCaseFactory.create(),
    private val networkMonitor: NetworkMonitor = BsuCabinetDataComponent.networkMonitor,
    private val captchaRecognizer: CaptchaRecognizer = MlKitCaptchaRecognizer(),
    private val lessonNotificationScheduler: com.example.scheduleiseu.notification.LessonNotificationScheduler = BsuCabinetDataComponent.lessonNotificationScheduler
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AppNavigationViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        return AppNavigationViewModel(
            preferencesDataSource = BsuCabinetDataComponent.preferences,
            authRepository = authRepository,
            studentRegistrationRepository = studentRegistrationRepository,
            teacherRegistrationRepository = teacherRegistrationRepository,
            scheduleRepository = scheduleRepository,
            performanceRepository = performanceRepository,
            profileRepository = profileRepository,
            firstEntryBootstrapUseCase = firstEntryBootstrapUseCase,
            backgroundRefreshUseCase = backgroundRefreshUseCase,
            networkMonitor = networkMonitor,
            captchaRecognizer = captchaRecognizer,
            lessonNotificationScheduler = lessonNotificationScheduler
        ) as T
    }
}
