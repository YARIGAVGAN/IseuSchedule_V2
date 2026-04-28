package com.example.scheduleiseu.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.scheduleiseu.data.repository.core.BsuCabinetDataComponent
import com.example.scheduleiseu.data.repository.core.PerformanceRepositoryFactory
import com.example.scheduleiseu.data.repository.core.ProfileRepositoryFactory
import com.example.scheduleiseu.domain.core.repository.ActiveSessionRepository
import com.example.scheduleiseu.domain.core.repository.ProfileRepository
import com.example.scheduleiseu.domain.core.usecase.GetActiveUserRoleUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveCachedUserPhotoUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveLatestPerformanceUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveStudentProfileUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveTeacherProfileUseCase
import com.example.scheduleiseu.domain.core.usecase.RefreshStudentProfileUseCase
import com.example.scheduleiseu.domain.core.usecase.RefreshTeacherProfileUseCase
import com.example.scheduleiseu.domain.core.usecase.RefreshUserPhotoUseCase

class MenuProfileViewModelFactory(
    private val profileRepository: ProfileRepository = ProfileRepositoryFactory.create(),
    private val activeSessionRepository: ActiveSessionRepository = ProfileRepositoryFactory.createActiveSessionRepository()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MenuProfileViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }

        return MenuProfileViewModel(
            getActiveUserRole = GetActiveUserRoleUseCase(
                activeSessionRepository = activeSessionRepository,
                savedUserRoleProvider = BsuCabinetDataComponent.preferences::getUserRoleBlocking
            ),
            observeStudentProfile = ObserveStudentProfileUseCase(profileRepository),
            observeTeacherProfile = ObserveTeacherProfileUseCase(profileRepository),
            observeCachedUserPhoto = ObserveCachedUserPhotoUseCase(profileRepository),
            observeLatestPerformance = ObserveLatestPerformanceUseCase(PerformanceRepositoryFactory.create()),
            refreshStudentProfile = RefreshStudentProfileUseCase(profileRepository, activeSessionRepository),
            refreshTeacherProfile = RefreshTeacherProfileUseCase(profileRepository, activeSessionRepository),
            refreshUserPhoto = RefreshUserPhotoUseCase(profileRepository, activeSessionRepository),
            isStudentScheduleOnlyModeProvider = BsuCabinetDataComponent.preferences::isStudentScheduleOnlyModeEnabledBlocking
        ) as T
    }
}
