package com.example.scheduleiseu.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.scheduleiseu.data.repository.core.BsuCabinetDataComponent
import com.example.scheduleiseu.data.repository.core.ScheduleRepositoryFactory
import com.example.scheduleiseu.data.repository.core.TeacherRegistrationRepositoryFactory
import com.example.scheduleiseu.domain.core.network.NetworkMonitor
import com.example.scheduleiseu.domain.core.repository.ScheduleRepository
import com.example.scheduleiseu.domain.core.repository.TeacherRegistrationRepository
import com.example.scheduleiseu.domain.core.usecase.LoadTeacherScheduleContextUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveOwnTeacherCachedWeeksUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveOwnTeacherCachedWeekValuesUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveOwnTeacherScheduleForWeekUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveOwnTeacherScheduleUseCase
import com.example.scheduleiseu.domain.core.usecase.RefreshOwnTeacherScheduleForWeekUseCase
import com.example.scheduleiseu.domain.core.usecase.RefreshOwnTeacherScheduleUseCase

class TeacherScheduleViewModelFactory(
    private val scheduleRepository: ScheduleRepository = ScheduleRepositoryFactory.create(),
    private val teacherRegistrationRepository: TeacherRegistrationRepository = TeacherRegistrationRepositoryFactory.create(),
    private val networkMonitor: NetworkMonitor = BsuCabinetDataComponent.networkMonitor
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TeacherScheduleViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        return TeacherScheduleViewModel(
            observeOwnTeacherCachedWeeks = ObserveOwnTeacherCachedWeeksUseCase(
                scheduleRepository = scheduleRepository,
                teacherRegistrationRepository = teacherRegistrationRepository
            ),
            observeOwnTeacherCachedWeekValues = ObserveOwnTeacherCachedWeekValuesUseCase(
                scheduleRepository = scheduleRepository,
                teacherRegistrationRepository = teacherRegistrationRepository
            ),
            observeOwnTeacherSchedule = ObserveOwnTeacherScheduleUseCase(
                scheduleRepository = scheduleRepository,
                teacherRegistrationRepository = teacherRegistrationRepository
            ),
            observeOwnTeacherScheduleForWeek = ObserveOwnTeacherScheduleForWeekUseCase(
                scheduleRepository = scheduleRepository,
                teacherRegistrationRepository = teacherRegistrationRepository
            ),
            refreshOwnTeacherSchedule = RefreshOwnTeacherScheduleUseCase(
                scheduleRepository = scheduleRepository,
                teacherRegistrationRepository = teacherRegistrationRepository,
                onOwnScheduleRefreshed = BsuCabinetDataComponent.lessonNotificationScheduler::scheduleNext
            ),
            refreshOwnTeacherScheduleForWeek = RefreshOwnTeacherScheduleForWeekUseCase(
                scheduleRepository = scheduleRepository,
                teacherRegistrationRepository = teacherRegistrationRepository,
                onOwnScheduleRefreshed = BsuCabinetDataComponent.lessonNotificationScheduler::scheduleNext
            ),
            loadTeacherScheduleContext = LoadTeacherScheduleContextUseCase(scheduleRepository),
            networkMonitor = networkMonitor
        ) as T
    }
}
