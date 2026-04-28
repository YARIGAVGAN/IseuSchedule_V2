package com.example.scheduleiseu.data.repository.core

import com.example.scheduleiseu.domain.core.usecase.BackgroundRefreshUseCase

object BackgroundRefreshUseCaseFactory {
    private val useCase: BackgroundRefreshUseCase by lazy {
        BackgroundRefreshUseCase(
            activeSessionRepository = ProfileRepositoryFactory.createActiveSessionRepository(),
            profileRepository = ProfileRepositoryFactory.create(),
            studentRegistrationRepository = StudentRegistrationRepositoryFactory.create(),
            teacherRegistrationRepository = TeacherRegistrationRepositoryFactory.create(),
            scheduleRepository = ScheduleRepositoryFactory.create(),
            performanceRepository = PerformanceRepositoryFactory.create(),
            onOwnScheduleRefreshed = BsuCabinetDataComponent.lessonNotificationScheduler::scheduleNext
        )
    }

    fun create(): BackgroundRefreshUseCase = useCase
}
