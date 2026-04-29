package com.example.scheduleiseu.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.scheduleiseu.data.repository.core.BsuCabinetDataComponent
import com.example.scheduleiseu.data.repository.core.ScheduleRepositoryFactory
import com.example.scheduleiseu.data.repository.core.StudentRegistrationRepositoryFactory
import com.example.scheduleiseu.data.repository.core.TeacherRegistrationRepositoryFactory
import com.example.scheduleiseu.domain.core.network.NetworkMonitor
import com.example.scheduleiseu.domain.core.repository.ScheduleRepository
import com.example.scheduleiseu.domain.core.repository.StudentRegistrationRepository
import com.example.scheduleiseu.domain.core.repository.TeacherRegistrationRepository
import com.example.scheduleiseu.domain.core.usecase.LoadExternalStudentGroupScheduleUseCase
import com.example.scheduleiseu.domain.core.usecase.LoadExternalTeacherScheduleUseCase
import com.example.scheduleiseu.domain.core.usecase.LoadStudentScheduleContextUseCase
import com.example.scheduleiseu.domain.core.usecase.LoadTeacherScheduleContextUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveOwnStudentCachedWeeksUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveOwnStudentCachedWeekValuesUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveOwnStudentScheduleForWeekUseCase
import com.example.scheduleiseu.domain.core.usecase.ObserveOwnStudentScheduleUseCase
import com.example.scheduleiseu.domain.core.usecase.RefreshOwnStudentScheduleForWeekUseCase
import com.example.scheduleiseu.domain.core.usecase.RefreshOwnStudentScheduleUseCase
import com.example.scheduleiseu.domain.core.usecase.ScheduleLessonVisibilityFilter

class ScheduleViewModelFactory(
    private val scheduleRepository: ScheduleRepository = ScheduleRepositoryFactory.create(),
    private val studentRegistrationRepository: StudentRegistrationRepository = StudentRegistrationRepositoryFactory.create(),
    private val teacherRegistrationRepository: TeacherRegistrationRepository = TeacherRegistrationRepositoryFactory.create(),
    private val networkMonitor: NetworkMonitor = BsuCabinetDataComponent.networkMonitor,
    private val visibilityFilter: ScheduleLessonVisibilityFilter = BsuCabinetDataComponent.scheduleLessonVisibilityFilter
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        return ScheduleViewModel(
            ownGroupTitle = studentRegistrationRepository.getSavedStudentProfile()?.group,
            registeredSubgroup = studentRegistrationRepository.getSavedStudentProfile()?.subgroup,
            preferencesDataSource = BsuCabinetDataComponent.preferences,
            observeOwnStudentCachedWeeks = ObserveOwnStudentCachedWeeksUseCase(
                scheduleRepository = scheduleRepository,
                studentRegistrationRepository = studentRegistrationRepository
            ),
            observeOwnStudentCachedWeekValues = ObserveOwnStudentCachedWeekValuesUseCase(
                scheduleRepository = scheduleRepository,
                studentRegistrationRepository = studentRegistrationRepository
            ),
            observeOwnStudentSchedule = ObserveOwnStudentScheduleUseCase(
                scheduleRepository = scheduleRepository,
                studentRegistrationRepository = studentRegistrationRepository
            ),
            observeOwnStudentScheduleForWeek = ObserveOwnStudentScheduleForWeekUseCase(
                scheduleRepository = scheduleRepository,
                studentRegistrationRepository = studentRegistrationRepository
            ),
            refreshOwnStudentSchedule = RefreshOwnStudentScheduleUseCase(
                scheduleRepository = scheduleRepository,
                studentRegistrationRepository = studentRegistrationRepository,
                onOwnScheduleRefreshed = BsuCabinetDataComponent.lessonNotificationScheduler::scheduleNext
            ),
            refreshOwnStudentScheduleForWeek = RefreshOwnStudentScheduleForWeekUseCase(
                scheduleRepository = scheduleRepository,
                studentRegistrationRepository = studentRegistrationRepository,
                onOwnScheduleRefreshed = BsuCabinetDataComponent.lessonNotificationScheduler::scheduleNext
            ),
            loadStudentScheduleContext = LoadStudentScheduleContextUseCase(
                scheduleRepository = scheduleRepository,
                studentRegistrationRepository = studentRegistrationRepository
            ),
            loadTeacherScheduleContext = LoadTeacherScheduleContextUseCase(
                scheduleRepository = scheduleRepository
            ),
            loadExternalStudentGroupSchedule = LoadExternalStudentGroupScheduleUseCase(
                scheduleRepository = scheduleRepository,
                studentRegistrationRepository = studentRegistrationRepository
            ),
            loadExternalTeacherSchedule = LoadExternalTeacherScheduleUseCase(
                scheduleRepository = scheduleRepository,
                teacherRegistrationRepository = teacherRegistrationRepository
            ),
            networkMonitor = networkMonitor,
            visibilityFilter = visibilityFilter
        ) as T
    }
}
